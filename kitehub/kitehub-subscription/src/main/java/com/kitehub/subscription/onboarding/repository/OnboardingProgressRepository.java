package com.kitehub.subscription.onboarding.repository;

import com.kitehub.subscription.onboarding.entity.OnboardingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link OnboardingProgress} (Wave 78 GAP-538).
 *
 * <p>One row per tenant, accessed by tenant UUID. Lazy-init pattern: service
 * layer auto-creates a default row on first lookup (saving an explicit create
 * endpoint).</p>
 *
 * @since Wave 78 — GAP-538
 */
@Repository
public interface OnboardingProgressRepository extends JpaRepository<OnboardingProgress, Long> {

    /** Tenant-scoped lookup (one row per tenant). */
    Optional<OnboardingProgress> findByTenantId(UUID tenantId);
}
