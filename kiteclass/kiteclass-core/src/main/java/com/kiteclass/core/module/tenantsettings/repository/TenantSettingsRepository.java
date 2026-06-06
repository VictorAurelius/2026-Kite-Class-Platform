package com.kiteclass.core.module.tenantsettings.repository;

import com.kiteclass.core.module.tenantsettings.entity.TenantSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link TenantSettings} — 1:1 per tenant (instance).
 *
 * @since Wave provisioning-1 (GAP-947)
 */
@Repository
public interface TenantSettingsRepository extends JpaRepository<TenantSettings, Long> {

    /**
     * Find the settings row for a tenant (non-deleted).
     *
     * @param instanceId tenant instance ID
     * @return Optional of TenantSettings
     */
    @Query("SELECT ts FROM TenantSettings ts WHERE ts.instanceId = :instanceId AND ts.deleted = false")
    Optional<TenantSettings> findByInstanceIdAndDeletedFalse(@Param("instanceId") UUID instanceId);

    /**
     * Check whether a tenant already has a settings row.
     *
     * @param instanceId tenant instance ID
     * @return true if a non-deleted settings row exists
     */
    @Query("SELECT COUNT(ts) > 0 FROM TenantSettings ts WHERE ts.instanceId = :instanceId AND ts.deleted = false")
    boolean existsByInstanceIdAndDeletedFalse(@Param("instanceId") UUID instanceId);
}
