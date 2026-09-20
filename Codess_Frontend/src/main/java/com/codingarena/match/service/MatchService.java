package com.codingarena.match.service;

import com.codingarena.auth.model.User;
import com.codingarena.match.dto.MatchDto;
import com.codingarena.match.dto.MatchEvent;
import com.codingarena.match.dto.ProblemExampleDto;
import com.codingarena.match.exception.MatchAccessDeniedException;
import com.codingarena.match.exception.MatchNotFoundException;
import com.codingarena.match.model.Match;
import com.codingarena.match.repository.MatchRepository;
import com.codingarena.match.repository.TestCaseRepository;
import com.codingarena.submission.exception.MatchCompletedException;
import com.codingarena.submission.service.EloService;
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
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final MatchRepository matchRepository;
    private final TestCaseRepository testCaseRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final EloService eloService;

    @Autowired
    public MatchService(MatchRepository matchRepository,
                        TestCaseRepository testCaseRepository,
                        SimpMessageSendingOperations messagingTemplate,
                        EloService eloService) {
        this.matchRepository = matchRepository;
        this.testCaseRepository = testCaseRepository;
        this.messagingTemplate = messagingTemplate;
        this.eloService = eloService;
    }

    public MatchService(MatchRepository matchRepository,
                        TestCaseRepository testCaseRepository,
                        SimpMessageSendingOperations messagingTemplate) {
        this(matchRepository, testCaseRepository, messagingTemplate, null);
    }

    public MatchService(MatchRepository matchRepository, SimpMessageSendingOperations messagingTemplate) {
        this(matchRepository, null, messagingTemplate, null);
    }

    public MatchService(MatchRepository matchRepository,
                        SimpMessageSendingOperations messagingTemplate,
                        EloService eloService) {
        this(matchRepository, null, messagingTemplate, eloService);
    }

    @Transactional(readOnly = true)
    public MatchDto getMatchDetails(UUID matchId, User currentUser) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match not found"));

        boolean isPlayerA = match.getPlayerA() != null && match.getPlayerA().getId().equals(currentUser.getId());
        boolean isPlayerB = match.getPlayerB() != null && match.getPlayerB().getId().equals(currentUser.getId());

        if (!isPlayerA && !isPlayerB) {
            throw new MatchAccessDeniedException("Access denied");
        }

        return buildMatchDto(match);
    }

    /**
     * Forfeits the match for the calling player.
     * The calling player loses and the opponent is declared the winner.
     */
    @Transactional
    public MatchDto forfeitMatch(UUID matchId, User currentUser) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match not found"));

        boolean isPlayerA = match.getPlayerA() != null && match.getPlayerA().getId().equals(currentUser.getId());
        boolean isPlayerB = match.getPlayerB() != null && match.getPlayerB().getId().equals(currentUser.getId());

        if (!isPlayerA && !isPlayerB) {
            throw new MatchAccessDeniedException("Access denied");
        }

        if ("COMPLETED".equals(match.getStatus())) {
            throw new MatchCompletedException("Match is already completed");
        }
        if ("EXPIRED".equals(match.getStatus())) {
            throw new MatchCompletedException("Match is already expired");
        }

        User winner = isPlayerA ? match.getPlayerB() : match.getPlayerA();
        User loser = currentUser;

        int updatedRows = matchRepository.forfeitMatchAtomically(matchId, winner.getId());
        if (updatedRows == 1) {
            if (eloService != null) {
                eloService.updateRatings(match, winner, loser);
            }

            match.setStatus("COMPLETED");
            match.setWinnerId(winner.getId());
            match.setEndedAt(LocalDateTime.now());

            broadcastMatchEnd(match, "FORFEIT");

            log.info("Match {} forfeited by {}. Winner: {}", matchId, loser.getUsername(), winner.getUsername());
            return buildMatchDto(match);
        } else {
            log.info("Match {} forfeit requested by {}, but match already ended concurrently", matchId, currentUser.getUsername());
            Match currentMatch = matchRepository.findById(matchId).orElse(match);
            return buildMatchDto(currentMatch);
        }
    }

    /**
     * Atomically sets the match winner at database level.
     * Prevents race conditions from simultaneous correct submissions.
     */
    @Transactional
    public boolean setWinnerAtomically(UUID matchId, UUID winnerId) {
        int updatedRows = matchRepository.setWinnerAtomically(matchId, winnerId);
        if (updatedRows > 0) {
            matchRepository.findById(matchId).ifPresent(this::broadcastMatchEnd);
            return true;
        }
        return false;
    }

    public void broadcastMatchEnd(Match match) {
        broadcastMatchEnd(match, null);
    }

    public void broadcastMatchEnd(Match match, String reason) {
        MatchDto matchDto = buildMatchDto(match);
        MatchEvent endEvent = new MatchEvent("MATCH_END", matchDto, reason);
        messagingTemplate.convertAndSend("/topic/match/" + match.getId(), endEvent);
    }

    public MatchDto buildMatchDto(Match match) {
        MatchDto matchDto = MatchDto.fromEntity(match);
        if (testCaseRepository != null && match.getProblem() != null && matchDto.getProblem() != null) {
            List<ProblemExampleDto> examples = testCaseRepository.findByProblemIdAndIsSampleTrue(match.getProblem().getId())
                    .stream()
                    .limit(3)
                    .map(tc -> new ProblemExampleDto(tc.getInput(), tc.getExpectedOutput()))
                    .collect(Collectors.toList());
            matchDto.getProblem().setExamples(examples);
        }
        return matchDto;
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getActiveMatch(User currentUser) {
        List<Match> matches = matchRepository.findActiveMatchesByUserId(
                currentUser.getId(),
                org.springframework.data.domain.PageRequest.of(0, 1)
        );
        if (!matches.isEmpty()) {
            Match m = matches.get(0);
            return java.util.Map.of(
                    "active", true,
                    "matchId", m.getId().toString()
            );
        }
        return java.util.Map.of("active", false);
    }
}
