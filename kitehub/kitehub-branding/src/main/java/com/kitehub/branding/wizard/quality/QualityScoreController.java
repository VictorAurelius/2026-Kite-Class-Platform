package com.kitehub.branding.wizard.quality;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.security.TenantOwnershipGuard;
import com.kitehub.branding.wizard.quality.dto.QualityScoreResponse;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Quality-score endpoint for the AI Branding wizard preview step.
 *
 * <p>Replaces Wave 32 v1 inline {@code mockChecks()} placeholder
 * (sub-GAP-272c). Tied to {@code InstanceQualityReviewer.review()} per
 * {@code ai-branding-guidelines.md} §5.</p>
 *
 * <p>v0 implementation: real WCAG / visual regression / ML classifier
 * measurement is deferred to GAP-226 / GAP-227 / GAP-228. Until those
 * land, {@link QualityScoreAggregator} returns deterministic placeholder
 * sub-scores derived from job metadata. The endpoint shape matches the
 * api-contract, so FE consumers can integrate today and the aggregator
 * gets swapped behind the same response.</p>
 *
 * @see <a href="../../../../../../../../documents/01-business/kitehub/ai-branding/api-contract.md">api-contract</a>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/branding/jobs")
@RequiredArgsConstructor
@Tag(name = "Branding Wizard — Quality Score",
        description = "Aggregated quality breakdown for wizard preview step (sub-GAP-272c)")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
        extraTags = {"slo", "tier-b", "controller", "branding-quality-score"})
public class QualityScoreController {

    private final BrandingJobRepository jobRepository;
    private final QualityScoreAggregator aggregator;

    /** Read access — owner + staff personas (mirrors {@code BrandingJobV1Controller.READ_AUTHZ}). */
    private static final String READ_AUTHZ =
            "hasAnyRole('OWNER','MANAGER','TEACHER','ACCOUNTANT','PLATFORM_ADMIN','ADMIN','STAFF')";

    /**
     * GET /api/v1/branding/jobs/{jobId}/quality-score
     *
     * @param jobId job ID
     * @return aggregated score + sub-scores + issues
     */
    @GetMapping("/{jobId}/quality-score")
    @PreAuthorize(READ_AUTHZ)
    public ResponseEntity<?> getQualityScore(@PathVariable UUID jobId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader) {
        Optional<BrandingJob> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "JOB_NOT_FOUND",
                    "jobId", jobId.toString()
            ));
        }
        BrandingJob job = jobOpt.get();
        // GAP-1526 (OWASP A01 / IDOR) — own-tenant quality-score read only after findById.
        TenantOwnershipGuard.requireInstanceOwnership(job.getInstanceId(), tenantHeader);

        // Per api-contract: 409 when job in GENERATING/early phase before REVIEW step.
        // v0 mapping: QUEUED is too early to score; everything else returns a score
        // (the aggregator emits placeholder sub-scores so FE can integrate today).
        if (job.getStatus() == JobStatus.QUEUED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "QUALITY_SCORE_NOT_READY",
                    "currentStatus", "GENERATING",
                    "message", "score available only after REVIEW step completes"
            ));
        }

        QualityScoreResponse response = aggregator.aggregate(job);
        log.debug("Quality score for job {}: {}/{} (passed={})",
                jobId, response.score(), response.threshold(), response.passed());
        return ResponseEntity.ok(response);
    }
}
