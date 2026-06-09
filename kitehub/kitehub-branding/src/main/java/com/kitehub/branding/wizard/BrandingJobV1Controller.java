package com.kitehub.branding.wizard;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.service.BrandingJobService;
import com.kitehub.branding.wizard.dto.ApproveDeployRequest;
import com.kitehub.branding.wizard.dto.BrandColours;
import com.kitehub.branding.wizard.dto.BrandingJobResponse;
import com.kitehub.branding.wizard.dto.CreateWizardJobRequest;
import com.kitehub.branding.wizard.quality.BrandColoursDeriver;
import com.kitehub.branding.wizard.service.MockProvisioningService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * v1 jobs/{id} controller — Wave 34 Bucket B.
 *
 * <p>Serves {@code GET /api/v1/branding/jobs/{jobId}} per
 * {@code documents/01-business/kitehub/ai-branding/api-contract.md}.
 * Returns {@link BrandingJobResponse} with the {@code brandColors} field
 * (sub-GAP-272k).</p>
 *
 * <p>The legacy controller {@code BrandingJobController} at
 * {@code /api/platform/branding/jobs/{id}} returns the entity directly and
 * is left untouched — Bucket B owns the v1 path and the new response DTO.</p>
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/branding/jobs")
@RequiredArgsConstructor
@Tag(name = "Branding Jobs v1",
        description = "v1 job lookup with brandColors (sub-GAP-272k)")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
        extraTags = {"slo", "tier-b", "controller", "branding-job-v1"})
public class BrandingJobV1Controller {

    private final BrandingJobRepository jobRepository;
    private final BrandColoursDeriver coloursDeriver;
    private final BrandingJobService brandingJobService;
    private final MockProvisioningService mockProvisioningService;

    /** Read access — owner + staff personas (mirrors getJob). */
    private static final String READ_AUTHZ =
            "hasAnyRole('OWNER','MANAGER','TEACHER','ACCOUNTANT','PLATFORM_ADMIN','ADMIN','STAFF')";

    /** Write access — owner-tier only per GAP-562 (mirrors BrandingWizardController). */
    private static final String WRITE_AUTHZ =
            "hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')";

    /**
     * Create a wizard branding job (Phase 1 MOCK — GAP-1021). Called by the FE
     * when it enters Step 6 so {@code wizardState.jobId} becomes non-empty and the
     * preview + deploy-stream hooks enable. Persists a real {@link BrandingJob}
     * (status {@code QUEUED}) tied to a synthetic instance — NO heavy AI pipeline
     * enqueue (see {@link BrandingJobService#createWizardJob}).
     *
     * @param req wizard selections (organizationName/slug drives preview palette)
     * @return {@code 201} with {@link BrandingJobResponse} (jobId + brandColors)
     */
    @PostMapping
    @PreAuthorize(WRITE_AUTHZ)
    public ResponseEntity<?> createJob(@RequestBody(required = false) CreateWizardJobRequest req) {
        CreateWizardJobRequest body = req == null
                ? new CreateWizardJobRequest(null, null, null, null, null, null, null, null, null)
                : req;
        String orgName = firstNonBlank(body.organizationName(), body.slug(), "Trung tâm mới");
        BrandingJob job = brandingJobService.createWizardJob(orgName, body.language(), body.logoUrl());
        BrandColours colours = coloursDeriver.derive(job);
        String tenantId = MDC.get("tenantId");
        log.info("Wizard job created: {} status={}", job.getId(), job.getStatus());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BrandingJobResponse.from(job, colours, tenantId));
    }

    /**
     * Approve the generated theme + trigger MOCK deploy provisioning (GAP-1021).
     * Persists the approved theme as the instance active branding then drives the
     * lifecycle NOT_STARTED→INITIALIZING→GENERATING→DEPLOYED asynchronously — the
     * SSE {@code deploy-stream} surfaces live progress. Returns {@code 202}
     * immediately so the stream can observe progression.
     *
     * <p><b>Mock boundary:</b> real per-tenant infra is deferred to GAP-1055;
     * {@code frontendUrl} is a placeholder. See {@link MockProvisioningService}.</p>
     *
     * @param jobId branding job to deploy
     * @param req   approved resources + slug (for frontendUrl)
     * @return {@code 202} with {jobId, status, frontendUrl}, or {@code 404}
     */
    @PostMapping("/{jobId}/approve")
    @PreAuthorize(WRITE_AUTHZ)
    public ResponseEntity<?> approve(@PathVariable UUID jobId,
                                     @RequestBody(required = false) ApproveDeployRequest req) {
        Optional<BrandingJob> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "JOB_NOT_FOUND",
                    "jobId", jobId.toString()
            ));
        }
        String slug = req != null ? req.slug() : null;
        if (slug == null || slug.isBlank()) {
            slug = "tenant";
        }
        List<String> approved = req != null && req.approvedResources() != null
                ? req.approvedResources() : List.of();
        String templateId = req != null ? req.templateId() : null;

        // Async — returns 202 immediately; SSE deploy-stream observes progression.
        mockProvisioningService.provisionAsync(jobId, slug, templateId, approved);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobId", jobId.toString());
        body.put("status", "INITIALIZING");
        body.put("frontendUrl", "https://" + slug + ".kiteclass.vn");
        body.put("message", "Đang triển khai (mock provisioning)");
        return ResponseEntity.accepted().body(body);
    }

    private static String firstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                return c;
            }
        }
        return "Trung tâm mới";
    }

    @GetMapping("/{jobId}")
    @PreAuthorize(READ_AUTHZ)
    public ResponseEntity<?> getJob(@PathVariable UUID jobId) {
        Optional<BrandingJob> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "JOB_NOT_FOUND",
                    "jobId", jobId.toString()
            ));
        }
        BrandingJob job = jobOpt.get();
        BrandColours colours = coloursDeriver.derive(job);
        // GAP-390-A: resolve tenantId from request-scoped MDC (populated by gateway tenant filter
        // per logback-spring.xml MDC keys). Falls back to null when request lacks tenant context.
        String tenantId = MDC.get("tenantId");
        BrandingJobResponse response = BrandingJobResponse.from(job, colours, tenantId);
        log.debug("v1 job lookup: {} status={} brandColors.source={}",
                jobId, response.status(), colours.source());
        return ResponseEntity.ok(response);
    }
}
