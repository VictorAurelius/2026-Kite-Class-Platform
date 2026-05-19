package com.kitehub.branding.controller;

import com.kitehub.branding.dto.LogoAnalysis;
import com.kitehub.branding.dto.ThemeConfig;
import com.kitehub.branding.service.AIBrandingService;
import com.kitehub.branding.service.AIInputCapService;
import com.kitehub.branding.service.AIRateLimitService;
import com.kitehub.branding.service.ThemeGenerationService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for AI branding operations.
 * Includes per-tier rate limiting based on instance subscription.
 *
 * <p>SLO Tier E (HTTP entry point returns jobId immediately; actual generation
 * is queue-bound, governed by branding queue SLAs in {@code application.yml}).
 * The Tier E HTTP step itself is bounded by Tier C budget (write).
 * See {@code documents/05-guides/api-performance-slo.md}.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/branding/ai")
@RequiredArgsConstructor
@Tag(name = "AI Branding", description = "AI-powered logo analysis, image generation, and theme creation")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-c", "controller", "ai-branding"})
public class AIBrandingController {

    private final AIBrandingService aiBrandingService;
    private final ThemeGenerationService themeGenerationService;
    private final AIRateLimitService aiRateLimitService;
    private final AIInputCapService aiInputCapService;

    /**
     * GAP-562/562b Wave 101 Bucket B — OWNER-only write authorization.
     * Mirrors kitehub-subscription pattern. STAFF/MANAGER/TEACHER → 403.
     */
    private static final String OWNER_AUTHZ =
            "hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')";

    /**
     * Analyze logo and extract brand identity.
     *
     * @param request Logo analysis request
     * @param instanceId Instance ID from gateway header
     * @param tier Subscription tier from gateway header
     * @return Logo analysis result
     */
    @PostMapping("/analyze-logo")
    @PreAuthorize(OWNER_AUTHZ)
    public Mono<ResponseEntity<Object>> analyzeLogo(
            @Valid @RequestBody AnalyzeLogoRequest request,
            @RequestHeader(value = "X-Instance-Id", required = false) String instanceId,
            @RequestHeader(value = "X-Subscription-Tier", required = false, defaultValue = "FREE") String tier
    ) {
        ResponseEntity<Object> rateLimitResponse = checkRateLimit(instanceId, tier);
        if (rateLimitResponse != null) {
            return Mono.just(rateLimitResponse);
        }

        ResponseEntity<Object> inputCapResponse = aiInputCapService.checkInputSize(
                tier, request.getLogoUrl(), request.getOrganizationName());
        if (inputCapResponse != null) {
            return Mono.just(inputCapResponse);
        }

        recordUsageIfPresent(instanceId);

        return aiBrandingService.analyzeLogo(request.getLogoUrl(), request.getOrganizationName())
            .map(result -> ResponseEntity.ok((Object) result));
    }

    /**
     * Generate hero banner image.
     *
     * @param request Image generation request
     * @param instanceId Instance ID from gateway header
     * @param tier Subscription tier from gateway header
     * @return Generated image URL
     */
    @PostMapping("/generate-image")
    @PreAuthorize(OWNER_AUTHZ)
    public Mono<ResponseEntity<Object>> generateImage(
            @Valid @RequestBody GenerateImageRequest request,
            @RequestHeader(value = "X-Instance-Id", required = false) String instanceId,
            @RequestHeader(value = "X-Subscription-Tier", required = false, defaultValue = "FREE") String tier
    ) {
        ResponseEntity<Object> rateLimitResponse = checkRateLimit(instanceId, tier);
        if (rateLimitResponse != null) {
            return Mono.just(rateLimitResponse);
        }

        ResponseEntity<Object> inputCapResponse = aiInputCapService.checkInputSize(
                tier, request.getOrganizationName(), request.getTheme(), request.getColors());
        if (inputCapResponse != null) {
            return Mono.just(inputCapResponse);
        }

        recordUsageIfPresent(instanceId);

        return aiBrandingService.generateHeroImage(
                request.getOrganizationName(),
                request.getTheme(),
                request.getColors()
            )
            .map(imageUrl -> ResponseEntity.ok((Object) new ImageGenerationResponse(imageUrl)));
    }

    /**
     * Generate marketing copy.
     *
     * @param request Text generation request
     * @param instanceId Instance ID from gateway header
     * @param tier Subscription tier from gateway header
     * @return Generated marketing copy
     */
    @PostMapping("/generate-text")
    @PreAuthorize(OWNER_AUTHZ)
    public Mono<ResponseEntity<Object>> generateText(
            @Valid @RequestBody GenerateTextRequest request,
            @RequestHeader(value = "X-Instance-Id", required = false) String instanceId,
            @RequestHeader(value = "X-Subscription-Tier", required = false, defaultValue = "FREE") String tier
    ) {
        ResponseEntity<Object> rateLimitResponse = checkRateLimit(instanceId, tier);
        if (rateLimitResponse != null) {
            return Mono.just(rateLimitResponse);
        }

        ResponseEntity<Object> inputCapResponse = aiInputCapService.checkInputSize(
                tier, request.getOrganizationName(), request.getTheme(), request.getTargetAudience());
        if (inputCapResponse != null) {
            return Mono.just(inputCapResponse);
        }

        recordUsageIfPresent(instanceId);

        return aiBrandingService.generateMarketingCopy(
                request.getOrganizationName(),
                request.getTheme(),
                request.getTargetAudience()
            )
            .map(text -> ResponseEntity.ok((Object) new TextGenerationResponse(text)));
    }

    /**
     * Generate complete theme configuration from logo analysis.
     * This endpoint creates a full theme JSON with colors, typography, spacing, and layout.
     *
     * @param analysis Logo analysis from AI
     * @param instanceId Instance ID from gateway header
     * @param tier Subscription tier from gateway header
     * @return Complete theme configuration ready for KiteClass frontend
     */
    @PostMapping("/generate-theme")
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<Object> generateTheme(
            @Valid @RequestBody LogoAnalysis analysis,
            @RequestHeader(value = "X-Instance-Id", required = false) String instanceId,
            @RequestHeader(value = "X-Subscription-Tier", required = false, defaultValue = "FREE") String tier
    ) {
        ResponseEntity<Object> rateLimitResponse = checkRateLimit(instanceId, tier);
        if (rateLimitResponse != null) {
            return rateLimitResponse;
        }

        ResponseEntity<Object> inputCapResponse = aiInputCapService.checkInputSize(
                tier,
                analysis.getPrimaryColor(), analysis.getSecondaryColor(), analysis.getAccentColor(),
                analysis.getTheme(), analysis.getTypography(), analysis.getTargetAudience());
        if (inputCapResponse != null) {
            return inputCapResponse;
        }

        recordUsageIfPresent(instanceId);

        ThemeConfig themeConfig = themeGenerationService.generateThemeConfig(analysis);
        return ResponseEntity.ok(themeConfig);
    }

    /**
     * Check rate limit for the given instance and tier.
     *
     * @return error response if rate limited, null if allowed
     */
    private ResponseEntity<Object> checkRateLimit(String instanceId, String tier) {
        if (instanceId == null || instanceId.isBlank()) {
            return null; // No instance header = internal call, no rate limit
        }

        try {
            UUID parsedInstanceId = UUID.fromString(instanceId);
            if (aiRateLimitService.isRateLimited(parsedInstanceId, tier)) {
                int limit = aiRateLimitService.getDailyLimit(tier);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                        "error", "AI_RATE_LIMIT_EXCEEDED",
                        "message", "Daily AI request limit exceeded for your subscription tier",
                        "dailyLimit", limit,
                        "tier", tier
                    ));
            }
        } catch (IllegalArgumentException e) {
            log.warn("Invalid instance ID header: {}", instanceId);
        }

        return null;
    }

    /**
     * Record usage if instance ID is present and valid.
     */
    private void recordUsageIfPresent(String instanceId) {
        if (instanceId != null && !instanceId.isBlank()) {
            try {
                aiRateLimitService.recordUsage(UUID.fromString(instanceId));
            } catch (IllegalArgumentException e) {
                log.warn("Cannot record usage for invalid instance ID: {}", instanceId);
            }
        }
    }

    // Request/Response DTOs

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyzeLogoRequest {
        @NotBlank(message = "Logo URL is required")
        private String logoUrl;

        @NotBlank(message = "Organization name is required")
        private String organizationName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateImageRequest {
        @NotBlank(message = "Organization name is required")
        private String organizationName;

        @NotBlank(message = "Theme is required")
        private String theme;

        @NotBlank(message = "Colors are required")
        private String colors;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateTextRequest {
        @NotBlank(message = "Organization name is required")
        private String organizationName;

        @NotBlank(message = "Theme is required")
        private String theme;

        @NotBlank(message = "Target audience is required")
        private String targetAudience;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageGenerationResponse {
        private String imageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextGenerationResponse {
        private String text;
    }
}
