package com.kitehub.branding.service;

import com.kitehub.branding.client.OpenAIClient;
import com.kitehub.branding.dto.Feature;
import com.kitehub.branding.dto.LandingPageContent;
import com.kitehub.branding.dto.LogoAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Service for generating landing page content using AI.
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentGenerationService {

    private final OpenAIClient openAIClient;

    /**
     * Generate complete landing page content based on logo analysis.
     *
     * @param logoAnalysis Logo analysis data
     * @param orgName Organization name
     * @param language Content language (vi for Vietnamese)
     * @return Generated landing page content
     */
    public LandingPageContent generateLandingPageContent(
            LogoAnalysis logoAnalysis,
            String orgName,
            String language
    ) {
        log.info("Generating landing page content for: {}", orgName);

        String theme = logoAnalysis.getTheme();
        String targetAudience = logoAnalysis.getTargetAudience();
        String brandPersonality = String.join(", ", logoAnalysis.getBrandPersonality());

        return LandingPageContent.builder()
                .heroTitle(generateHeroTitle(orgName, theme, language))
                .heroSubtitle(generateHeroSubtitle(orgName, theme, targetAudience, language))
                .tagline(generateTagline(orgName, brandPersonality, language))
                .aboutUs(generateAboutUs(orgName, theme, language))
                .mission(generateMission(orgName, targetAudience, language))
                .vision(generateVision(orgName, theme, language))
                .features(generateFeatures(orgName, theme, language))
                .ctaText(generateCtaText(language))
                .build();
    }

    /**
     * Generate hero title (max 60 chars).
     */
    private String generateHeroTitle(String orgName, String theme, String language) {
        String prompt = String.format(
                "Create a catchy %s hero title for '%s', " +
                "an education center with %s style. " +
                "Max 60 characters. Return ONLY the title text, no quotes.",
                language.equals("vi") ? "Vietnamese" : "English",
                orgName,
                theme
        );

        String title = openAIClient.generateText(prompt).block();
        return truncate(title.trim().replace("\"", ""), 60);
    }

    /**
     * Generate hero subtitle (max 150 chars).
     */
    private String generateHeroSubtitle(String orgName, String theme, String targetAudience, String language) {
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

        String subtitle = openAIClient.generateText(prompt).block();
        return truncate(subtitle.trim().replace("\"", ""), 150);
    }

    /**
     * Generate tagline (max 30 chars).
     */
    private String generateTagline(String orgName, String brandPersonality, String language) {
        String prompt = String.format(
                "Create a memorable %s tagline for '%s', " +
                "with personality: %s. " +
                "Max 30 characters. Return ONLY the tagline, no quotes.",
                language.equals("vi") ? "Vietnamese" : "English",
                orgName,
                brandPersonality
        );

        String tagline = openAIClient.generateText(prompt).block();
        return truncate(tagline.trim().replace("\"", ""), 30);
    }

    /**
     * Generate About Us section (3 paragraphs).
     */
    private String generateAboutUs(String orgName, String theme, String language) {
        String prompt = String.format(
                "Write a %s 'About Us' section for '%s', " +
                "an education center with %s style. " +
                "Write 3 short paragraphs (2-3 sentences each) in a warm, engaging tone. " +
                "Return ONLY the text, no title.",
                language.equals("vi") ? "Vietnamese" : "English",
                orgName,
                theme
        );

        return openAIClient.generateText(prompt).block().trim();
    }

    /**
     * Generate mission statement (1-2 sentences).
     */
    private String generateMission(String orgName, String targetAudience, String language) {
        String prompt = String.format(
                "Write a %s mission statement for '%s', " +
                "an education center serving %s. " +
                "1-2 sentences max. Return ONLY the mission text.",
                language.equals("vi") ? "Vietnamese" : "English",
                orgName,
                targetAudience
        );

        return openAIClient.generateText(prompt).block().trim();
    }

    /**
     * Generate vision statement (1-2 sentences).
     */
    private String generateVision(String orgName, String theme, String language) {
        String prompt = String.format(
                "Write a %s vision statement for '%s', " +
                "an education center with %s style. " +
                "1-2 sentences max. Return ONLY the vision text.",
                language.equals("vi") ? "Vietnamese" : "English",
                orgName,
                theme
        );

        return openAIClient.generateText(prompt).block().trim();
    }

    /**
     * Generate feature highlights (3-5 features).
     */
    private List<Feature> generateFeatures(String orgName, String theme, String language) {
        // TODO: Implement proper JSON parsing in future PR
        // String prompt = String.format(
        //         "List 4 key features of '%s', an education center with %s style. " +
        //         "For each feature, provide: " +
        //         "1. Title (max 30 chars in %s) " +
        //         "2. Description (1-2 sentences in %s) " +
        //         "Format as JSON array: [{\"title\": \"...\", \"description\": \"...\", \"icon\": \"video|calendar|trophy|users\"}]",
        //         orgName,
        //         theme,
        //         language.equals("vi") ? "Vietnamese" : "English",
        //         language.equals("vi") ? "Vietnamese" : "English"
        // );

        // For now, return mock features (OpenAI JSON parsing can be complex)
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
