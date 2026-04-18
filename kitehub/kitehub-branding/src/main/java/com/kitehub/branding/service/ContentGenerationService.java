package com.kitehub.branding.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.client.AIClient;
import com.kitehub.branding.dto.Feature;
import com.kitehub.branding.dto.LandingPageContent;
import com.kitehub.branding.dto.LogoAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * Service for generating landing page content using AI.
 *
 * <p>Fully reactive: public API returns {@link Mono} — no {@code .block()} calls
 * in the production path (GAP-002 async pipeline).</p>
 *
 * <p>Callers are expected to compose reactively; controllers adapt the returned
 * {@link Mono} to async Servlet / WebFlux responses.</p>
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentGenerationService {

    private final AIClient aiClient;
    private final ObjectMapper objectMapper;

    /**
     * Generate complete landing page content based on logo analysis.
     *
     * <p>Reactive pipeline — each field is generated via a separate AI call and
     * combined via {@link Mono#zip}. No blocking calls in production code.</p>
     *
     * @param logoAnalysis Logo analysis data
     * @param orgName Organization name
     * @param language Content language (vi for Vietnamese)
     * @return Mono emitting the generated landing page content
     */
    public Mono<LandingPageContent> generateLandingPageContent(
            LogoAnalysis logoAnalysis,
            String orgName,
            String language
    ) {
        log.info("Generating landing page content for: {}", orgName);

        String theme = logoAnalysis.getTheme();
        String targetAudience = logoAnalysis.getTargetAudience();
        String brandPersonality = String.join(", ", logoAnalysis.getBrandPersonality());

        Mono<String> heroTitle = generateHeroTitle(orgName, theme, language);
        Mono<String> heroSubtitle = generateHeroSubtitle(orgName, theme, targetAudience, language);
        Mono<String> tagline = generateTagline(orgName, brandPersonality, language);
        Mono<String> aboutUs = generateAboutUs(orgName, theme, language);
        Mono<String> mission = generateMission(orgName, targetAudience, language);
        Mono<String> vision = generateVision(orgName, theme, language);
        Mono<List<Feature>> features = generateFeatures(orgName, theme, language);

        // Zip strings first (max 8 args per Mono.zip), then combine with features + CTA.
        return Mono.zip(heroTitle, heroSubtitle, tagline, aboutUs, mission, vision, features)
                .map(tuple -> LandingPageContent.builder()
                        .heroTitle(tuple.getT1())
                        .heroSubtitle(tuple.getT2())
                        .tagline(tuple.getT3())
                        .aboutUs(tuple.getT4())
                        .mission(tuple.getT5())
                        .vision(tuple.getT6())
                        .features(tuple.getT7())
                        .ctaText(generateCtaText(language))
                        .build());
    }

    /**
     * Generate hero title (max 60 chars).
     */
    private Mono<String> generateHeroTitle(String orgName, String theme, String language) {
        String prompt = String.format(
                "Create a catchy %s hero title for '%s', " +
                "an education center with %s style. " +
                "Max 60 characters. Return ONLY the title text, no quotes.",
                language.equals("vi") ? "Vietnamese" : "English",
                orgName,
                theme
        );

        return aiClient.generateText(prompt)
                .map(title -> truncate(title.trim().replace("\"", ""), 60));
    }

    /**
     * Generate hero subtitle (max 150 chars).
     */
    private Mono<String> generateHeroSubtitle(String orgName, String theme, String targetAudience, String language) {
        String prompt = String.format(
                "Write a compelling %s subtitle for '%s', " +
                "education center targeting %s. " +
                "Describe the value proposition in %s tone. " +
                "Max 150 characters. Return ONLY the subtitle text, no quotes.",
                language.equals("vi") ? "Vietnamese" : "English",
                orgName,
                targetAudience,
                theme
        );

        return aiClient.generateText(prompt)
                .map(subtitle -> truncate(subtitle.trim().replace("\"", ""), 150));
    }

    /**
     * Generate tagline (max 30 chars).
     */
    private Mono<String> generateTagline(String orgName, String brandPersonality, String language) {
        String prompt = String.format(
                "Create a memorable %s tagline for '%s', " +
                "with personality: %s. " +
                "Max 30 characters. Return ONLY the tagline, no quotes.",
                language.equals("vi") ? "Vietnamese" : "English",
                orgName,
                brandPersonality
        );

        return aiClient.generateText(prompt)
                .map(tagline -> truncate(tagline.trim().replace("\"", ""), 30));
    }

    /**
     * Generate About Us section (3 paragraphs).
     */
    private Mono<String> generateAboutUs(String orgName, String theme, String language) {
        String prompt = String.format(
                "Write a %s 'About Us' section for '%s', " +
                "an education center with %s style. " +
                "Write 3 short paragraphs (2-3 sentences each) in a warm, engaging tone. " +
                "Return ONLY the text, no title.",
                language.equals("vi") ? "Vietnamese" : "English",
                orgName,
                theme
        );

        return aiClient.generateText(prompt).map(String::trim);
    }

    /**
     * Generate mission statement (1-2 sentences).
     */
    private Mono<String> generateMission(String orgName, String targetAudience, String language) {
        String prompt = String.format(
                "Write a %s mission statement for '%s', " +
                "an education center serving %s. " +
                "1-2 sentences max. Return ONLY the mission text.",
                language.equals("vi") ? "Vietnamese" : "English",
                orgName,
                targetAudience
        );

        return aiClient.generateText(prompt).map(String::trim);
    }

    /**
     * Generate vision statement (1-2 sentences).
     */
    private Mono<String> generateVision(String orgName, String theme, String language) {
        String prompt = String.format(
                "Write a %s vision statement for '%s', " +
                "an education center with %s style. " +
                "1-2 sentences max. Return ONLY the vision text.",
                language.equals("vi") ? "Vietnamese" : "English",
                orgName,
                theme
        );

        return aiClient.generateText(prompt).map(String::trim);
    }

    /**
     * Generate feature highlights (3-5 features).
     *
     * <p>Parses JSON response from AI; falls back to localized mock list on any
     * error so the landing page still renders.</p>
     */
    private Mono<List<Feature>> generateFeatures(String orgName, String theme, String language) {
        String prompt = String.format(
                "List 4 key features of '%s', an education center with %s style. " +
                "For each feature, provide: " +
                "1. Title (max 30 chars in %s) " +
                "2. Description (1-2 sentences in %s) " +
                "3. Icon (choose from: video, calendar, trophy, users, star, book) " +
                "Format as JSON array: [{\"title\": \"...\", \"description\": \"...\", \"icon\": \"...\"}] " +
                "Return ONLY valid JSON array, no additional text.",
                orgName,
                theme,
                language.equals("vi") ? "Vietnamese" : "English",
                language.equals("vi") ? "Vietnamese" : "English"
        );

        return aiClient.generateText(prompt)
                .map(response -> parseFeaturesJson(response, orgName))
                .onErrorResume(err -> {
                    log.error("Failed to generate features via AI: {}", err.getMessage());
                    log.warn("Using fallback mock features for {}", language);
                    return Mono.just(getMockFeatures(language));
                });
    }

    /**
     * Parse features JSON, falling back to mocks on parse failure.
     */
    private List<Feature> parseFeaturesJson(String response, String orgName) {
        try {
            String trimmed = response.trim();
            log.debug("Raw features response: {}", trimmed);

            String jsonContent = extractJson(trimmed);
            List<Feature> features = objectMapper.readValue(
                    jsonContent,
                    new TypeReference<List<Feature>>() {}
            );

            log.info("Successfully generated {} features for {}", features.size(), orgName);
            return features;
        } catch (Exception e) {
            // Re-throw so the outer onErrorResume returns mock features.
            throw new IllegalStateException("Failed to parse features JSON", e);
        }
    }

    /**
     * Get fallback mock features for development/testing.
     *
     * @param language Content language
     * @return List of mock features
     */
    private List<Feature> getMockFeatures(String language) {
        if (language.equals("vi")) {
            return Arrays.asList(
                    Feature.builder()
                            .title("Học Trực Tuyến Linh Hoạt")
                            .description("Học mọi lúc, mọi nơi với nền tảng trực tuyến hiện đại")
                            .icon("video")
                            .build(),
                    Feature.builder()
                            .title("Giáo Viên Giàu Kinh Nghiệm")
                            .description("Đội ngũ giảng viên chuyên nghiệp, tận tâm với học viên")
                            .icon("users")
                            .build(),
                    Feature.builder()
                            .title("Lộ Trình Cá Nhân Hóa")
                            .description("Chương trình học được thiết kế riêng cho từng học viên")
                            .icon("calendar")
                            .build(),
                    Feature.builder()
                            .title("Cam Kết Đầu Ra")
                            .description("Đảm bảo chất lượng và kết quả học tập rõ ràng")
                            .icon("trophy")
                            .build()
            );
        } else {
            return Arrays.asList(
                    Feature.builder()
                            .title("Flexible Online Learning")
                            .description("Learn anytime, anywhere with our modern online platform")
                            .icon("video")
                            .build(),
                    Feature.builder()
                            .title("Experienced Teachers")
                            .description("Professional and dedicated teaching staff")
                            .icon("users")
                            .build(),
                    Feature.builder()
                            .title("Personalized Curriculum")
                            .description("Customized learning path for each student")
                            .icon("calendar")
                            .build(),
                    Feature.builder()
                            .title("Quality Guarantee")
                            .description("Clear learning outcomes and quality assurance")
                            .icon("trophy")
                            .build()
            );
        }
    }

    /**
     * Extract JSON from content string.
     * OpenAI sometimes returns JSON with markdown code blocks (```json ... ```).
     *
     * @param content Content string potentially containing JSON
     * @return Extracted JSON string
     */
    private String extractJson(String content) {
        // Remove markdown code blocks if present
        String cleaned = content.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7); // Remove ```json
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3); // Remove ```
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3); // Remove trailing ```
        }

        return cleaned.trim();
    }

    /**
     * Generate CTA text (max 40 chars).
     */
    private String generateCtaText(String language) {
        if (language.equals("vi")) {
            return "Đăng Ký Học Thử Miễn Phí";
        } else {
            return "Register for Free Trial";
        }
    }

    /**
     * Truncate text to max length.
     */
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
