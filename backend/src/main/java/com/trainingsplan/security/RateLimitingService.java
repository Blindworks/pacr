package com.trainingsplan.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory rate limiter for auth endpoints.
 * Tracks attempts per key (IP or email) within a sliding time window.
 */
@Service
public class RateLimitingService {

    private static final int MAX_LOGIN_ATTEMPTS = 10;
    private static final int MAX_REGISTER_ATTEMPTS = 5;
    private static final int MAX_VERIFICATION_ATTEMPTS = 10;
    private static final long WINDOW_SECONDS = 900; // 15 minutes

    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();

    public boolean isLoginRateLimited(String key) {
        return isRateLimited("login:" + key, MAX_LOGIN_ATTEMPTS);
    }

    public boolean isRegisterRateLimited(String key) {
        return isRateLimited("register:" + key, MAX_REGISTER_ATTEMPTS);
    }

    public boolean isVerificationRateLimited(String key) {
        return isRateLimited("verify:" + key, MAX_VERIFICATION_ATTEMPTS);
    }

    public void recordLoginAttempt(String key) {
        record("login:" + key);
    }

    public void recordRegisterAttempt(String key) {
        record("register:" + key);
    }

    public void recordVerificationAttempt(String key) {
        record("verify:" + key);
    }

    private boolean isRateLimited(String key, int maxAttempts) {
        RateBucket bucket = buckets.get(key);
        if (bucket == null) {
            return false;
        }
        if (bucket.isExpired()) {
            buckets.remove(key);
            return false;
        }
        return bucket.count.get() >= maxAttempts;
    }

    private void record(String key) {
        buckets.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new RateBucket();
            }
            existing.count.incrementAndGet();
            return existing;
        });
    }

    /**
     * Periodic cleanup of expired buckets. Called by Spring scheduler.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 300_000) // every 5 min
    public void cleanup() {
        buckets.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    private static class RateBucket {
        final AtomicInteger count = new AtomicInteger(1);
        final Instant windowStart = Instant.now();

        boolean isExpired() {
            return Instant.now().isAfter(windowStart.plusSeconds(WINDOW_SECONDS));
        }
    }
}
