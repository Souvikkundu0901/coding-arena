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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final TestCaseRepository testCaseRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    @Autowired
    public MatchService(MatchRepository matchRepository,
                        TestCaseRepository testCaseRepository,
                        SimpMessageSendingOperations messagingTemplate) {
        this.matchRepository = matchRepository;
        this.testCaseRepository = testCaseRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public MatchService(MatchRepository matchRepository, SimpMessageSendingOperations messagingTemplate) {
        this(matchRepository, null, messagingTemplate);
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

        MatchDto matchDto = MatchDto.fromEntity(match);
        if (testCaseRepository != null && matchDto.getProblem() != null) {
            List<ProblemExampleDto> examples = testCaseRepository.findByProblemIdAndIsSampleTrue(match.getProblem().getId())
                    .stream()
                    .limit(3)
                    .map(tc -> new ProblemExampleDto(tc.getInput(), tc.getExpectedOutput()))
                    .collect(Collectors.toList());
            matchDto.getProblem().setExamples(examples);
        }

        return matchDto;
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
    }
}
