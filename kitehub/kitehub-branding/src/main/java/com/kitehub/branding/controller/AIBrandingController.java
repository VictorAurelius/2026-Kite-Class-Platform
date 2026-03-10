package com.kitehub.branding.controller;

import com.kitehub.branding.dto.LogoAnalysis;
import com.kitehub.branding.service.AIBrandingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for AI branding operations.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/platform/ai")
@RequiredArgsConstructor
public class AIBrandingController {

    private final AIBrandingService aiBrandingService;

    /**
     * Analyze logo and extract brand identity.
     *
     * @param request Logo analysis request
     * @return Logo analysis result
     */
    @PostMapping("/analyze-logo")
    public Mono<ResponseEntity<LogoAnalysis>> analyzeLogo(@Valid @RequestBody AnalyzeLogoRequest request) {
        return aiBrandingService.analyzeLogo(request.getLogoUrl(), request.getOrganizationName())
            .map(ResponseEntity::ok);
    }

    /**
     * Generate hero banner image.
     *
     * @param request Image generation request
     * @return Generated image URL
     */
    @PostMapping("/generate-image")
    public Mono<ResponseEntity<ImageGenerationResponse>> generateImage(
        @Valid @RequestBody GenerateImageRequest request
    ) {
        return aiBrandingService.generateHeroImage(
                request.getOrganizationName(),
                request.getTheme(),
                request.getColors()
            )
            .map(imageUrl -> ResponseEntity.ok(new ImageGenerationResponse(imageUrl)));
    }

    /**
     * Generate marketing copy.
     *
     * @param request Text generation request
     * @return Generated marketing copy
     */
    @PostMapping("/generate-text")
    public Mono<ResponseEntity<TextGenerationResponse>> generateText(
        @Valid @RequestBody GenerateTextRequest request
    ) {
        return aiBrandingService.generateMarketingCopy(
                request.getOrganizationName(),
                request.getTheme(),
                request.getTargetAudience()
            )
            .map(text -> ResponseEntity.ok(new TextGenerationResponse(text)));
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
