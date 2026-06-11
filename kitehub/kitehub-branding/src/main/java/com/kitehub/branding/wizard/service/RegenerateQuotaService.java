package com.kitehub.branding.wizard.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.wizard.dto.RegenerateQuotaResponse;
import com.kitehub.branding.wizard.entity.BrandingRegenerateUsage;
import com.kitehub.branding.wizard.repository.BrandingRegenerateUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Regenerate-quota service for the AI Branding wizard (Wave 34 sub-GAP-272d).
 *
 * <p>Tier caps mirror {@code .claude/rules/ai-branding-guidelines.md} §4.3:
 * FREE=3 / PRO=10 / PREMIUM=30 / ENTERPRISE=-1 (unlimited). Window = UTC day.
 *
 * <p>This service is intentionally separate from the legacy
 * {@code AIRateLimitService} — that one rate-limits AI provider calls per-day;
 * this one tracks user-facing regenerate budgets the wizard surfaces.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegenerateQuotaService {

    @Value("${kitehub.regenerate.free-limit:3}")
    int freeLimit;

    @Value("${kitehub.regenerate.pro-limit:10}")
    int proLimit;

    @Value("${kitehub.regenerate.premium-limit:30}")
    int premiumLimit;

    @Value("${kitehub.regenerate.enterprise-limit:-1}")
    int enterpriseLimit;

    private final BrandingRegenerateUsageRepository usageRepository;
    private final BrandingJobRepository brandingJobRepository;
    private final Clock clock;

    /**
     * Local idempotency-hash cache (Wave 36 GAP-393-D).
     *
     * <p>Saves the 2-query latency on idempotent regenerate replays: first the
     * {@code findByUserIdAndIdempotencyKey} usage lookup, then {@code findById}
     * for the job. With this cache, replays within 10 minutes return the cached
     * job UUID without hitting the DB. Local-only on purpose — idempotency is
     * an in-session concern and the DB row remains the source of truth on cold
     * start.
     */
    private final Cache<String, UUID> idempotencyCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    /** Resolve the tier cap. Unknown tier → FREE (fail-safe). */
    int limitFor(String tier) {
        if (tier == null) return freeLimit;
        return switch (tier.toUpperCase(Locale.ROOT)) {
            // BASIC = canonical (PricingTier.java); "PRO" giữ làm alias backward-compat
            // cho JWT cũ phát trước rename (GAP-1228)
            case "BASIC", "PRO" -> proLimit;
            case "PREMIUM" -> premiumLimit;
            case "ENTERPRISE" -> enterpriseLimit;
            default -> freeLimit; // FREE / TRIAL / unknown
        };
    }

    /**
     * Read the user's current quota state. Lazy — does not create a row if the
     * user has never regenerated today.
     *
     * <p>Wave 36 GAP-393-A: cached in {@link com.kitehub.branding.config.CacheConfig#REGENERATE_QUOTA_CACHE}
     * keyed by {@code userId + '_' + tier}. Wizard fetches this multiple times per
     * session (template select, preview, deploy). Eviction occurs in
     * {@link #regenerate} when usage is recorded.
     */
    @Cacheable(value = "regenerateQuota", key = "#userId + '_' + #tier", unless = "#result == null")
    public RegenerateQuotaResponse getQuota(String userId, String tier) {
        int limit = limitFor(tier);
        LocalDateTime windowStart = todayStart();
        int used = usageRepository
                .findByUserIdAndWindowStart(userId, windowStart)
                .map(BrandingRegenerateUsage::getUsedCount)
                .orElse(0);
        Instant resetAt = limit == -1 ? null : windowStart.plusDays(1).toInstant(ZoneOffset.UTC);
        return new RegenerateQuotaResponse(canonicalTier(tier), used, limit, resetAt);
    }

    /**
     * Consume one regenerate slot atomically. Returns the updated job on
     * success.
     *
     * @throws QuotaExceededException when limit is hit
     * @throws IllegalStateException  when the job is not in a regen-eligible state
     */
    @Transactional
    @CacheEvict(value = "regenerateQuota", key = "#userId + '_' + #tier")
    public BrandingJob regenerate(
            UUID jobId, UUID instanceId, String userId, String tier, String idempotencyKey) {

        // Wave 36 GAP-393-D — idempotent replay: try local Caffeine cache first
        // (saves the 2 DB queries: usage lookup + job fetch). DB remains the
        // source of truth; cache miss falls through to the original 2-query path.
        if (idempotencyKey != null) {
            String cacheKey = userId + ":" + idempotencyKey;
            UUID cachedJobId = idempotencyCache.getIfPresent(cacheKey);
            if (cachedJobId != null) {
                BrandingJob cachedHit = brandingJobRepository.findById(cachedJobId).orElse(null);
                if (cachedHit != null) {
                    log.debug("Idempotent regenerate cache-hit for user={} key={}",
                            userId, idempotencyKey);
                    return cachedHit;
                }
                // Cached UUID points to a missing job — invalidate + fall through.
                idempotencyCache.invalidate(cacheKey);
            }

            Optional<BrandingRegenerateUsage> replay = usageRepository
                    .findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (replay.isPresent() && replay.get().getJobId() != null) {
                BrandingJob existing = brandingJobRepository
                        .findById(replay.get().getJobId())
                        .orElse(null);
                if (existing != null) {
                    idempotencyCache.put(cacheKey, existing.getId());
                    log.debug("Idempotent regenerate replay for user={} key={}",
                            userId, idempotencyKey);
                    return existing;
                }
            }
        }

        BrandingJob job = brandingJobRepository
                .findByIdAndInstanceId(jobId, instanceId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (!isRegenEligible(job.getStatus())) {
            throw new InvalidJobStateException(job.getStatus().name());
        }

        int limit = limitFor(tier);
        LocalDateTime windowStart = todayStart();
        BrandingRegenerateUsage usage = usageRepository
                .findByUserIdAndWindowStart(userId, windowStart)
                .orElseGet(() -> {
                    BrandingRegenerateUsage fresh = new BrandingRegenerateUsage();
                    fresh.setUserId(userId);
                    fresh.setTier(canonicalTier(tier));
                    fresh.setWindowStart(windowStart);
                    fresh.setWindowEnd(windowStart.plusDays(1));
                    fresh.setUsedCount(0);
                    return fresh;
                });

        if (limit != -1 && usage.getUsedCount() >= limit) {
            throw new QuotaExceededException(
                    canonicalTier(tier), usage.getUsedCount(), limit,
                    windowStart.plusDays(1).toInstant(ZoneOffset.UTC));
        }

        usage.setUsedCount(usage.getUsedCount() + 1);
        usage.setInstanceId(instanceId);
        usage.setJobId(job.getId());
        usage.setIdempotencyKey(idempotencyKey);
        usage.setLastRegenerateAt(LocalDateTime.now(clock));
        usageRepository.save(usage);

        job.setStatus(JobStatus.PROCESSING);
        job.setProgress(0);
        BrandingJob saved = brandingJobRepository.save(job);

        // Wave 36 GAP-393-D — seed idempotency cache so a same-key replay
        // within the 10-minute window is served without DB hits.
        if (idempotencyKey != null) {
            idempotencyCache.put(userId + ":" + idempotencyKey, saved.getId());
        }
        return saved;
    }

    /**
     * Test seam — exposes idempotency cache size for assertions.
     * Visible for tests only.
     */
    long idempotencyCacheSize() {
        return idempotencyCache.estimatedSize();
    }

    private boolean isRegenEligible(JobStatus status) {
        // Per api-contract.md: regenerate only allowed from DEPLOYED|FAILED.
        // Existing JobStatus enum doesn't have DEPLOYED — closest analogues
        // are COMPLETED (success terminal) and FAILED (recoverable).
        return status == JobStatus.COMPLETED || status == JobStatus.FAILED;
    }

    private LocalDateTime todayStart() {
        return LocalDateTime.now(clock).toLocalDate().atStartOfDay();
    }

    private static String canonicalTier(String tier) {
        if (tier == null) return "FREE";
        String upper = tier.toUpperCase(Locale.ROOT);
        return switch (upper) {
            // Canonical hoá: alias cũ "PRO" → BASIC (GAP-1228); snapshot DB lưu canonical
            case "PRO", "BASIC" -> "BASIC";
            case "PREMIUM", "ENTERPRISE" -> upper;
            default -> "FREE";
        };
    }

    /** Thrown when the user has consumed their daily regenerate quota. */
    public static class QuotaExceededException extends RuntimeException {
        public final String tier;
        public final int used;
        public final int limit;
        public final Instant resetAt;

        public QuotaExceededException(String tier, int used, int limit, Instant resetAt) {
            super("regenerate quota exceeded");
            this.tier = tier;
            this.used = used;
            this.limit = limit;
            this.resetAt = resetAt;
        }
    }

    /** Thrown when target job is not in a regen-eligible state. */
    public static class InvalidJobStateException extends RuntimeException {
        public final String currentStatus;

        public InvalidJobStateException(String currentStatus) {
            super("regenerate only allowed from DEPLOYED/FAILED, current=" + currentStatus);
            this.currentStatus = currentStatus;
        }
    }

    /** Thrown when the target job ID is not found for the instance. */
    public static class JobNotFoundException extends RuntimeException {
        public final UUID jobId;

        public JobNotFoundException(UUID jobId) {
            super("job not found: " + jobId);
            this.jobId = jobId;
        }
    }
}
