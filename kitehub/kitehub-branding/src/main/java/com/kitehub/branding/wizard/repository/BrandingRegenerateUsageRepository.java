package com.kitehub.branding.wizard.repository;

import com.kitehub.branding.wizard.entity.BrandingRegenerateUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for {@link BrandingRegenerateUsage} (Wave 34 sub-GAP-272d).
 */
@Repository
public interface BrandingRegenerateUsageRepository
        extends JpaRepository<BrandingRegenerateUsage, Long> {

    /**
     * Look up the user's counter row for the given window. One row per day per user.
     */
    Optional<BrandingRegenerateUsage> findByUserIdAndWindowStart(
            String userId, LocalDateTime windowStart);

    /**
     * Idempotency lookup — find the existing usage row for a user + idempotency key
     * (used to detect POST /regenerate replays within the window).
     */
    Optional<BrandingRegenerateUsage> findByUserIdAndIdempotencyKey(
            String userId, String idempotencyKey);
}
