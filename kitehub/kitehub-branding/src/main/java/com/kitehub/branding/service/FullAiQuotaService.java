package com.kitehub.branding.service;

import com.kitehub.branding.config.AIRateLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * FULL_AI (paid GPT image-gen) monthly cost quota gate (GAP-1137).
 *
 * <p>FULL_AI is the cost-bearing generation path (the Gemini TEMPLATE path is
 * $0), so it carries a tighter, AI-type-specific quota than the per-day regen
 * limit. Per the SUB-22 matrix + {@code ai-branding-guidelines.md} §2.4 (user
 * decision 2026-06-10):</p>
 *
 * <ul>
 *   <li><b>ENTERPRISE</b> — unlimited FULL_AI (quota {@code -1}).</li>
 *   <li><b>PREMIUM</b> — limited monthly FULL_AI quota (default 5), then fall
 *       back to the free TEMPLATE path.</li>
 *   <li><b>FREE / BASIC / unknown</b> — not FULL_AI-eligible at all (quota 0);
 *       {@code GenerationMode.forTier} already routes them to TEMPLATE, so this
 *       gate is a defence-in-depth backstop.</li>
 * </ul>
 *
 * <p>Counting reuses the Redis-backed {@link DistributedRateLimiter} month key
 * ({@code ai:fullai:{instanceId}:{yyyy-MM}}). When Redis is unavailable the gate
 * fails OPEN for eligible tiers (allow + log) — acceptable because the FULL_AI
 * path has its own no-API-key TEMPLATE fallback, so a missing counter cannot by
 * itself cause real spend.</p>
 *
 * @since GAP-1137
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FullAiQuotaService {

    private final AIRateLimitConfig rateLimitConfig;
    private final DistributedRateLimiter distributedRateLimiter;

    /**
     * Whether {@code tier} may use a FULL_AI generation for {@code instanceId}
     * right now (eligibility + remaining monthly quota).
     *
     * @param instanceId tenant id
     * @param tier       subscription tier
     * @return true if FULL_AI is permitted; false → caller uses TEMPLATE
     */
    public boolean canUseFullAi(UUID instanceId, String tier) {
        int quota = rateLimitConfig.getFullAiMonthlyQuotaForTier(tier);
        if (quota < 0) {
            return true; // unlimited (ENTERPRISE)
        }
        if (quota == 0) {
            return false; // not FULL_AI-eligible (FREE / BASIC / unknown)
        }
        long used = distributedRateLimiter.getMonthlyFullAiUsage(instanceId);
        if (used < 0) {
            // Redis unavailable — fail open (FULL_AI no-key fallback caps real cost).
            log.warn("FULL_AI quota counter unavailable for instance {} (tier={}) — allowing",
                    instanceId, tier);
            return true;
        }
        return used < quota;
    }

    /**
     * Record one FULL_AI usage against the monthly quota. No-op for unlimited
     * (ENTERPRISE) and ineligible tiers — only finite-quota tiers (PREMIUM) are
     * counted.
     *
     * @param instanceId tenant id
     * @param tier       subscription tier
     */
    public void recordFullAiUsage(UUID instanceId, String tier) {
        int quota = rateLimitConfig.getFullAiMonthlyQuotaForTier(tier);
        if (quota <= 0) {
            return; // unlimited or ineligible — nothing to count
        }
        long count = distributedRateLimiter.incrementMonthlyFullAiUsage(instanceId);
        log.debug("FULL_AI usage recorded for instance {} (tier={}, month-count={}, quota={})",
                instanceId, tier, count, quota);
    }
}
