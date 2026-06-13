package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.PricingTier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Instance tier synchronization helper — the single SUB-21 sync point
 * (GAP-1256, Wave kitehub-biz-100 Bucket 0 Foundation).
 *
 * <p>{@code subscriptions.tier} is the source-of-truth; {@code instances.tier} is the
 * denormalized current-effective-tier. The mirror is load-bearing: connection-pool size
 * ({@code MultiTenantDataSourceConfig}), custom-domain eligibility ({@code DomainService}),
 * and data-retention window ({@code DataRetentionService}) all read {@code instances.tier}.
 * Every path that changes the effective tier MUST route through {@link #syncInstanceTier}
 * so the denormalized column never drifts (root cause of GAP-1090 / GAP-1095 / GAP-1096 /
 * GAP-1256).</p>
 *
 * <p>Call-sites each owning bucket must wire (per wave plan §6):</p>
 * <ul>
 *   <li>BE-1: {@code SubscriptionService.applyPendingUpgrade} + {@code processRenewal}</li>
 *   <li>BE-2: {@code TrialToPaidService.rollback} (GAP-1256 rollback desync)</li>
 *   <li>BE-3: suspend / cancel / expiry paths (GAP-1256 lapse desync)</li>
 * </ul>
 */
@Slf4j
@Service
public class InstanceTierSyncService {

    /**
     * Set the denormalized {@code instances.tier} to the given effective tier. The caller
     * is responsible for persisting the {@link Instance} — this method only mutates the
     * in-memory entity within the caller's transaction (no-op when already in sync).
     *
     * @param instance      the instance whose tier mirror to update (non-null)
     * @param effectiveTier the new effective tier from the active subscription (non-null)
     * @throws IllegalArgumentException if either argument is null
     */
    public void syncInstanceTier(Instance instance, PricingTier effectiveTier) {
        if (instance == null || effectiveTier == null) {
            throw new IllegalArgumentException("instance and effectiveTier must be non-null");
        }
        PricingTier previous = instance.getTier();
        if (previous != effectiveTier) {
            instance.setTier(effectiveTier);
            log.debug("Synced instance {} tier {} -> {}", instance.getId(), previous, effectiveTier);
        }
    }
}
