package com.codingarena.match.service;

import com.codingarena.auth.model.User;
import com.codingarena.match.dto.MatchEvent;
import com.codingarena.match.model.Match;
import com.codingarena.match.model.Problem;
import com.codingarena.match.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchExpiryServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private com.codingarena.challenge.repository.ChallengeRepository challengeRepository;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    private MatchExpiryService matchExpiryService;

    private Match expiredMatch;
    private UUID matchId;

    @BeforeEach
    void setUp() {
        matchExpiryService = new MatchExpiryService(matchRepository, challengeRepository, messagingTemplate);

        matchId = UUID.randomUUID();
        User playerA = new User("playerA", "a@example.com", "hash");
        playerA.setId(UUID.randomUUID());
        User playerB = new User("playerB", "b@example.com", "hash");
        playerB.setId(UUID.randomUUID());
        Problem problem = new Problem("Problem", "EASY", "Desc");

        LocalDateTime startedAt = LocalDateTime.now().minusMinutes(20);
        LocalDateTime expiresAt = LocalDateTime.now().minusMinutes(5);

        expiredMatch = new Match(playerA, playerB, problem, "IN_PROGRESS", startedAt, expiresAt);
        expiredMatch.setId(matchId);
    }

    @Test
    void expireMatches_ExpiredMatchFound_ExpiresAtomicallyAndBroadcastsMatchEnd() {
        when(matchRepository.findExpiredMatches()).thenReturn(List.of(expiredMatch));
        when(matchRepository.expireMatchAtomically(matchId)).thenReturn(1);

        matchExpiryService.expireMatches();

        verify(matchRepository).expireMatchAtomically(matchId);

        ArgumentCaptor<MatchEvent> eventCaptor = ArgumentCaptor.forClass(MatchEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/match/" + matchId), eventCaptor.capture());

        MatchEvent event = eventCaptor.getValue();
        assertEquals("MATCH_END", event.getType());
        assertNotNull(event.getMatch());
        assertEquals("EXPIRED", event.getMatch().getStatus());
        assertNull(event.getMatch().getWinnerId());
        assertNotNull(event.getMatch().getEndedAt());
    }

    @Test
    void expireMatches_MatchAlreadyCompletedWithWinner_DoesNotBroadcastExpiry() {
        when(matchRepository.findExpiredMatches()).thenReturn(List.of(expiredMatch));
        // Atomic update returns 0 because match was already completed in a race condition
        when(matchRepository.expireMatchAtomically(matchId)).thenReturn(0);

        matchExpiryService.expireMatches();

        verify(matchRepository).expireMatchAtomically(matchId);
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void expireMatches_NoExpiredMatches_DoesNothing() {
        when(matchRepository.findExpiredMatches()).thenReturn(List.of());

        matchExpiryService.expireMatches();

        verify(matchRepository, never()).expireMatchAtomically(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void expireMatches_ExpiredChallengesFound_ExpiresAtomically() {
        when(matchRepository.findExpiredMatches()).thenReturn(List.of());

        com.codingarena.challenge.model.Challenge expiredChallenge = new com.codingarena.challenge.model.Challenge();
        UUID challengeId = UUID.randomUUID();
        expiredChallenge.setId(challengeId);

        when(challengeRepository.findExpiredPendingChallenges(any())).thenReturn(List.of(expiredChallenge));
        when(challengeRepository.expireChallengeAtomically(challengeId)).thenReturn(1);

        matchExpiryService.expireMatches();

        verify(challengeRepository).expireChallengeAtomically(challengeId);
    }
}
