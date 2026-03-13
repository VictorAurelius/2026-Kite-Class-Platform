package com.kitehub.branding.controller;

import com.kitehub.branding.dto.LandingPageContent;
import com.kitehub.branding.dto.LogoAnalysis;
import com.kitehub.branding.service.ContentGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for landing page content generation.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/branding/content")
@RequiredArgsConstructor
public class ContentGenerationController {

    private final ContentGenerationService contentGenerationService;

    /**
     * Generate landing page content based on logo analysis.
     *
     * @param request Content generation request
     * @return Generated landing page content
     */
    @PostMapping("/generate")
    public ResponseEntity<LandingPageContent> generateContent(
            @RequestBody ContentGenerationRequest request
    ) {
        log.info("Generating content for: {}", request.getOrgName());

        LandingPageContent content = contentGenerationService.generateLandingPageContent(
                request.getLogoAnalysis(),
                request.getOrgName(),
                request.getLanguage() != null ? request.getLanguage() : "vi"
        );

        return ResponseEntity.ok(content);
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

        // FUTURE: Implement content persistence in PR 4.9
        // For now, return 404 as content is not persisted
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
