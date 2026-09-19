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

    @Mock
    private com.codingarena.auth.repository.UserRepository userRepository;

    private com.codingarena.submission.service.EloService eloService;

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

        eloService = new com.codingarena.submission.service.EloService(userRepository, matchRepository);
        matchService = new MatchService(matchRepository, testCaseRepository, messagingTemplate, eloService);
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

    @Test
    void forfeitMatch_PlayerAForfeits_OpponentPlayerBWins() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchRepository.forfeitMatchAtomically(matchId, playerB.getId())).thenReturn(1);

        MatchDto result = matchService.forfeitMatch(matchId, playerA);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(playerB.getId(), result.getWinnerId());
        verify(userRepository).save(playerB);
        verify(userRepository).save(playerA);
        assertEquals(1, playerB.getWins());
        assertEquals(1, playerA.getLosses());

        org.mockito.ArgumentCaptor<com.codingarena.match.dto.MatchEvent> eventCaptor =
                org.mockito.ArgumentCaptor.forClass(com.codingarena.match.dto.MatchEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/match/" + matchId), eventCaptor.capture());
        assertEquals("MATCH_END", eventCaptor.getValue().getType());
        assertEquals("FORFEIT", eventCaptor.getValue().getReason());
        assertEquals(playerB.getId(), eventCaptor.getValue().getMatch().getWinnerId());
    }

    @Test
    void forfeitMatch_PlayerBForfeits_OpponentPlayerAWins() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchRepository.forfeitMatchAtomically(matchId, playerA.getId())).thenReturn(1);

        MatchDto result = matchService.forfeitMatch(matchId, playerB);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(playerA.getId(), result.getWinnerId());
        verify(userRepository).save(playerA);
        verify(userRepository).save(playerB);
        assertEquals(1, playerA.getWins());
        assertEquals(1, playerB.getLosses());
        verify(messagingTemplate).convertAndSend(eq("/topic/match/" + matchId), any(com.codingarena.match.dto.MatchEvent.class));
    }

    @Test
    void forfeitMatch_Spectator_ThrowsMatchAccessDeniedException() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThrows(MatchAccessDeniedException.class, () -> matchService.forfeitMatch(matchId, spectator));
        verify(matchRepository, never()).forfeitMatchAtomically(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void forfeitMatch_NonExistent_ThrowsMatchNotFoundException() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

        assertThrows(MatchNotFoundException.class, () -> matchService.forfeitMatch(matchId, playerA));
        verify(matchRepository, never()).forfeitMatchAtomically(any(), any());
    }

    @Test
    void forfeitMatch_AlreadyCompleted_ThrowsMatchCompletedException() {
        match.setStatus("COMPLETED");
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThrows(com.codingarena.submission.exception.MatchCompletedException.class,
                () -> matchService.forfeitMatch(matchId, playerA));
        verify(matchRepository, never()).forfeitMatchAtomically(any(), any());
    }

    @Test
    void forfeitMatch_AlreadyExpired_ThrowsMatchCompletedException() {
        match.setStatus("EXPIRED");
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThrows(com.codingarena.submission.exception.MatchCompletedException.class,
                () -> matchService.forfeitMatch(matchId, playerA));
        verify(matchRepository, never()).forfeitMatchAtomically(any(), any());
    }

    @Test
    void forfeitMatch_AtomicRaceConditionReturnsZero_ReturnsCurrentMatchWithoutEloUpdate() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchRepository.forfeitMatchAtomically(matchId, playerB.getId())).thenReturn(0);

        MatchDto result = matchService.forfeitMatch(matchId, playerA);

        assertNotNull(result);
        verify(userRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
