package com.kitehub.branding.wizard;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.wizard.dto.BrandColours;
import com.kitehub.branding.wizard.dto.BrandingJobResponse;
import com.kitehub.branding.wizard.quality.BrandColoursDeriver;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/{jobId}")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','TEACHER','ACCOUNTANT','PLATFORM_ADMIN','ADMIN','STAFF')")
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
