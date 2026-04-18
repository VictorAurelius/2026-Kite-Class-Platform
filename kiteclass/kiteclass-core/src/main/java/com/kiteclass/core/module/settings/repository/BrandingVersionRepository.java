package com.kiteclass.core.module.settings.repository;

import com.kiteclass.core.module.settings.entity.BrandingVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link BrandingVersion} entity.
 *
 * @since Wave 4 (GAP-033p)
 */
@Repository
public interface BrandingVersionRepository extends JpaRepository<BrandingVersion, Long> {

    /**
     * Paginated list of non-deleted versions, newest first.
     */
    @Query("""
            SELECT v FROM BrandingVersion v
            WHERE v.instanceId = :instanceId AND v.deleted = false
            ORDER BY v.versionNumber DESC
            """)
    Page<BrandingVersion> findByInstanceIdOrderByVersionNumberDesc(
            @Param("instanceId") UUID instanceId, Pageable pageable);

    /**
     * Find the current active version for a tenant. Exactly one row should
     * satisfy this predicate (enforced by a partial unique index).
     */
    @Query("""
            SELECT v FROM BrandingVersion v
            WHERE v.instanceId = :instanceId
              AND v.active = true
              AND v.deleted = false
            """)
    Optional<BrandingVersion> findActiveByInstanceId(@Param("instanceId") UUID instanceId);

    /**
     * Look up a specific version number for a tenant.
     */
    @Query("""
            SELECT v FROM BrandingVersion v
            WHERE v.instanceId = :instanceId
              AND v.versionNumber = :versionNumber
              AND v.deleted = false
            """)
    Optional<BrandingVersion> findByInstanceIdAndVersionNumber(
            @Param("instanceId") UUID instanceId,
            @Param("versionNumber") Integer versionNumber);

    /**
     * Highest version_number seen for a tenant (used when allocating new IDs).
     */
    @Query("""
            SELECT COALESCE(MAX(v.versionNumber), 0) FROM BrandingVersion v
            WHERE v.instanceId = :instanceId
            """)
    Integer maxVersionNumber(@Param("instanceId") UUID instanceId);
}
