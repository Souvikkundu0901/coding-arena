package com.codingarena.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, int capacity, int refillTokens, Duration period) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> createNewBucket(capacity, refillTokens, period));
        return bucket.tryConsume(1);
    }

    private Bucket createNewBucket(int capacity, int refillTokens, Duration period) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(refillTokens, period));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public void clear() {
        buckets.clear();
    }
}
