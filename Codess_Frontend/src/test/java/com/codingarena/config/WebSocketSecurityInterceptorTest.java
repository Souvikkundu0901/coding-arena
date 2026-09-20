package com.codingarena.config;

import com.codingarena.auth.model.User;
import com.codingarena.auth.security.JwtTokenProvider;
import com.codingarena.match.model.Match;
import com.codingarena.match.model.Problem;
import com.codingarena.match.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketSecurityInterceptorTest {

    private JwtTokenProvider tokenProvider;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MessageChannel messageChannel;

    private WebSocketSecurityInterceptor interceptor;

    private UUID user1Id;
    private UUID user2Id;
    private UUID matchId;
    private Match match;

    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "c3VwZXItc2VjcmV0LWtleS1mb3ItY29kaW5nLWFyZW5hLWp3dC1hdXRoZW50aWNhdGlvbi0yNTYtYml0cw==",
                86400000L
        );
        interceptor = new WebSocketSecurityInterceptor(tokenProvider, matchRepository);

        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();
        matchId = UUID.randomUUID();

        User playerA = new User("playerA", "a@example.com", "hash");
        playerA.setId(user1Id);

        User playerB = new User("playerB", "b@example.com", "hash");
        playerB.setId(user2Id);

        Problem problem = new Problem("Title", "EASY", "Desc");
        match = new Match(playerA, playerB, problem, "IN_PROGRESS", null);
        match.setId(matchId);

        user1Token = tokenProvider.generateToken(user1Id, "a@example.com");
        user2Token = tokenProvider.generateToken(user2Id, "b@example.com");
    }

    @Test
    void subscribe_TopicUser_MatchingUser_Allowed() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/user/" + user1Id);
        accessor.setNativeHeader("Authorization", "Bearer " + user1Token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertNotNull(result);
    }

    @Test
    void subscribe_TopicUser_MismatchedUser_ThrowsAccessDeniedException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/user/" + user1Id);
        accessor.setNativeHeader("Authorization", "Bearer " + user2Token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    void subscribe_TopicMatch_PlayerInMatch_Allowed() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/match/" + matchId);
        accessor.setNativeHeader("Authorization", "Bearer " + user1Token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertNotNull(result);
    }

    @Test
    void subscribe_TopicMatch_Outsider_ThrowsAccessDeniedException() {
        UUID outsiderId = UUID.randomUUID();
        String outsiderToken = tokenProvider.generateToken(outsiderId, "outsider@example.com");

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/match/" + matchId);
        accessor.setNativeHeader("Authorization", "Bearer " + outsiderToken);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, messageChannel));
    }
}
