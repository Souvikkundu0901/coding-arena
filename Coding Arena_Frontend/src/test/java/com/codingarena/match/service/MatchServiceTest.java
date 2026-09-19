package com.codingarena.match.service;

import com.codingarena.auth.model.User;
import com.codingarena.match.dto.MatchDto;
import com.codingarena.match.exception.MatchAccessDeniedException;
import com.codingarena.match.exception.MatchNotFoundException;
import com.codingarena.match.model.Match;
import com.codingarena.match.model.Problem;
import com.codingarena.match.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private com.codingarena.match.repository.TestCaseRepository testCaseRepository;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @InjectMocks
    private MatchService matchService;

    private User playerA;
    private User playerB;
    private User spectator;
    private Match match;
    private UUID matchId;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();

        playerA = new User("playerA", "a@example.com", "pass");
        playerA.setId(UUID.randomUUID());

        playerB = new User("playerB", "b@example.com", "pass");
        playerB.setId(UUID.randomUUID());

        spectator = new User("spectator", "spec@example.com", "pass");
        spectator.setId(UUID.randomUUID());

        Problem problem = new Problem("Test Problem", "MEDIUM", "Description");
        problem.setId(UUID.randomUUID());

        match = new Match(playerA, playerB, problem, "IN_PROGRESS", null);
        match.setId(matchId);
    }

    @Test
    void getMatchDetails_PlayerA_ReturnsMatchDto() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        MatchDto dto = matchService.getMatchDetails(matchId, playerA);

        assertNotNull(dto);
        assertEquals(matchId, dto.getId());
        assertEquals(playerA.getId(), dto.getPlayerAId());
    }

    @Test
    void getMatchDetails_PlayerB_ReturnsMatchDto() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        MatchDto dto = matchService.getMatchDetails(matchId, playerB);

        assertNotNull(dto);
        assertEquals(matchId, dto.getId());
        assertEquals(playerB.getId(), dto.getPlayerBId());
    }

    @Test
    void getMatchDetails_Spectator_ThrowsMatchAccessDeniedException() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThrows(MatchAccessDeniedException.class, () -> matchService.getMatchDetails(matchId, spectator));
    }

    @Test
    void getMatchDetails_NonExistent_ThrowsMatchNotFoundException() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

        assertThrows(MatchNotFoundException.class, () -> matchService.getMatchDetails(matchId, playerA));
    }

    @Test
    void setWinnerAtomically_Success_ReturnsTrueAndBroadcastsEndEvent() {
        when(matchRepository.setWinnerAtomically(matchId, playerA.getId())).thenReturn(1);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        boolean updated = matchService.setWinnerAtomically(matchId, playerA.getId());

        assertTrue(updated);
        verify(messagingTemplate).convertAndSend(eq("/topic/match/" + matchId), any(Object.class));
    }

    @Test
    void setWinnerAtomically_AlreadyCompleted_ReturnsFalse() {
        when(matchRepository.setWinnerAtomically(matchId, playerA.getId())).thenReturn(0);

        boolean updated = matchService.setWinnerAtomically(matchId, playerA.getId());

        assertFalse(updated);
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
