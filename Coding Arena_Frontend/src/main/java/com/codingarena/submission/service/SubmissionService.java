package com.codingarena.submission.service;

import com.codingarena.auth.model.User;
import com.codingarena.match.dto.MatchDto;
import com.codingarena.match.dto.MatchEvent;
import com.codingarena.match.dto.ProblemExampleDto;
import com.codingarena.match.exception.MatchAccessDeniedException;
import com.codingarena.match.exception.MatchNotFoundException;
import com.codingarena.match.exception.ProblemNotFoundException;
import com.codingarena.match.model.Match;
import com.codingarena.match.model.Problem;
import com.codingarena.match.model.TestCase;
import com.codingarena.match.repository.MatchRepository;
import com.codingarena.match.repository.ProblemRepository;
import com.codingarena.match.repository.TestCaseRepository;
import com.codingarena.submission.client.Judge0Client;
import com.codingarena.submission.client.Judge0Response;
import com.codingarena.submission.dto.SubmissionDto;
import com.codingarena.submission.dto.SubmissionRequest;
import com.codingarena.submission.exception.MatchCompletedException;
import com.codingarena.submission.model.Submission;
import com.codingarena.submission.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    private final SubmissionRepository submissionRepository;
    private final MatchRepository matchRepository;
    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final Judge0Client judge0Client;
    private final EloService eloService;
    private final SimpMessageSendingOperations messagingTemplate;

    @Autowired
    public SubmissionService(SubmissionRepository submissionRepository,
                             MatchRepository matchRepository,
                             ProblemRepository problemRepository,
                             TestCaseRepository testCaseRepository,
                             Judge0Client judge0Client,
                             EloService eloService,
                             SimpMessageSendingOperations messagingTemplate) {
        this.submissionRepository = submissionRepository;
        this.matchRepository = matchRepository;
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.judge0Client = judge0Client;
        this.eloService = eloService;
        this.messagingTemplate = messagingTemplate;
    }

    public SubmissionService(SubmissionRepository submissionRepository,
                             MatchRepository matchRepository,
                             TestCaseRepository testCaseRepository,
                             Judge0Client judge0Client,
                             EloService eloService,
                             SimpMessageSendingOperations messagingTemplate) {
        this(submissionRepository, matchRepository, null, testCaseRepository, judge0Client, eloService, messagingTemplate);
    }

    @Transactional
    public SubmissionDto createSubmission(UUID matchId, SubmissionRequest request, User currentUser) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match not found"));

        boolean isPlayerA = match.getPlayerA() != null && match.getPlayerA().getId().equals(currentUser.getId());
        boolean isPlayerB = match.getPlayerB() != null && match.getPlayerB().getId().equals(currentUser.getId());

        if (!isPlayerA && !isPlayerB) {
            throw new MatchAccessDeniedException("Access denied to this match");
        }

        if ("COMPLETED".equals(match.getStatus())) {
            throw new MatchCompletedException("Match is already completed");
        }

        if ("EXPIRED".equals(match.getStatus())) {
            throw new MatchCompletedException("Match is already expired");
        }

        List<TestCase> testCases = testCaseRepository.findByProblemId(match.getProblem().getId());
        String finalVerdict = evaluateSubmission(request.getCode(), request.getLanguage(), testCases);

        Submission submission = new Submission(
                match,
                currentUser,
                request.getCode(),
                request.getLanguage(),
                finalVerdict
        );
        submissionRepository.save(submission);

        log.info("Submission {} for match {} received verdict: {}", submission.getId(), matchId, finalVerdict);

        // If ACCEPTED, attempt atomic winner update
        if ("ACCEPTED".equals(finalVerdict)) {
            int updatedRows = matchRepository.setWinnerAtomically(match.getId(), currentUser.getId());

            // Gated: ONLY proceed with Elo update & MATCH_END if this submission really was first to win
            if (updatedRows == 1) {
                User winner = currentUser;
                User loser = currentUser.getId().equals(match.getPlayerA().getId())
                        ? match.getPlayerB()
                        : match.getPlayerA();

                eloService.updateRatings(match, winner, loser);

                // Update in-memory match instance so MatchDto.fromEntity(match) sent in MATCH_END broadcast reflects completed state
                match.setStatus("COMPLETED");
                match.setWinnerId(winner.getId());
                match.setEndedAt(LocalDateTime.now());

                MatchDto matchDto = MatchDto.fromEntity(match);
                if (testCaseRepository != null && matchDto.getProblem() != null) {
                    List<ProblemExampleDto> examples = testCaseRepository.findByProblemIdAndIsSampleTrue(match.getProblem().getId())
                            .stream()
                            .limit(3)
                            .map(tc -> new ProblemExampleDto(tc.getInput(), tc.getExpectedOutput()))
                            .collect(Collectors.toList());
                    matchDto.getProblem().setExamples(examples);
                }

                MatchEvent endEvent = new MatchEvent("MATCH_END", matchDto);
                messagingTemplate.convertAndSend("/topic/match/" + match.getId(), endEvent);

                log.info("Match {} completed. Winner: {}", match.getId(), winner.getUsername());
            } else {
                log.info("Submission {} was ACCEPTED, but match {} already has a winner declared", submission.getId(), match.getId());
            }
        }

        return SubmissionDto.fromEntity(submission);
    }

    @Transactional
    public SubmissionDto createPracticeSubmission(UUID problemId, SubmissionRequest request, User currentUser) {
        if (problemRepository == null) {
            throw new IllegalStateException("ProblemRepository not configured");
        }
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException("Problem not found: " + problemId));

        List<TestCase> testCases = testCaseRepository.findByProblemId(problemId);
        String finalVerdict = evaluateSubmission(request.getCode(), request.getLanguage(), testCases);

        Submission submission = new Submission(
                problem,
                currentUser,
                request.getCode(),
                request.getLanguage(),
                finalVerdict
        );
        Submission saved = submissionRepository.save(submission);

        log.info("Practice submission {} for problem {} received verdict: {}", saved.getId(), problemId, finalVerdict);
        return SubmissionDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SubmissionDto> getPracticeSubmissions(UUID problemId, User currentUser) {
        if (problemRepository != null && !problemRepository.existsById(problemId)) {
            throw new ProblemNotFoundException("Problem not found: " + problemId);
        }
        return submissionRepository.findByProblemIdAndUserIdAndMatchIsNullOrderBySubmittedAtDesc(problemId, currentUser.getId())
                .stream()
                .map(SubmissionDto::fromEntity)
                .collect(Collectors.toList());
    }

    private String evaluateSubmission(String code, String language, List<TestCase> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            Judge0Response response = judge0Client.execute(code, language, "", "");
            return mapJudge0ResponseToVerdict(response, "");
        }

        for (TestCase testCase : testCases) {
            Judge0Response response = judge0Client.execute(
                    code,
                    language,
                    testCase.getInput(),
                    testCase.getExpectedOutput()
            );

            String verdict = mapJudge0ResponseToVerdict(response, testCase.getExpectedOutput());
            if (!"ACCEPTED".equals(verdict)) {
                return verdict; // Return first failing verdict (WRONG_ANSWER, TLE, RE)
            }
        }

        return "ACCEPTED";
    }

    private String mapJudge0ResponseToVerdict(Judge0Response response, String expectedOutput) {
        if (response == null || response.getStatus() == null) {
            return "RE";
        }

        int statusId = response.getStatus().getId() != null ? response.getStatus().getId() : -1;

        if (statusId == 5) {
            return "TLE"; // Time Limit Exceeded
        }

        if (statusId == 4) {
            return "WRONG_ANSWER";
        }

        if (statusId == 6 || (statusId >= 7 && statusId <= 12)) {
            return "RE"; // Compilation Error or Runtime Error
        }

        if (statusId == 3) { // Accepted in Judge0
            String normalizedActual = normalizeOutput(response.getStdout());
            String normalizedExpected = normalizeOutput(expectedOutput);

            if (normalizedActual.equals(normalizedExpected)) {
                return "ACCEPTED";
            } else {
                return "WRONG_ANSWER";
            }
        }

        return "RE";
    }

    public String normalizeOutput(String output) {
        if (output == null) {
            return "";
        }
        // Normalize CRLF and CR to LF
        String normalized = output.replace("\r\n", "\n").replace("\r", "\n");

        // Trim trailing whitespace on each line
        String[] lines = normalized.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(lines[i].replaceAll("\\s+$", ""));
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }

        // Trim leading and trailing newlines/whitespace
        return sb.toString().strip();
    }

    @Transactional(readOnly = true)
    public List<SubmissionDto> getSubmissionsForMatch(UUID matchId, User currentUser) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match not found"));

        boolean isPlayerA = match.getPlayerA() != null && match.getPlayerA().getId().equals(currentUser.getId());
        boolean isPlayerB = match.getPlayerB() != null && match.getPlayerB().getId().equals(currentUser.getId());

        if (!isPlayerA && !isPlayerB) {
            throw new MatchAccessDeniedException("Access denied to this match's submissions");
        }

        List<Submission> submissions = submissionRepository.findByMatchIdOrderBySubmittedAtAsc(matchId);
        return submissions.stream()
                .map(SubmissionDto::fromEntity)
                .collect(Collectors.toList());
    }
}
