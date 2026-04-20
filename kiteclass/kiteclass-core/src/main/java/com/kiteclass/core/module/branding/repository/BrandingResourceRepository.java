package com.kiteclass.core.module.branding.repository;

import com.kiteclass.core.module.branding.entity.BrandingResource;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import com.kiteclass.core.module.branding.entity.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @since 3.16.0 (GAP-007)
 */
@Repository
public interface BrandingResourceRepository extends JpaRepository<BrandingResource, Long> {

    List<BrandingResource> findByTypeAndDeletedFalse(ResourceType type);

    Optional<BrandingResource> findFirstByTypeAndCategoryAndDeletedFalse(
            ResourceType type, ResourceCategory category);

    List<BrandingResource> findByCategoryAndDeletedFalse(ResourceCategory category);

    /**
     * Tenant-scoped lookup of all active branding resources for a given instance.
     *
     * <p>Backed by composite index {@code idx_branding_resources_instance_deleted}
     * (added in V45 migration). Replaces the previous full-table-scan pattern of
     * {@code findAll().stream().filter(!deleted)} which leaked ALL tenants' resources
     * into a single tenant's branding package response (GAP-129 — multi-tenancy bug
     * masquerading as a perf bug).
     *
     * @param instanceId the tenant {@code instance_id} (UUID)
     * @return active branding resources for this tenant, ordered as returned by the DB
     * @since 4.5.0 (GAP-129 fix)
     */
    List<BrandingResource> findByInstanceIdAndDeletedFalse(UUID instanceId);
}
