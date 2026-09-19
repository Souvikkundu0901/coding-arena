package com.codingarena.common.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
    }

    @Test
    void tryConsume_AllowsUpToCapacity_ThenRejects() {
        String key = "test-ip-1";
        int capacity = 3;

        assertTrue(rateLimitService.tryConsume(key, capacity, capacity, Duration.ofMinutes(1)));
        assertTrue(rateLimitService.tryConsume(key, capacity, capacity, Duration.ofMinutes(1)));
        assertTrue(rateLimitService.tryConsume(key, capacity, capacity, Duration.ofMinutes(1)));

        // 4th request exceeds capacity
        assertFalse(rateLimitService.tryConsume(key, capacity, capacity, Duration.ofMinutes(1)));
    }

    @Test
    void tryConsume_SeparateKeys_HaveIsolatedLimits() {
        String key1 = "user-1";
        String key2 = "user-2";
        int capacity = 2;

        assertTrue(rateLimitService.tryConsume(key1, capacity, capacity, Duration.ofMinutes(1)));
        assertTrue(rateLimitService.tryConsume(key1, capacity, capacity, Duration.ofMinutes(1)));
        assertFalse(rateLimitService.tryConsume(key1, capacity, capacity, Duration.ofMinutes(1)));

        // key2 is completely unaffected
        assertTrue(rateLimitService.tryConsume(key2, capacity, capacity, Duration.ofMinutes(1)));
        assertTrue(rateLimitService.tryConsume(key2, capacity, capacity, Duration.ofMinutes(1)));
        assertFalse(rateLimitService.tryConsume(key2, capacity, capacity, Duration.ofMinutes(1)));
    }

    @Test
    void clear_ResetsAllBuckets() {
        String key = "test-key";
        int capacity = 1;

        assertTrue(rateLimitService.tryConsume(key, capacity, capacity, Duration.ofMinutes(1)));
        assertFalse(rateLimitService.tryConsume(key, capacity, capacity, Duration.ofMinutes(1)));

        rateLimitService.clear();

        // Bucket reset after clear
        assertTrue(rateLimitService.tryConsume(key, capacity, capacity, Duration.ofMinutes(1)));
    }
}
