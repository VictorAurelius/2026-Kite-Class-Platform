package com.kitehub.branding.repository;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Check whether any branding job's organization name matches the given
     * slug after lowercasing both sides. Case-insensitive comparison.
     *
     * <p>Replaces the prior {@code findAll().stream().anyMatch(...)} N+1
     * anti-pattern in {@code SlugAvailabilityService} (GAP-392). Uses the
     * functional index {@code idx_branding_job_org_name_lower} added in
     * V31 to keep the lookup O(log n).</p>
     *
     * @param slug normalized lowercase slug
     * @return true if a matching organization name exists
     */
    @Query("SELECT (COUNT(j) > 0) FROM BrandingJob j "
            + "WHERE LOWER(j.organizationName) = :slug")
    boolean existsByOrganizationNameLowercased(@Param("slug") String slug);
}
