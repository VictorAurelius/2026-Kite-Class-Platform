package com.kiteclass.core.module.marketing.repository;

import com.kiteclass.core.module.marketing.entity.LandingPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LandingPage entity.
 * Business Rule: BR-MKT-001 - Each tenant has ONE landing page.
 *
 * @since 2.10
 */
@Repository
public interface LandingPageRepository extends JpaRepository<LandingPage, Long> {

    /**
     * Find landing page by instance ID (tenant).
     * Due to Hibernate filter bypassing findById(), we use custom query.
     *
     * @param instanceId tenant instance ID
     * @return landing page if exists
     */
    @Query("SELECT lp FROM LandingPage lp WHERE lp.instanceId = :instanceId AND lp.deleted = false")
    Optional<LandingPage> findByInstanceIdAndDeletedFalse(@Param("instanceId") UUID instanceId);

    /**
     * Check if landing page exists for tenant.
     *
     * @param instanceId tenant instance ID
     * @return true if exists
     */
    boolean existsByInstanceIdAndDeletedFalse(UUID instanceId);
}
