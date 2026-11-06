package com.example.appbackend.service;

import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight per-instance rate limiter for abuse-prone write endpoints.
 *
 * <p>This is intentionally dependency-free and is adequate for a single
 * backend instance. Multi-instance deployments should replace the backing map
 * with Redis so limits are shared across nodes.</p>
 */
@Component
public class SimpleRateLimiter {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public void check(String key, int limit, Duration duration, String message) {
        if (!tryAcquire(key, limit, duration)) {
            throw new BusinessException(429, message);
        }
    }

    public boolean tryAcquire(String key, int limit, Duration duration) {
        if (key == null || key.isBlank() || limit < 1 || duration == null || duration.isNegative()) {
            return true;
        }
        long now = System.currentTimeMillis();
        long windowMillis = Math.max(1L, duration.toMillis());
        final boolean[] allowed = {false};
        windows.compute(key, (ignored, current) -> {
            if (current == null || current.resetAt <= now) {
                allowed[0] = true;
                return new Window(now + windowMillis, new AtomicInteger(1));
            }
            if (current.count.get() < limit) {
                allowed[0] = true;
                current.count.incrementAndGet();
            }
            return current;
        });
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.entrySet().removeIf(entry -> entry.getValue().resetAt <= now);
        }
        return allowed[0];
    }

    private static final class Window {
        private final long resetAt;
        private final AtomicInteger count;

        private Window(long resetAt, AtomicInteger count) {
            this.resetAt = resetAt;
            this.count = count;
        }
    }
}
