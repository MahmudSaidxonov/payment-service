package uz.example.payment.auth.service;

import org.springframework.stereotype.Component;
import uz.example.payment.common.RateLimitExceededException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private static class AttemptCounter {
        int count = 0;
        Instant resetAt = Instant.now().plus(WINDOW);
    }

    private final Map<String, AttemptCounter> counters = new ConcurrentHashMap<>();

    public void checkRateLimit(String key) {
        Instant now = Instant.now();
        AttemptCounter counter = counters.computeIfAbsent(key, k -> new AttemptCounter());

        synchronized (counter) {
            if (now.isAfter(counter.resetAt)) {
                counter.count = 0;
                counter.resetAt = now.plus(WINDOW);
            }

            if (counter.count >= MAX_ATTEMPTS) {
                throw new RateLimitExceededException("Too many login attempts. Please wait 1 minute.");
            }
            counter.count++;
        }
    }
}
