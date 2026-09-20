package com.codingarena.matchmaking.service;

import com.codingarena.auth.model.User;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.match.service.MatchCreationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);
    private static final String QUEUE_KEY = "matchmaking:queue";

    private final RedisOperations<String, String> redisTemplate;
    private final RedisScript<List> matchPairingScript;
    private final UserRepository userRepository;
    private final MatchCreationService matchCreationService;

    @Autowired
    public MatchmakingService(RedisOperations<String, String> redisTemplate,
                              RedisScript<List> matchPairingScript,
                              UserRepository userRepository,
                              MatchCreationService matchCreationService) {
        this.redisTemplate = redisTemplate;
        this.matchPairingScript = matchPairingScript;
        this.userRepository = userRepository;
        this.matchCreationService = matchCreationService;
    }

    public void joinQueue(User user) {
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            log.warn("Deleted user {} attempted to join queue", user.getUsername());
            return;
        }
        double rating = user.getRating() != null ? user.getRating() : 0.0;
        redisTemplate.opsForZSet().add(QUEUE_KEY, user.getId().toString(), rating);
        log.info("User {} (rating: {}) joined matchmaking queue", user.getUsername(), rating);
        
        processQueue();
    }

    public void leaveQueue(User user) {
        redisTemplate.opsForZSet().remove(QUEUE_KEY, user.getId().toString());
        log.info("User {} left matchmaking queue", user.getUsername());
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void processQueue() {
        try {
            List<String> result = redisTemplate.execute(matchPairingScript, Collections.singletonList(QUEUE_KEY));

            if (result != null && result.size() == 2) {
                UUID user1Id = UUID.fromString(result.get(0));
                UUID user2Id = UUID.fromString(result.get(1));

                Optional<User> user1Opt = userRepository.findById(user1Id);
                Optional<User> user2Opt = userRepository.findById(user2Id);

                if (user1Opt.isPresent() && user2Opt.isPresent()) {
                    matchCreationService.createMatchAndNotify(user1Opt.get(), user2Opt.get());
                } else {
                    log.warn("One or both matched users not found in DB: {}, {}", user1Id, user2Id);
                }
            }
        } catch (Exception e) {
            log.error("Error processing matchmaking queue", e);
        }
    }
}
