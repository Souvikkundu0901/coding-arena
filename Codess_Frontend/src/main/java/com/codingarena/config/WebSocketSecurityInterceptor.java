package com.codingarena.config;

import com.codingarena.auth.security.JwtTokenProvider;
import com.codingarena.match.model.Match;
import com.codingarena.match.repository.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSecurityInterceptor.class);

    private final JwtTokenProvider tokenProvider;
    private final MatchRepository matchRepository;

    public WebSocketSecurityInterceptor(JwtTokenProvider tokenProvider, MatchRepository matchRepository) {
        this.tokenProvider = tokenProvider;
        this.matchRepository = matchRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();

            if (destination != null) {
                if (destination.startsWith("/topic/match/")) {
                    String matchIdStr = destination.substring("/topic/match/".length());
                    try {
                        UUID matchId = UUID.fromString(matchIdStr);
                        String token = extractToken(accessor);

                        if (!StringUtils.hasText(token) || !tokenProvider.validateToken(token)) {
                            log.warn("Unauthorized STOMP subscription attempt to {}", destination);
                            throw new AccessDeniedException("Invalid or missing JWT token for WebSocket subscription");
                        }

                        UUID userId = tokenProvider.getUserIdFromToken(token);
                        Optional<Match> matchOpt = matchRepository.findById(matchId);

                        if (matchOpt.isEmpty()) {
                            log.warn("STOMP subscription attempt for non-existent match {}", matchId);
                            throw new AccessDeniedException("Match not found");
                        }

                        Match match = matchOpt.get();
                        boolean isPlayerA = match.getPlayerA() != null && match.getPlayerA().getId().equals(userId);
                        boolean isPlayerB = match.getPlayerB() != null && match.getPlayerB().getId().equals(userId);

                        if (!isPlayerA && !isPlayerB) {
                            log.warn("User {} unauthorized to subscribe to match {}", userId, matchId);
                            throw new AccessDeniedException("Access denied to match topic");
                        }

                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid match ID in destination {}", destination);
                        throw new AccessDeniedException("Invalid match ID destination");
                    }
                } else if (destination.startsWith("/topic/user/")) {
                    String targetUserIdStr = destination.substring("/topic/user/".length());
                    try {
                        UUID targetUserId = UUID.fromString(targetUserIdStr);
                        String token = extractToken(accessor);

                        if (!StringUtils.hasText(token) || !tokenProvider.validateToken(token)) {
                            log.warn("Unauthorized STOMP subscription attempt to {}", destination);
                            throw new AccessDeniedException("Invalid or missing JWT token for personal notification topic");
                        }

                        UUID authenticatedUserId = tokenProvider.getUserIdFromToken(token);
                        if (!authenticatedUserId.equals(targetUserId)) {
                            log.warn("User {} unauthorized to subscribe to personal topic of {}", authenticatedUserId, targetUserId);
                            throw new AccessDeniedException("Access denied to personal user topic");
                        }

                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid user ID in destination {}", destination);
                        throw new AccessDeniedException("Invalid user ID destination");
                    }
                }
            }
        }
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        List<String> authorization = accessor.getNativeHeader("Authorization");
        if (authorization != null && !authorization.isEmpty()) {
            String bearerToken = authorization.get(0);
            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
        }

        List<String> tokenHeader = accessor.getNativeHeader("token");
        if (tokenHeader != null && !tokenHeader.isEmpty()) {
            return tokenHeader.get(0);
        }

        return null;
    }
}
