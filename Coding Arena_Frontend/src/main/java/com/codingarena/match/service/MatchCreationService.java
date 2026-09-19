package com.codingarena.match.service;

import com.codingarena.auth.model.User;
import com.codingarena.match.dto.MatchDto;
import com.codingarena.match.dto.MatchEvent;
import com.codingarena.match.dto.ProblemExampleDto;
import com.codingarena.match.model.Match;
import com.codingarena.match.model.Problem;
import com.codingarena.match.model.TestCase;
import com.codingarena.match.repository.MatchRepository;
import com.codingarena.match.repository.ProblemRepository;
import com.codingarena.match.repository.TestCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MatchCreationService {

    private static final Logger log = LoggerFactory.getLogger(MatchCreationService.class);

    private final ProblemRepository problemRepository;
    private final MatchRepository matchRepository;
    private final TestCaseRepository testCaseRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final long durationMinutes;

    @Autowired
    public MatchCreationService(ProblemRepository problemRepository,
                                MatchRepository matchRepository,
                                TestCaseRepository testCaseRepository,
                                SimpMessageSendingOperations messagingTemplate,
                                @Value("${match.duration-minutes:30}") long durationMinutes) {
        this.problemRepository = problemRepository;
        this.matchRepository = matchRepository;
        this.testCaseRepository = testCaseRepository;
        this.messagingTemplate = messagingTemplate;
        this.durationMinutes = durationMinutes;
    }

    public MatchCreationService(ProblemRepository problemRepository,
                                MatchRepository matchRepository,
                                TestCaseRepository testCaseRepository,
                                SimpMessageSendingOperations messagingTemplate) {
        this(problemRepository, matchRepository, testCaseRepository, messagingTemplate, 30L);
    }

    public MatchCreationService(ProblemRepository problemRepository,
                                MatchRepository matchRepository,
                                SimpMessageSendingOperations messagingTemplate,
                                long durationMinutes) {
        this(problemRepository, matchRepository, null, messagingTemplate, durationMinutes);
    }

    public MatchCreationService(ProblemRepository problemRepository,
                                MatchRepository matchRepository,
                                SimpMessageSendingOperations messagingTemplate) {
        this(problemRepository, matchRepository, null, messagingTemplate, 30L);
    }

    @Transactional
    public Match createMatchAndNotify(User playerA, User playerB) {
        Problem problem = problemRepository.findRandomProblem().orElseGet(this::seedDefaultProblem);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(durationMinutes);

        Match match = new Match(playerA, playerB, problem, "IN_PROGRESS", now, expiresAt);
        Match savedMatch = matchRepository.save(match);

        MatchDto matchDto = MatchDto.fromEntity(savedMatch);
        if (testCaseRepository != null && matchDto.getProblem() != null) {
            List<ProblemExampleDto> examples = testCaseRepository.findByProblemIdAndIsSampleTrue(problem.getId())
                    .stream()
                    .limit(3)
                    .map(tc -> new ProblemExampleDto(tc.getInput(), tc.getExpectedOutput()))
                    .collect(Collectors.toList());
            matchDto.getProblem().setExamples(examples);
        }

        MatchEvent foundEvent = new MatchEvent("MATCH_FOUND", matchDto);

        // 1. Notify both players individually on their per-user personal channels
        messagingTemplate.convertAndSend("/topic/user/" + playerA.getId(), foundEvent);
        messagingTemplate.convertAndSend("/topic/user/" + playerB.getId(), foundEvent);

        // 2. Broadcast MATCH_FOUND & MATCH_START events to match-specific topic /topic/match/{matchId}
        String matchDestination = "/topic/match/" + savedMatch.getId();
        messagingTemplate.convertAndSend(matchDestination, foundEvent);

        MatchEvent startEvent = new MatchEvent("MATCH_START", matchDto);
        messagingTemplate.convertAndSend(matchDestination, startEvent);

        log.info("Created match {} between {} and {}", savedMatch.getId(), playerA.getUsername(), playerB.getUsername());
        return savedMatch;
    }

    private Problem seedDefaultProblem() {
        Problem starter = new Problem(
                "Two Sum",
                "EASY",
                "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target."
        );
        return problemRepository.save(starter);
    }
}
