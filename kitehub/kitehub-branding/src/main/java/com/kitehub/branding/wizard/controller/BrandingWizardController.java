package com.kitehub.branding.wizard.controller;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.wizard.dto.RegenerateQuotaResponse;
import com.kitehub.branding.wizard.dto.SlugAvailabilityResponse;
import com.kitehub.branding.wizard.service.RegenerateQuotaService;
import com.kitehub.branding.wizard.service.RegenerateQuotaService.InvalidJobStateException;
import com.kitehub.branding.wizard.service.RegenerateQuotaService.JobNotFoundException;
import com.kitehub.branding.wizard.service.RegenerateQuotaService.QuotaExceededException;
import com.kitehub.branding.wizard.service.SlugAvailabilityService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AI Branding wizard endpoints (Wave 34 — sub-GAP-272i / 272d).
 *
 * <p>Replaces Wave 32 v1 inline mocks ({@code MOCK_TAKEN_SLUGS},
 * {@code STUB_JOB_ID}, inline tier-quota logic) with real backend services.
 * Schemas match {@code documents/01-business/kitehub/ai-branding/api-contract.md}
 * (merged in Bucket 0 PR #905).
 *
 * <p>Distinct from {@link com.kitehub.branding.controller.BrandingJobController}
 * which lives at {@code /api/platform/branding/jobs} and predates Wave 34.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/branding")
@RequiredArgsConstructor
@Tag(name = "AI Branding Wizard", description = "Wave 34 wizard endpoints — slug, quota, regenerate")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
        extraTags = {"slo", "tier-c", "controller", "branding-wizard"})
public class BrandingWizardController {

    private final SlugAvailabilityService slugService;
    private final RegenerateQuotaService quotaService;

    /**
     * GAP-562/562b Wave 101 Bucket B — OWNER-only write authorization.
     * Mirrors kitehub-subscription pattern.
     */
    private static final String OWNER_AUTHZ =
            "hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')";

    private static final String OWNER_OR_STAFF_AUTHZ =
            "hasAnyRole('OWNER','MANAGER','TEACHER','ACCOUNTANT','PLATFORM_ADMIN','ADMIN','STAFF')";

    // ---------------------------------------------------------------------
    // GET /api/v1/branding/slug-availability  (sub-GAP-272i)
    // ---------------------------------------------------------------------
    @GetMapping("/slug-availability")
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<Object> checkSlug(
            @RequestParam("slug") String slug,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Tenant-Id",
                    required = false) String tenantHeader) {
        String formatErr = slugService.validateFormat(slug);
        if (formatErr != null) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", "INVALID_SLUG_FORMAT");
            body.put("message", formatErr);
            body.put("slug", slug);
            return ResponseEntity.badRequest().body(body);
        }
        // G1 walk 2026-06-12: gateway-trusted X-Tenant-Id → own-subdomain exempt (re-brand).
        java.util.UUID ownInstanceId = null;
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            try {
                ownInstanceId = java.util.UUID.fromString(tenantHeader.trim());
            } catch (IllegalArgumentException ignored) {
                // non-UUID header → treat as anonymous (no exemption)
            }
        }
        SlugAvailabilityResponse result = slugService.check(slug, ownInstanceId);
        return ResponseEntity.ok(result);
    }

    // ---------------------------------------------------------------------
    // GET /api/v1/branding/regenerate-quota  (sub-GAP-272d, read)
    // ---------------------------------------------------------------------
    @GetMapping("/regenerate-quota")
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<RegenerateQuotaResponse> getQuota(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "anonymous")
            String userId,
            @RequestHeader(value = "X-Subscription-Tier", required = false, defaultValue = "FREE")
            String tier) {
        return ResponseEntity.ok(quotaService.getQuota(userId, tier));
    }

    // ---------------------------------------------------------------------
    // POST /api/v1/branding/jobs/{jobId}/regenerate  (sub-GAP-272d, write)
    // ---------------------------------------------------------------------
    @PostMapping("/jobs/{jobId}/regenerate")
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<Object> regenerate(
            @PathVariable UUID jobId,
            @RequestHeader(value = "X-Instance-Id", required = false) UUID instanceId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "anonymous")
            String userId,
            @RequestHeader(value = "X-Subscription-Tier", required = false, defaultValue = "FREE")
            String tier,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", "MISSING_IDEMPOTENCY_KEY");
            body.put("message", "Idempotency-Key header is required for regenerate");
            return ResponseEntity.badRequest().body(body);
        }
        if (instanceId == null) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", "MISSING_INSTANCE_ID");
            body.put("message", "X-Instance-Id header is required");
            return ResponseEntity.badRequest().body(body);
        }

        try {
            BrandingJob job = quotaService.regenerate(jobId, instanceId, userId, tier, idempotencyKey);
            return ResponseEntity.ok(job);
        } catch (QuotaExceededException ex) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", "AI_REGENERATE_QUOTA_EXCEEDED");
            body.put("tier", ex.tier);
            body.put("used", ex.used);
            body.put("limit", ex.limit);
            body.put("resetAt", ex.resetAt);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        } catch (InvalidJobStateException ex) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", "INVALID_JOB_STATE");
            body.put("currentStatus", ex.currentStatus);
            body.put("message", "regenerate only allowed from DEPLOYED/FAILED");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        } catch (JobNotFoundException ex) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", "JOB_NOT_FOUND");
            body.put("jobId", ex.jobId.toString());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
    }
}
