package com.kiteclass.core.module.storage.repository;

import com.kiteclass.core.module.storage.entity.StorageQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for StorageQuota entity.
 *
 * <p>Provides data access methods including:
 * <ul>
 *   <li>Find quota by tenant instance ID</li>
 *   <li>Check quota existence for tenant</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@Repository
public interface StorageQuotaRepository extends JpaRepository<StorageQuota, Long> {

    /**
     * Finds storage quota for a tenant.
     *
     * @param instanceId the tenant instance ID
     * @return Optional containing the quota if found
     */
    Optional<StorageQuota> findByInstanceId(UUID instanceId);

    /**
     * Checks if quota exists for a tenant.
     *
     * @param instanceId the tenant instance ID
     * @return true if quota exists
     */
    boolean existsByInstanceId(UUID instanceId);
}
