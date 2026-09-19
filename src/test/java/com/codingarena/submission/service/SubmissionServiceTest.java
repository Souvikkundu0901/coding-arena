package com.codingarena.submission.service;

import com.codingarena.auth.model.User;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.match.exception.MatchAccessDeniedException;
import com.codingarena.match.model.Match;
import com.codingarena.match.model.Problem;
import com.codingarena.match.model.TestCase;
import com.codingarena.match.repository.MatchRepository;
import com.codingarena.match.repository.TestCaseRepository;
import com.codingarena.submission.client.Judge0Client;
import com.codingarena.submission.client.Judge0Response;
import com.codingarena.submission.dto.SubmissionDto;
import com.codingarena.submission.dto.SubmissionRequest;
import com.codingarena.submission.exception.MatchCompletedException;
import com.codingarena.submission.model.Submission;
import com.codingarena.submission.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    private TestJudge0Client testJudge0Client;
    private EloService eloService;
    private SubmissionService submissionService;

    private User playerA;
    private User playerB;
    private Problem problem;
    private Match match;
    private UUID matchId;

    static class TestJudge0Client extends Judge0Client {
        Judge0Response stubResponse;

        public TestJudge0Client() {
            super(new RestTemplateBuilder(), "http://localhost:2358", false);
        }

        @Override
        public Judge0Response execute(String code, String language, String stdin, String expectedOutput) {
            return stubResponse;
        }
    }

    @BeforeEach
    void setUp() {
        testJudge0Client = new TestJudge0Client();
        eloService = new EloService(userRepository, matchRepository);

        submissionService = new SubmissionService(
                submissionRepository,
                matchRepository,
                testCaseRepository,
                testJudge0Client,
                eloService,
                messagingTemplate
        );

        playerA = new User("playerA", "a@example.com", "hash");
        playerA.setId(UUID.randomUUID());
        playerA.setRating(1200);

        playerB = new User("playerB", "b@example.com", "hash");
        playerB.setId(UUID.randomUUID());
        playerB.setRating(1200);

        problem = new Problem("Two Sum", "EASY", "Find target sum");
        problem.setId(UUID.randomUUID());

        matchId = UUID.randomUUID();
        match = new Match(playerA, playerB, problem, "IN_PROGRESS", LocalDateTime.now());
        match.setId(matchId);
    }

    @Test
    void createSubmission_OutsiderUser_ThrowsMatchAccessDeniedException() {
        User outsider = new User("outsider", "out@example.com", "hash");
        outsider.setId(UUID.randomUUID());

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        SubmissionRequest request = new SubmissionRequest("print(1)", "python");

        assertThrows(MatchAccessDeniedException.class,
                () -> submissionService.createSubmission(matchId, request, outsider));
    }

    @Test
    void createSubmission_CompletedMatch_ThrowsMatchCompletedException() {
        match.setStatus("COMPLETED");
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        SubmissionRequest request = new SubmissionRequest("print(1)", "python");

        assertThrows(MatchCompletedException.class,
                () -> submissionService.createSubmission(matchId, request, playerA));
    }

    @Test
    void createSubmission_ExpiredMatch_ThrowsMatchCompletedException() {
        match.setStatus("EXPIRED");
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        SubmissionRequest request = new SubmissionRequest("print(1)", "python");

        assertThrows(MatchCompletedException.class,
                () -> submissionService.createSubmission(matchId, request, playerA));
    }

    @Test
    void createSubmission_AcceptedAndFirstWinner_UpdatesRatingsAndBroadcastsMatchEnd() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        TestCase testCase = new TestCase(problem, "[2,7,11,15], 9", "[0,1]", true);
        when(testCaseRepository.findByProblemId(problem.getId())).thenReturn(List.of(testCase));

        Judge0Response acceptedResponse = new Judge0Response();
        acceptedResponse.setStatus(new Judge0Response.Judge0Status(3, "Accepted"));
        acceptedResponse.setStdout("[0,1]\n");
        testJudge0Client.stubResponse = acceptedResponse;

        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission s = invocation.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            return s;
        });

        // Simulates this submission being FIRST to set the winner (affected 1 row)
        when(matchRepository.setWinnerAtomically(matchId, playerA.getId())).thenReturn(1);

        SubmissionRequest request = new SubmissionRequest("solution", "java");
        SubmissionDto result = submissionService.createSubmission(matchId, request, playerA);

        assertEquals("ACCEPTED", result.getVerdict());
        verify(matchRepository).setWinnerAtomically(matchId, playerA.getId());
        verify(userRepository).save(playerA);
        verify(userRepository).save(playerB);
        assertEquals(1216, playerA.getRating());
        assertEquals(1184, playerB.getRating());
        verify(messagingTemplate).convertAndSend(eq("/topic/match/" + matchId), any(Object.class));
    }

    @Test
    void createSubmission_AcceptedButSecondWinner_DoesNotUpdateRatingsTwice() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        TestCase testCase = new TestCase(problem, "1 2", "3", true);
        when(testCaseRepository.findByProblemId(problem.getId())).thenReturn(List.of(testCase));

        Judge0Response acceptedResponse = new Judge0Response();
        acceptedResponse.setStatus(new Judge0Response.Judge0Status(3, "Accepted"));
        acceptedResponse.setStdout("3\n");
        testJudge0Client.stubResponse = acceptedResponse;

        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission s = invocation.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            return s;
        });

        // Simulates another submission already set the winner (affected 0 rows)
        when(matchRepository.setWinnerAtomically(matchId, playerB.getId())).thenReturn(0);

        SubmissionRequest request = new SubmissionRequest("solution", "python");
        SubmissionDto result = submissionService.createSubmission(matchId, request, playerB);

        assertEquals("ACCEPTED", result.getVerdict());
        verify(matchRepository).setWinnerAtomically(matchId, playerB.getId());
        // Verify Elo ratings are NOT updated again!
        verify(userRepository, never()).save(any(User.class));
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/match/" + matchId), any(Object.class));
    }

    @Test
    void createSubmission_WrongAnswer_DoesNotCallAtomicWinnerUpdate() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        TestCase testCase = new TestCase(problem, "1 2", "3", true);
        when(testCaseRepository.findByProblemId(problem.getId())).thenReturn(List.of(testCase));

        Judge0Response waResponse = new Judge0Response();
        waResponse.setStatus(new Judge0Response.Judge0Status(3, "Accepted"));
        waResponse.setStdout("4\n"); // Mismatched output
        testJudge0Client.stubResponse = waResponse;

        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission s = invocation.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            return s;
        });

        SubmissionRequest request = new SubmissionRequest("solution", "python");
        SubmissionDto result = submissionService.createSubmission(matchId, request, playerA);

        assertEquals("WRONG_ANSWER", result.getVerdict());
        verify(matchRepository, never()).setWinnerAtomically(any(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getSubmissionsForMatch_AuthorizedPlayer_ReturnsSubmissions() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        Submission sub1 = new Submission(match, playerA, "code1", "python", "ACCEPTED");
        sub1.setId(UUID.randomUUID());
        when(submissionRepository.findByMatchIdOrderBySubmittedAtAsc(matchId)).thenReturn(List.of(sub1));

        List<SubmissionDto> results = submissionService.getSubmissionsForMatch(matchId, playerA);

        assertEquals(1, results.size());
        assertEquals("ACCEPTED", results.get(0).getVerdict());
    }

    @Test
    void normalizeOutput_HandlesCRLFTrailingWhitespaceAndBlankLines() {
        String rawOutput = "\r\n1   \r\n2 \t  \r\n3\r\n\r\n";
        String expected = "1\n2\n3";

        assertEquals(expected, submissionService.normalizeOutput(rawOutput));
        assertEquals(expected, submissionService.normalizeOutput("1\n2\n3\n"));
    }

    @Test
    void createSubmission_OutputsWithTrailingSpacesAndCRLF_EvaluatesAsAccepted() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        TestCase testCase = new TestCase(problem, "1 2", "1\n2\n3", true);
        when(testCaseRepository.findByProblemId(problem.getId())).thenReturn(List.of(testCase));

        Judge0Response acceptedResponse = new Judge0Response();
        acceptedResponse.setStatus(new Judge0Response.Judge0Status(3, "Accepted"));
        // Simulates output from Judge0 with windows CRLF and trailing spaces per line
        acceptedResponse.setStdout("1  \r\n2 \r\n3\r\n\r\n");
        testJudge0Client.stubResponse = acceptedResponse;

        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission s = invocation.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            return s;
        });

        when(matchRepository.setWinnerAtomically(matchId, playerA.getId())).thenReturn(1);

        SubmissionRequest request = new SubmissionRequest("solution", "python");
        SubmissionDto result = submissionService.createSubmission(matchId, request, playerA);

        assertEquals("ACCEPTED", result.getVerdict());
    }
}
