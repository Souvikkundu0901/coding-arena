package com.codingarena.match.service;

import com.codingarena.auth.model.User;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.match.dto.MatchDto;
import com.codingarena.match.model.Match;
import com.codingarena.match.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchSessionTrackerTest {

    private static class StubMatchService extends MatchService {
        boolean forfeitCalled = false;

        public StubMatchService() {
            super(null, null, null, null);
        }

        @Override
        public MatchDto forfeitMatch(UUID matchId, User currentUser) {
            forfeitCalled = true;
            return null;
        }
    }

    private StubMatchService matchService;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private UserRepository userRepository;

    private MatchSessionTracker tracker;

    private UUID userId;
    private UUID matchId;
    private Match match;

    @BeforeEach
    void setUp() {
        matchService = new StubMatchService();
        tracker = new MatchSessionTracker(matchService, matchRepository, userRepository);
        userId = UUID.randomUUID();
        matchId = UUID.randomUUID();

        match = new Match();
        match.setId(matchId);
        match.setStatus("IN_PROGRESS");
    }

    private SessionDisconnectEvent createDisconnectEvent(String sessionId) {
        return new SessionDisconnectEvent(this, new GenericMessage<>(new byte[0]), sessionId, CloseStatus.NORMAL);
    }

    @Test
    void disconnect_UnknownSession_DoesNothing() {
        tracker.handleSessionDisconnect(createDisconnectEvent("unknown-session"));

        verifyNoInteractions(matchRepository);
        assertFalse(matchService.forfeitCalled);
    }

    @Test
    void disconnect_UserHasOtherActiveSession_DoesNotScheduleForfeit() {
        String session1 = "session-1";
        String session2 = "session-2";

        tracker.registerSession(session1, userId, matchId);
        tracker.registerSession(session2, userId, matchId);

        tracker.handleSessionDisconnect(createDisconnectEvent(session1));

        verifyNoInteractions(matchRepository);
        assertFalse(matchService.forfeitCalled);
    }

    @Test
    void disconnect_MatchNotInProgress_DoesNotScheduleForfeit() {
        String session = "session-1";
        tracker.registerSession(session, userId, matchId);

        match.setStatus("COMPLETED");
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        tracker.handleSessionDisconnect(createDisconnectEvent(session));

        verify(matchRepository).findById(matchId);
        assertFalse(matchService.forfeitCalled);
    }

    @Test
    void reconnect_CancelsPendingForfeit() {
        String session1 = "session-1";
        tracker.registerSession(session1, userId, matchId);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        tracker.handleSessionDisconnect(createDisconnectEvent(session1));

        // Reconnect with new session
        String session2 = "session-2";
        tracker.registerSession(session2, userId, matchId);

        // Verify forfeit is not called immediately
        assertFalse(matchService.forfeitCalled);
    }
}
