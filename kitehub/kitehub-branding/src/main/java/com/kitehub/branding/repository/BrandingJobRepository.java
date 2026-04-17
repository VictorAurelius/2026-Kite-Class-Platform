package com.kitehub.branding.repository;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for BrandingJob entity.
 *
 * @since 1.0
 */
@Repository
public interface BrandingJobRepository extends JpaRepository<BrandingJob, UUID> {

    /**
     * Find jobs by instance ID.
     *
     * @param instanceId instance ID
     * @return list of jobs
     */
    List<BrandingJob> findByInstanceIdOrderByCreatedAtDesc(UUID instanceId);

    /**
     * Find job by ID and instance ID.
     *
     * @param id job ID
     * @param instanceId instance ID
     * @return optional job
     */
    Optional<BrandingJob> findByIdAndInstanceId(UUID id, UUID instanceId);

    /**
     * Find jobs by status.
     *
     * @param status job status
     * @return list of jobs
     */
    List<BrandingJob> findByStatus(JobStatus status);
}
