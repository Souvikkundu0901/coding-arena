package com.codingarena.matchmaking.service;

import com.codingarena.auth.model.User;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.match.model.Match;
import com.codingarena.match.model.Problem;
import com.codingarena.match.repository.MatchRepository;
import com.codingarena.match.repository.ProblemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchmakingServiceTest {

    @Mock
    private RedisOperations<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    private RedisScript<List> matchPairingScript;
    private MatchmakingService matchmakingService;

    private User user1;
    private User user2;
    private Problem mockProblem;

    @BeforeEach
    void setUp() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText("return {}");
        script.setResultType(List.class);
        com.codingarena.match.service.MatchCreationService matchCreationService =
                new com.codingarena.match.service.MatchCreationService(problemRepository, matchRepository, messagingTemplate, 30L);

        matchmakingService = new MatchmakingService(
                redisTemplate,
                matchPairingScript,
                userRepository,
                matchCreationService
        );

        user1 = new User("user1", "user1@example.com", "hash1");
        user1.setId(UUID.randomUUID());
        user1.setRating(1200);

        user2 = new User("user2", "user2@example.com", "hash2");
        user2.setId(UUID.randomUUID());
        user2.setRating(1220);

        mockProblem = new Problem("Sample", "EASY", "Desc");
        mockProblem.setId(UUID.randomUUID());
    }

    @Test
    void joinQueue_AddsUserToRedisZSet() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        matchmakingService.joinQueue(user1);

        verify(zSetOperations).add("matchmaking:queue", user1.getId().toString(), 1200.0);
    }

    @Test
    void leaveQueue_RemovesUserFromRedisZSet() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        matchmakingService.leaveQueue(user1);

        verify(zSetOperations).remove("matchmaking:queue", user1.getId().toString());
    }

    @Test
    void processQueue_PairsUsersWhenLuaScriptReturnsPair() {
        List<String> matchedIds = List.of(user1.getId().toString(), user2.getId().toString());
        when(redisTemplate.execute(eq(matchPairingScript), anyList())).thenReturn(matchedIds);

        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(problemRepository.findRandomProblem()).thenReturn(Optional.of(mockProblem));

        Match savedMatch = new Match(user1, user2, mockProblem, "IN_PROGRESS", null);
        savedMatch.setId(UUID.randomUUID());
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);

        matchmakingService.processQueue();

        verify(matchRepository).save(any(Match.class));
        // Verify per-user notifications
        verify(messagingTemplate).convertAndSend(eq("/topic/user/" + user1.getId()), any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/user/" + user2.getId()), any(Object.class));
        // Verify match topic notifications
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/match/" + savedMatch.getId()), any(Object.class));
    }
}
