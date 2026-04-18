package com.kitehub.branding.service;

import com.kitehub.branding.config.AIRateLimitConfig;
import com.kitehub.branding.domain.entity.AIUsageLog;
import com.kitehub.branding.repository.AIUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service for checking and recording AI usage against tier-based rate limits.
 *
 * <p>Wave 3 (GAP-005a): primary counter lives in <b>Redis</b> for horizontal
 * scaling (atomic {@code INCR} + 24h TTL). JPA {@link AIUsageLogRepository}
 * is kept as a resilience fallback when Redis is unavailable, and as the
 * long-term audit trail.</p>
 *
 * <h3>Flow</h3>
 * <pre>
 *   recordUsage()   → Redis INCR   (fast path)
 *                   → if Redis down, JPA increment (fallback)
 *   getCurrentUsage → Redis GET    (fast path)
 *                   → if Redis down, JPA SELECT
 * </pre>
 *
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIRateLimitService {

    private final AIRateLimitConfig rateLimitConfig;
    private final AIUsageLogRepository usageLogRepository;
    private final DistributedRateLimiter distributedRateLimiter;

    /**
     * Check if the given instance has exceeded its daily AI request limit.
     *
     * @param instanceId the instance UUID
     * @param tier       the subscription tier
     * @return true if the limit is exceeded, false otherwise
     */
    public boolean isRateLimited(UUID instanceId, String tier) {
        int limit = rateLimitConfig.getLimitForTier(tier);

        // -1 means unlimited
        if (limit < 0) {
            return false;
        }

        int currentUsage = getCurrentUsage(instanceId);
        boolean limited = currentUsage >= limit;

        if (limited) {
            log.warn("AI rate limit exceeded for instance {} (tier={}, usage={}, limit={})",
                    instanceId, tier, currentUsage, limit);
        }

        return limited;
    }

    /**
     * Record an AI request for the given instance.
     *
     * <p>Fast path: atomic {@code INCR} on Redis. Fallback: JPA increment
     * (original behaviour) — ensures we never lose accounting when Redis is
     * temporarily unavailable.</p>
     *
     * @param instanceId the instance UUID
     */
    @Transactional
    public void recordUsage(UUID instanceId) {
        // Try Redis first — atomic INCR + TTL.
        long redisCount = distributedRateLimiter.incrementDailyUsage(instanceId);
        if (redisCount >= 0) {
            log.debug("Recorded AI usage for instance {} via Redis (count={})",
                    instanceId, redisCount);
            return;
        }

        // Fallback to DB — Redis unavailable or errored.
        log.debug("Redis unavailable — falling back to DB counter for {}", instanceId);
        recordUsageViaDb(instanceId);
    }

    /**
     * JPA-backed fallback counter — creates or increments the per-day row.
     */
    private void recordUsageViaDb(UUID instanceId) {
        LocalDate today = LocalDate.now();
        int updated = usageLogRepository.incrementRequestCount(instanceId, today);

        if (updated == 0) {
            AIUsageLog newLog = new AIUsageLog(instanceId, today);
            usageLogRepository.save(newLog);
            log.debug("Created new AI usage log for instance {} on {}", instanceId, today);
        } else {
            log.debug("Incremented AI usage (DB) for instance {} on {}", instanceId, today);
        }
    }

    /**
     * Get the current daily usage count for an instance.
     *
     * @param instanceId the instance UUID
     * @return current request count for today
     */
    public int getCurrentUsage(UUID instanceId) {
        long redisCount = distributedRateLimiter.getDailyUsage(instanceId);
        if (redisCount >= 0) {
            return (int) redisCount;
        }
        // Fallback — DB.
        return usageLogRepository.findByInstanceIdAndUsageDate(instanceId, LocalDate.now())
                .map(AIUsageLog::getRequestCount)
                .orElse(0);
    }

    /**
     * Get the daily limit for a given tier.
     *
     * @param tier subscription tier
     * @return daily limit, -1 for unlimited
     */
    public int getDailyLimit(String tier) {
        return rateLimitConfig.getLimitForTier(tier);
    }

    /**
     * Get remaining AI requests for today.
     *
     * @param instanceId the instance UUID
     * @param tier       the subscription tier
     * @return remaining requests, or -1 for unlimited
     */
    public int getRemainingRequests(UUID instanceId, String tier) {
        int limit = rateLimitConfig.getLimitForTier(tier);
        if (limit < 0) {
            return -1; // unlimited
        }
        int usage = getCurrentUsage(instanceId);
        return Math.max(0, limit - usage);
    }
}
