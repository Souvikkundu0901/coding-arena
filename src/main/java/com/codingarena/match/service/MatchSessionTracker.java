package com.codingarena.match.service;

import com.codingarena.auth.model.User;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.match.model.Match;
import com.codingarena.match.repository.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
public class MatchSessionTracker {

    private static final Logger log = LoggerFactory.getLogger(MatchSessionTracker.class);
    private static final long DISCONNECT_GRACE_PERIOD_MS = 6000; // 6-second grace period for page refresh / reconnects

    public static class SessionInfo {
        private final UUID userId;
        private final UUID matchId;

        public SessionInfo(UUID userId, UUID matchId) {
            this.userId = userId;
            this.matchId = matchId;
        }

        public UUID getUserId() {
            return userId;
        }

        public UUID getMatchId() {
            return matchId;
        }
    }

    private final Map<String, SessionInfo> sessionToMatchMap = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> pendingForfeitTasks = new ConcurrentHashMap<>();

    private final MatchService matchService;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final ThreadPoolTaskScheduler scheduler;

    public MatchSessionTracker(@org.springframework.context.annotation.Lazy MatchService matchService,
                               MatchRepository matchRepository,
                               UserRepository userRepository) {
        this.matchService = matchService;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;

        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(2);
        taskScheduler.setThreadNamePrefix("match-disconnect-");
        taskScheduler.initialize();
        this.scheduler = taskScheduler;
    }

    /**
     * Registers an active WebSocket session subscription for a user in a match.
     */
    public void registerSession(String sessionId, UUID userId, UUID matchId) {
        if (sessionId == null || userId == null || matchId == null) {
            return;
        }
        sessionToMatchMap.put(sessionId, new SessionInfo(userId, matchId));
        String key = matchId + ":" + userId;
        ScheduledFuture<?> future = pendingForfeitTasks.remove(key);
        if (future != null) {
            future.cancel(false);
            log.info("User {} reconnected to match {}. Canceled pending auto-forfeit.", userId, matchId);
        }
    }

    /**
     * Unregisters an unsubscription or disconnect.
     */
    public void unregisterSession(String sessionId) {
        if (sessionId != null) {
            sessionToMatchMap.remove(sessionId);
        }
    }

    /**
     * Handles abrupt WebSocket disconnection.
     * If the disconnected user has no other active WebSocket session in the match,
     * schedules an auto-forfeit after the grace period.
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        SessionInfo info = sessionToMatchMap.remove(sessionId);
        if (info == null) {
            return;
        }

        UUID matchId = info.getMatchId();
        UUID userId = info.getUserId();

        // Check if the user still has another active session for this match
        boolean hasOtherSession = sessionToMatchMap.values().stream()
                .anyMatch(s -> s.getMatchId().equals(matchId) && s.getUserId().equals(userId));
        if (hasOtherSession) {
            log.debug("User {} disconnected session {}, but still has other active sessions in match {}",
                    userId, sessionId, matchId);
            return;
        }

        Optional<Match> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isEmpty() || !"IN_PROGRESS".equals(matchOpt.get().getStatus())) {
            return;
        }

        log.info("User {} disconnected from active match {}. Scheduling auto-forfeit in {}ms",
                userId, matchId, DISCONNECT_GRACE_PERIOD_MS);

        String key = matchId + ":" + userId;
        ScheduledFuture<?> task = scheduler.schedule(() -> {
            pendingForfeitTasks.remove(key);
            try {
                Optional<Match> currentMatchOpt = matchRepository.findById(matchId);
                if (currentMatchOpt.isPresent() && "IN_PROGRESS".equals(currentMatchOpt.get().getStatus())) {
                    log.info("Disconnect grace period elapsed for user {} in match {}. Triggering auto-forfeit.",
                            userId, matchId);
                    Optional<User> userOpt = userRepository.findById(userId);
                    if (userOpt.isPresent()) {
                        matchService.forfeitMatch(matchId, userOpt.get());
                    }
                }
            } catch (Exception e) {
                log.error("Error executing auto-forfeit for user {} in match {}", userId, matchId, e);
            }
        }, Instant.now().plusMillis(DISCONNECT_GRACE_PERIOD_MS));

        pendingForfeitTasks.put(key, task);
    }
}
