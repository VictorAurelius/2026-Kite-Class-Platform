package com.kitehub.branding.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Redis-backed distributed rate limiter + concurrency semaphore (GAP-005a).
 *
 * <p>Two primitives are exposed:
 * <ul>
 *   <li><b>Daily counter</b> — {@code ai:ratelimit:{instanceId}:{yyyy-MM-dd}}
 *   with a 24h TTL; atomic {@code INCR} gives the new usage count.</li>
 *   <li><b>Concurrency semaphore</b> — {@code ai:concurrency:{instanceId}}
 *   tracks in-flight jobs; {@link #tryAcquireConcurrencySlot} increments iff
 *   under {@code maxConcurrent}, {@link #releaseConcurrencySlot} decrements.</li>
 * </ul>
 *
 * <p>Redis is optional — when the template is not autowired (e.g. unit tests
 * without Redis), {@link #isAvailable()} returns {@code false} and callers
 * should fall back to the JPA-based counter.</p>
 *
 * <h3>Key design</h3>
 * <pre>
 *   ai:ratelimit:{instanceId}:{YYYY-MM-DD}   -> Long (daily usage), TTL 24h
 *   ai:concurrency:{instanceId}              -> Long (in-flight jobs)
 * </pre>
 *
 * @since 1.0
 */
@Slf4j
@Component
public class DistributedRateLimiter {

    static final String DAILY_KEY_PREFIX = "ai:ratelimit:";
    static final String CONCURRENCY_KEY_PREFIX = "ai:concurrency:";
    // GAP-1137: FULL_AI (paid GPT image-gen) monthly cost counter.
    static final String FULLAI_MONTHLY_KEY_PREFIX = "ai:fullai:";
    static final Duration DAILY_TTL = Duration.ofDays(1);
    static final Duration FULLAI_MONTHLY_TTL = Duration.ofDays(32);

    private final StringRedisTemplate redisTemplate;

    /**
     * @param redisTemplateProvider optional Redis template — Spring's {@link ObjectProvider}
     *                              returns {@code null} from {@code getIfAvailable()} when
     *                              Redis autoconfiguration is disabled (e.g. unit tests).
     */
    public DistributedRateLimiter(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        if (this.redisTemplate == null) {
            log.warn("DistributedRateLimiter started WITHOUT Redis — callers must use DB fallback");
        }
    }

    /**
     * @return true if Redis is wired and reachable from the container
     */
    public boolean isAvailable() {
        return redisTemplate != null;
    }

    /**
     * Atomically increment the daily counter and return the new value.
     *
     * <p>On first hit of the day, sets TTL to 24h. Returns -1 on any error,
     * signalling the caller to fall back to the DB counter.</p>
     *
     * @param instanceId tenant id
     * @return new counter value, or -1 on failure
     */
    public long incrementDailyUsage(UUID instanceId) {
        if (!isAvailable()) {
            return -1;
        }
        String key = dailyKey(instanceId, LocalDate.now());
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                // First increment of the day — set TTL so the key expires.
                redisTemplate.expire(key, DAILY_TTL);
            }
            return count == null ? -1 : count;
        } catch (RuntimeException ex) {
            log.warn("Redis INCR failed for {} — caller should fall back to DB: {}",
                    key, ex.getMessage());
            return -1;
        }
    }

    /**
     * Read today's daily counter without incrementing.
     *
     * @param instanceId tenant id
     * @return current count, or -1 if Redis unavailable / key missing
     */
    public long getDailyUsage(UUID instanceId) {
        if (!isAvailable()) {
            return -1;
        }
        String key = dailyKey(instanceId, LocalDate.now());
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? 0L : Long.parseLong(value);
        } catch (RuntimeException ex) {
            log.warn("Redis GET failed for {}: {}", key, ex.getMessage());
            return -1;
        }
    }

    /**
     * Try to acquire a concurrency slot (best-effort compare-and-increment).
     *
     * <p>Implementation: {@code INCR} then compare to cap. If over cap,
     * {@code DECR} to roll back. Not strictly atomic but acceptable for
     * soft concurrency caps — tighter guarantees would need a Lua script.</p>
     *
     * @param instanceId    tenant id
     * @param maxConcurrent maximum allowed in-flight jobs
     * @return true if slot acquired, false if cap reached or Redis unavailable
     */
    public boolean tryAcquireConcurrencySlot(UUID instanceId, int maxConcurrent) {
        if (!isAvailable()) {
            return false;
        }
        String key = concurrencyKey(instanceId);
        try {
            Long current = redisTemplate.opsForValue().increment(key);
            if (current != null && current <= maxConcurrent) {
                return true;
            }
            // Roll back — we over-incremented beyond the cap.
            redisTemplate.opsForValue().decrement(key);
            return false;
        } catch (RuntimeException ex) {
            log.warn("Redis concurrency check failed for {}: {}", key, ex.getMessage());
            return false;
        }
    }

    /**
     * Release a previously-acquired concurrency slot. Clamps at zero.
     *
     * @param instanceId tenant id
     */
    public void releaseConcurrencySlot(UUID instanceId) {
        if (!isAvailable()) {
            return;
        }
        String key = concurrencyKey(instanceId);
        try {
            Long remaining = redisTemplate.opsForValue().decrement(key);
            if (remaining != null && remaining < 0L) {
                // Clamp — never let counter go negative.
                redisTemplate.opsForValue().set(key, "0");
            }
        } catch (RuntimeException ex) {
            log.warn("Redis DECR failed for {}: {}", key, ex.getMessage());
        }
    }

    /**
     * Read current in-flight job count.
     *
     * @param instanceId tenant id
     * @return current count, or -1 if Redis unavailable
     */
    public long getConcurrencyCount(UUID instanceId) {
        if (!isAvailable()) {
            return -1;
        }
        String key = concurrencyKey(instanceId);
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? 0L : Long.parseLong(value);
        } catch (RuntimeException ex) {
            log.warn("Redis GET failed for {}: {}", key, ex.getMessage());
            return -1;
        }
    }

    /**
     * Read this month's FULL_AI usage counter without incrementing (GAP-1137).
     *
     * @param instanceId tenant id
     * @return current month's FULL_AI count, or -1 if Redis unavailable
     */
    public long getMonthlyFullAiUsage(UUID instanceId) {
        if (!isAvailable()) {
            return -1;
        }
        String key = monthlyFullAiKey(instanceId, YearMonth.now());
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? 0L : Long.parseLong(value);
        } catch (RuntimeException ex) {
            log.warn("Redis GET failed for {}: {}", key, ex.getMessage());
            return -1;
        }
    }

    /**
     * Atomically increment this month's FULL_AI usage counter (GAP-1137).
     *
     * <p>On the first hit of the month, sets a ~32-day TTL so the key expires.
     * Returns -1 on any error (Redis unavailable / INCR failure).</p>
     *
     * @param instanceId tenant id
     * @return new counter value, or -1 on failure
     */
    public long incrementMonthlyFullAiUsage(UUID instanceId) {
        if (!isAvailable()) {
            return -1;
        }
        String key = monthlyFullAiKey(instanceId, YearMonth.now());
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, FULLAI_MONTHLY_TTL);
            }
            return count == null ? -1 : count;
        } catch (RuntimeException ex) {
            log.warn("Redis INCR failed for {}: {}", key, ex.getMessage());
            return -1;
        }
    }

    static String dailyKey(UUID instanceId, LocalDate date) {
        return DAILY_KEY_PREFIX + instanceId + ":" + date;
    }

    static String concurrencyKey(UUID instanceId) {
        return CONCURRENCY_KEY_PREFIX + instanceId;
    }

    static String monthlyFullAiKey(UUID instanceId, YearMonth month) {
        return FULLAI_MONTHLY_KEY_PREFIX + instanceId + ":" + month;
    }
}
