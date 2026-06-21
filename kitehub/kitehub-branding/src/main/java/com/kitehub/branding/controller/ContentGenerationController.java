package com.kitehub.branding.controller;

import com.kitehub.branding.dto.LandingPageContent;
import com.kitehub.branding.dto.LogoAnalysis;
import com.kitehub.branding.service.ContentGenerationService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for landing page content generation.
 *
 * <p>SLO Tier C (synchronous content generation; AI calls bounded by service
 * timeout). See {@code documents/05-guides/api-performance-slo.md}.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/branding/content")
@RequiredArgsConstructor
@Tag(name = "Content Generation", description = "AI-generated landing page content")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-c", "controller", "content-generation"})
public class ContentGenerationController {

    private final ContentGenerationService contentGenerationService;

    /**
     * GAP-1526 (OWASP A01) — OWNER-tier write authorization (mirrors
     * {@code BrandingJobController.OWNER_AUTHZ}). AI content-generation is a paid/owner action;
     * STAFF/MANAGER/TEACHER → 403. The request carries no tenant-scoped resource id, so this is a
     * role-gate only (no per-instance ownership check applies).
     */
    private static final String OWNER_AUTHZ =
            "hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')";

    /**
     * Generate landing page content based on logo analysis.
     *
     * @param request Content generation request
     * @return Generated landing page content
     */
    @PostMapping("/generate")
    @PreAuthorize(OWNER_AUTHZ)
    public Mono<ResponseEntity<LandingPageContent>> generateContent(
            @RequestBody ContentGenerationRequest request
    ) {
        log.info("Generating content for: {}", request.getOrgName());

        // Fully reactive — Spring MVC supports Mono return type (async dispatch),
        // so we never block the servlet thread waiting for the AI provider.
        return contentGenerationService.generateLandingPageContent(
                        request.getLogoAnalysis(),
                        request.getOrgName(),
                        request.getLanguage() != null ? request.getLanguage() : "vi"
                )
                .map(ResponseEntity::ok);
    }

    /**
     * Get cached landing page content for an instance.
     *
     * Note: Actual caching/persistence will be implemented in PR 4.9 (Job Queue).
     * For now, this endpoint returns a placeholder response.
     *
     * @param instanceId Instance UUID
     * @return Cached content or 404
     */
    @GetMapping("/{instanceId}")
    public ResponseEntity<LandingPageContent> getContent(@PathVariable String instanceId) {
        log.info("Getting content for instance: {}", instanceId);

        // Content persistence is deferred until Job Queue infrastructure (PR 4.9).
        // Currently returns 404 as content is generated on-demand, not persisted.
        return ResponseEntity.notFound().build();
    }

    /**
     * Request DTO for content generation.
     */
    public static class ContentGenerationRequest {
        private LogoAnalysis logoAnalysis;
        private String orgName;
        private String language; // "vi" or "en"

        public LogoAnalysis getLogoAnalysis() {
            return logoAnalysis;
        }

        public void setLogoAnalysis(LogoAnalysis logoAnalysis) {
            this.logoAnalysis = logoAnalysis;
        }

        public String getOrgName() {
            return orgName;
        }

        public void setOrgName(String orgName) {
            this.orgName = orgName;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }
}
