package com.kiteclass.core.module.student.bulkimport.repository;

import com.kiteclass.core.module.student.bulkimport.entity.BulkImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link BulkImportJob}.
 *
 * <p>Tenant-scoped lookups are done via the inherited {@code instanceId} filter
 * plus explicit parameters below to guarantee isolation even outside the
 * Hibernate filter session.
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@Repository
public interface BulkImportJobRepository extends JpaRepository<BulkImportJob, Long> {

    /**
     * Finds a non-deleted bulk-import job by ID within a tenant.
     *
     * @param id         job ID
     * @param instanceId tenant instance ID
     * @return Optional containing the job if found
     */
    Optional<BulkImportJob> findByIdAndInstanceIdAndDeletedFalse(Long id, UUID instanceId);
}
