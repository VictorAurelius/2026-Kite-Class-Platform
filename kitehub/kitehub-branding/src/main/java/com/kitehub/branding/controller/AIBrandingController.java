package com.kitehub.branding.controller;

import com.kitehub.branding.dto.LogoAnalysis;
import com.kitehub.branding.dto.ThemeConfig;
import com.kitehub.branding.service.AIBrandingService;
import com.kitehub.branding.service.AIRateLimitService;
import com.kitehub.branding.service.ThemeGenerationService;
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
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/branding/ai")
@RequiredArgsConstructor
@Tag(name = "AI Branding", description = "AI-powered logo analysis, image generation, and theme creation")
public class AIBrandingController {

    private final AIBrandingService aiBrandingService;
    private final ThemeGenerationService themeGenerationService;
    private final AIRateLimitService aiRateLimitService;

    /**
     * Analyze logo and extract brand identity.
     *
     * @param request Logo analysis request
     * @param instanceId Instance ID from gateway header
     * @param tier Subscription tier from gateway header
     * @return Logo analysis result
     */
    @PostMapping("/analyze-logo")
    public Mono<ResponseEntity<Object>> analyzeLogo(
            @Valid @RequestBody AnalyzeLogoRequest request,
            @RequestHeader(value = "X-Instance-Id", required = false) String instanceId,
            @RequestHeader(value = "X-Subscription-Tier", required = false, defaultValue = "FREE") String tier
    ) {
        ResponseEntity<Object> rateLimitResponse = checkRateLimit(instanceId, tier);
        if (rateLimitResponse != null) {
            return Mono.just(rateLimitResponse);
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
    public Mono<ResponseEntity<Object>> generateImage(
            @Valid @RequestBody GenerateImageRequest request,
            @RequestHeader(value = "X-Instance-Id", required = false) String instanceId,
            @RequestHeader(value = "X-Subscription-Tier", required = false, defaultValue = "FREE") String tier
    ) {
        ResponseEntity<Object> rateLimitResponse = checkRateLimit(instanceId, tier);
        if (rateLimitResponse != null) {
            return Mono.just(rateLimitResponse);
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
    public Mono<ResponseEntity<Object>> generateText(
            @Valid @RequestBody GenerateTextRequest request,
            @RequestHeader(value = "X-Instance-Id", required = false) String instanceId,
            @RequestHeader(value = "X-Subscription-Tier", required = false, defaultValue = "FREE") String tier
    ) {
        ResponseEntity<Object> rateLimitResponse = checkRateLimit(instanceId, tier);
        if (rateLimitResponse != null) {
            return Mono.just(rateLimitResponse);
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
    public ResponseEntity<Object> generateTheme(
            @Valid @RequestBody LogoAnalysis analysis,
            @RequestHeader(value = "X-Instance-Id", required = false) String instanceId,
            @RequestHeader(value = "X-Subscription-Tier", required = false, defaultValue = "FREE") String tier
    ) {
        ResponseEntity<Object> rateLimitResponse = checkRateLimit(instanceId, tier);
        if (rateLimitResponse != null) {
            return rateLimitResponse;
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
