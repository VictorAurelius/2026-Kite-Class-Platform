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
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIRateLimitService {

    private final AIRateLimitConfig rateLimitConfig;
    private final AIUsageLogRepository usageLogRepository;

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
     * Creates a new usage log entry if none exists for today, otherwise increments the count.
     *
     * @param instanceId the instance UUID
     */
    @Transactional
    public void recordUsage(UUID instanceId) {
        LocalDate today = LocalDate.now();
        int updated = usageLogRepository.incrementRequestCount(instanceId, today);

        if (updated == 0) {
            AIUsageLog newLog = new AIUsageLog(instanceId, today);
            usageLogRepository.save(newLog);
            log.debug("Created new AI usage log for instance {} on {}", instanceId, today);
        } else {
            log.debug("Incremented AI usage for instance {} on {}", instanceId, today);
        }
    }

    /**
     * Get the current daily usage count for an instance.
     *
     * @param instanceId the instance UUID
     * @return current request count for today
     */
    public int getCurrentUsage(UUID instanceId) {
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
