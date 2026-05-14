package com.kitehub.subscription.auth.twofactor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link RecoveryCode} rows (GAP-516).
 *
 * @since 1.0.0 (Wave 72b)
 */
@Repository
public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, Long> {

    /** Active (unused) codes for a user, oldest first. */
    List<RecoveryCode> findByUserIdAndUsedAtIsNullOrderByIdAsc(UUID userId);

    /** All codes for a user (active + consumed). Used by audit + count diagnostics. */
    List<RecoveryCode> findByUserIdOrderByIdAsc(UUID userId);

    long countByUserIdAndUsedAtIsNull(UUID userId);

    /**
     * Atomically mark every still-active code for a user as consumed.
     * Returns the row count to feed into the audit payload.
     */
    @Modifying
    @Query("update RecoveryCode rc set rc.usedAt = :now "
        + "where rc.userId = :userId and rc.usedAt is null")
    int markAllUsed(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}
