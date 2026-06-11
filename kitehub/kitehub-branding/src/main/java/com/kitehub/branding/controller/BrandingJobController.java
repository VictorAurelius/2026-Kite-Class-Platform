package com.kitehub.branding.controller;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.security.TenantOwnershipGuard;
import com.kitehub.branding.service.BrandingJobService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for branding job management.
 * <p>
 * Endpoints:
 * - POST /api/v1/branding/jobs - Create new job
 * - GET /api/v1/branding/jobs/{id} - Get job status
 * - GET /api/v1/branding/jobs - List all jobs for instance
 * - DELETE /api/v1/branding/jobs/{id} - Cancel job
 *
 * <p>SLO Tier B (job CRUD; AI execution is async per Tier E queue SLOs).
 * See {@code documents/05-guides/api-performance-slo.md}.
 *
 * <p><strong>Authorization:</strong> {@code @PreAuthorize} is enforced — the module now has
 * spring-security + {@code @EnableMethodSecurity} (GAP-562 Wave 101 Bucket B). OWNER-only
 * writes; OWNER + STAFF subroles read.</p>
 *
 * <p><strong>GAP-1019 (Wave security-2 Bucket B):</strong> the role gate alone did not bind the
 * client-supplied {@code X-Instance-Id} to the caller's tenant — an OWNER could scope to another
 * tenant's instance. Each endpoint now verifies {@code X-Instance-Id} matches the gateway-trusted
 * {@code X-Tenant-Id} via {@link TenantOwnershipGuard} (platform admins bypass).</p>
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/branding/jobs")
@RequiredArgsConstructor
@Tag(name = "Branding Jobs", description = "Branding job lifecycle management")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-b", "controller", "branding-job"})
public class BrandingJobController {

    private final BrandingJobService jobService;

    /**
     * GAP-562/562b Wave 101 Bucket B — OWNER-only write authorization.
     * STAFF/MANAGER/TEACHER → 403 (no branding access per business rule).
     */
    private static final String OWNER_AUTHZ =
            "hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')";

    /**
     * Multi-role READ — OWNER + STAFF subroles can view branding job status
     * (read-only inspection of own tenant's jobs).
     */
    private static final String OWNER_OR_STAFF_AUTHZ =
            "hasAnyRole('OWNER','MANAGER','TEACHER','ACCOUNTANT','PLATFORM_ADMIN','ADMIN','STAFF')";

    /**
     * Create new branding job.
     *
     * @param instanceId instance ID from header
     * @param organizationName organization name
     * @param language language code
     * @param logoUrl logo S3 URL
     * @return created job
     */
    @PostMapping
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<BrandingJob> createJob(
            @RequestHeader("X-Instance-Id") UUID instanceId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestParam String organizationName,
            @RequestParam(defaultValue = "vi") String language,
            @RequestParam String logoUrl,
            @RequestHeader(value = "X-Subscription-Tier", required = false, defaultValue = "FREE") String tier) {

        // GAP-1019: bind client X-Instance-Id to the gateway-trusted tenant.
        TenantOwnershipGuard.requireInstanceOwnership(instanceId, tenantHeader);
        // GAP-1135/1137: propagate subscription tier (ADR-039 X-Subscription-Tier) so the
        // processor routes FULL_AI (PREMIUM/ENTERPRISE) vs TEMPLATE generation.
        log.info("Creating branding job for instance: {} (tier={})", instanceId, tier);

        BrandingJob job = jobService.createJob(instanceId, organizationName, language, logoUrl, tier);

        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }

    /**
     * Get job by ID.
     *
     * @param instanceId instance ID from header
     * @param id job ID
     * @return job or 404
     */
    @GetMapping("/{id}")
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<BrandingJob> getJob(
            @RequestHeader("X-Instance-Id") UUID instanceId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id) {

        TenantOwnershipGuard.requireInstanceOwnership(instanceId, tenantHeader);
        BrandingJob job = jobService.getJob(id, instanceId);

        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(job);
    }

    /**
     * List all jobs for instance.
     *
     * @param instanceId instance ID from header
     * @return list of jobs
     */
    @GetMapping
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<List<BrandingJob>> listJobs(
            @RequestHeader("X-Instance-Id") UUID instanceId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader) {

        TenantOwnershipGuard.requireInstanceOwnership(instanceId, tenantHeader);
        List<BrandingJob> jobs = jobService.getJobsByInstance(instanceId);

        return ResponseEntity.ok(jobs);
    }

    /**
     * Cancel job.
     *
     * @param instanceId instance ID from header
     * @param id job ID
     * @return 204 if cancelled, 404 if not found, 400 if already completed
     */
    /**
     * Get assets for a completed branding job.
     *
     * @param instanceId instance ID from header
     * @param id job ID
     * @return list of generated assets
     */
    @GetMapping("/{id}/assets")
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<?> getJobAssets(
            @RequestHeader("X-Instance-Id") UUID instanceId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id) {

        TenantOwnershipGuard.requireInstanceOwnership(instanceId, tenantHeader);
        BrandingJob job = jobService.getJob(id, instanceId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(job.getAssetsGenerated());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<Void> cancelJob(
            @RequestHeader("X-Instance-Id") UUID instanceId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id) {

        TenantOwnershipGuard.requireInstanceOwnership(instanceId, tenantHeader);
        boolean cancelled = jobService.cancelJob(id, instanceId);

        if (cancelled) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
}
