package com.kitehub.branding.controller;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.service.BrandingJobService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * <p><strong>GAP-562b (Wave 80 Bucket C):</strong> Branding endpoints SHOULD be
 * OWNER-only per business rule (STAFF has no branding access).
 * {@code kitehub-branding} module does NOT yet have {@code spring-boot-starter-security}
 * on classpath, so {@code @PreAuthorize} is not enforceable here in Wave 80. Defense
 * comes from (a) FE {@code RoleGuard} at {@code /branding/*} route layout, (b) gateway
 * path-level auth filter. Adding spring-security dependency + {@code @EnableMethodSecurity}
 * to this module is tracked as Wave 81 follow-up under GAP-562b §"BE @PreAuthorize coverage"
 * deferred portion.</p>
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
     * Create new branding job.
     *
     * @param instanceId instance ID from header
     * @param organizationName organization name
     * @param language language code
     * @param logoUrl logo S3 URL
     * @return created job
     */
    @PostMapping
    public ResponseEntity<BrandingJob> createJob(
            @RequestHeader("X-Instance-Id") UUID instanceId,
            @RequestParam String organizationName,
            @RequestParam(defaultValue = "vi") String language,
            @RequestParam String logoUrl) {

        log.info("Creating branding job for instance: {}", instanceId);

        BrandingJob job = jobService.createJob(instanceId, organizationName, language, logoUrl);

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
    public ResponseEntity<BrandingJob> getJob(
            @RequestHeader("X-Instance-Id") UUID instanceId,
            @PathVariable UUID id) {

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
    public ResponseEntity<List<BrandingJob>> listJobs(
            @RequestHeader("X-Instance-Id") UUID instanceId) {

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
    public ResponseEntity<?> getJobAssets(
            @RequestHeader("X-Instance-Id") UUID instanceId,
            @PathVariable UUID id) {

        BrandingJob job = jobService.getJob(id, instanceId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(job.getAssetsGenerated());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelJob(
            @RequestHeader("X-Instance-Id") UUID instanceId,
            @PathVariable UUID id) {

        boolean cancelled = jobService.cancelJob(id, instanceId);

        if (cancelled) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
}
