package com.kiteclass.core.module.settings.repository;

import com.kiteclass.core.module.settings.entity.Branding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Branding entity.
 *
 * @since 2.9
 */
@Repository
public interface BrandingRepository extends JpaRepository<Branding, Long> {

    /**
     * Find branding by instance ID (non-deleted).
     *
     * @param instanceId tenant instance ID
     * @return Optional of Branding
     */
    @Query("SELECT b FROM Branding b WHERE b.instanceId = :instanceId AND b.deleted = false")
    Optional<Branding> findByInstanceIdAndDeletedFalse(@Param("instanceId") UUID instanceId);

    /**
     * Check if branding exists for instance.
     *
     * @param instanceId tenant instance ID
     * @return true if exists
     */
    @Query("SELECT COUNT(b) > 0 FROM Branding b WHERE b.instanceId = :instanceId AND b.deleted = false")
    boolean existsByInstanceIdAndDeletedFalse(@Param("instanceId") UUID instanceId);
}
