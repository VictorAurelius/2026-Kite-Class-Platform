package com.kitehub.branding.service;

import com.kitehub.branding.client.OpenAIClient;
import com.kitehub.branding.dto.LogoAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service for AI-powered branding generation.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIBrandingService {

    private final OpenAIClient openAIClient;

    /**
     * Analyze logo and extract brand identity.
     *
     * @param logoUrl Logo image URL
     * @param organizationName Organization name
     * @return Logo analysis
     */
    public Mono<LogoAnalysis> analyzeLogo(String logoUrl, String organizationName) {
        log.info("Analyzing logo for: {}", organizationName);
        return openAIClient.analyzeLogo(logoUrl, organizationName);
    }

    /**
     * Generate hero banner image.
     *
     * @param organizationName Organization name
     * @param theme Brand theme
     * @param colors Primary colors
     * @return Generated image URL
     */
    public Mono<String> generateHeroImage(String organizationName, String theme, String colors) {
        String prompt = buildHeroImagePrompt(organizationName, theme, colors);
        return openAIClient.generateImage(prompt, "1792x1024");
    }

    /**
     * Generate marketing copy in Vietnamese.
     *
     * @param organizationName Organization name
     * @param theme Brand theme
     * @param targetAudience Target audience
     * @return Generated marketing copy
     */
    public Mono<String> generateMarketingCopy(String organizationName, String theme, String targetAudience) {
        String prompt = buildMarketingCopyPrompt(organizationName, theme, targetAudience);
        return openAIClient.generateText(prompt);
    }

    /**
     * Build hero image generation prompt.
     *
     * @param organizationName Organization name
     * @param theme Brand theme
     * @param colors Primary colors
     * @return Formatted prompt
     */
    private String buildHeroImagePrompt(String organizationName, String theme, String colors) {
        return String.format("""
            Professional hero banner for %s, education center,
            %s style, colors: %s,
            1920x600px, no text, photorealistic, high quality,
            warm and inviting atmosphere, modern classroom or learning environment
            """, organizationName, theme, colors);
    }

    /**
     * Build marketing copy generation prompt.
     *
     * @param organizationName Organization name
     * @param theme Brand theme
     * @param targetAudience Target audience
     * @return Formatted prompt
     */
    private String buildMarketingCopyPrompt(String organizationName, String theme, String targetAudience) {
        return String.format("""
            Write Vietnamese marketing copy for %s education center:
            1. Catchy hero title (max 60 characters)
            2. Compelling subtitle (max 150 characters)
            3. Brand tagline (max 30 characters)

            Style: %s
            Target audience: %s

            Requirements:
            - Use Vietnamese language
            - Focus on educational value
            - Emphasize quality and results
            - Professional but friendly tone

            Return in JSON format with fields: heroTitle, heroSubtitle, tagline
            """, organizationName, theme, targetAudience);
    }
}
