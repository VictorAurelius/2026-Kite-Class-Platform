package com.kitehub.branding.service;

import com.kitehub.branding.client.OpenAIClient;
import com.kitehub.branding.dto.LandingPageContent;
import com.kitehub.branding.dto.LogoAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for ContentGenerationService.
 *
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ContentGenerationServiceTest {

    @Mock
    private OpenAIClient openAIClient;

    @InjectMocks
    private ContentGenerationService contentGenerationService;

    private LogoAnalysis logoAnalysis;

    @BeforeEach
    void setUp() {
        logoAnalysis = LogoAnalysis.builder()
                .primaryColors(Arrays.asList("#FF5722", "#2196F3"))
                .secondaryColors(Arrays.asList("#FFC107", "#4CAF50"))
                .theme("modern")
                .typography("Sans-serif, clean")
                .targetAudience("Students aged 15-25")
                .brandPersonality(Arrays.asList("Innovative", "Friendly", "Professional"))
                .build();
    }

    @Test
    void testGenerateLandingPageContent_Vietnamese() {
        // Given
        String orgName = "Trung Tâm Anh Ngữ Kite";
        String language = "vi";

        // Mock OpenAI responses
        when(openAIClient.generateText(anyString()))
                .thenReturn(Mono.just("Trung Tâm Tiếng Anh Hàng Đầu Việt Nam"))  // Hero title
                .thenReturn(Mono.just("Phương pháp học hiện đại, giáo viên giàu kinh nghiệm"))  // Hero subtitle
                .thenReturn(Mono.just("Học Vui, Tiến Xa"))  // Tagline
                .thenReturn(Mono.just("Chúng tôi là trung tâm tiếng Anh hàng đầu.\n\nVới đội ngũ giáo viên chuyên nghiệp.\n\nCam kết chất lượng đầu ra."))  // About Us
                .thenReturn(Mono.just("Đem đến giáo dục chất lượng cao cho mọi học viên"))  // Mission
                .thenReturn(Mono.just("Trở thành trung tâm tiếng Anh số 1 Việt Nam"));  // Vision

        // When
        LandingPageContent content = contentGenerationService.generateLandingPageContent(
                logoAnalysis, orgName, language
        );

        // Then
        assertThat(content).isNotNull();
        assertThat(content.getHeroTitle()).isNotBlank();
        assertThat(content.getHeroTitle().length()).isLessThanOrEqualTo(60);

        assertThat(content.getHeroSubtitle()).isNotBlank();
        assertThat(content.getHeroSubtitle().length()).isLessThanOrEqualTo(150);

        assertThat(content.getTagline()).isNotBlank();
        assertThat(content.getTagline().length()).isLessThanOrEqualTo(30);

        assertThat(content.getAboutUs()).isNotBlank();
        assertThat(content.getMission()).isNotBlank();
        assertThat(content.getVision()).isNotBlank();

        assertThat(content.getFeatures()).hasSize(4);
        assertThat(content.getFeatures().get(0).getTitle()).isEqualTo("Học Trực Tuyến Linh Hoạt");

        assertThat(content.getCtaText()).isEqualTo("Đăng Ký Học Thử Miễn Phí");
    }

    @Test
    void testGenerateLandingPageContent_English() {
        // Given
        String orgName = "Kite English Center";
        String language = "en";

        // Mock OpenAI responses
        when(openAIClient.generateText(anyString()))
                .thenReturn(Mono.just("Top English Learning Center in Vietnam"))  // Hero title
                .thenReturn(Mono.just("Modern teaching methods, experienced teachers"))  // Hero subtitle
                .thenReturn(Mono.just("Learn Smart, Go Far"))  // Tagline
                .thenReturn(Mono.just("We are a leading English center.\n\nWith professional teachers.\n\nQuality guarantee."))  // About Us
                .thenReturn(Mono.just("Provide high-quality education for all students"))  // Mission
                .thenReturn(Mono.just("Become the #1 English center in Vietnam"));  // Vision

        // When
        LandingPageContent content = contentGenerationService.generateLandingPageContent(
                logoAnalysis, orgName, language
        );

        // Then
        assertThat(content).isNotNull();
        assertThat(content.getFeatures()).hasSize(4);
        assertThat(content.getFeatures().get(0).getTitle()).isEqualTo("Flexible Online Learning");

        assertThat(content.getCtaText()).isEqualTo("Register for Free Trial");
    }

    @Test
    void testTruncation_HeroTitle() {
        // Given
        String orgName = "Test Org";
        String language = "vi";

        // Mock very long title
        when(openAIClient.generateText(anyString()))
                .thenReturn(Mono.just("This is a very long hero title that exceeds sixty characters limit"))
                .thenReturn(Mono.just("Short subtitle"))
                .thenReturn(Mono.just("Short"))
                .thenReturn(Mono.just("About us"))
                .thenReturn(Mono.just("Mission"))
                .thenReturn(Mono.just("Vision"));

        // When
        LandingPageContent content = contentGenerationService.generateLandingPageContent(
                logoAnalysis, orgName, language
        );

        // Then
        assertThat(content.getHeroTitle().length()).isLessThanOrEqualTo(60);
        assertThat(content.getHeroTitle()).endsWith("...");
    }

    @Test
    void testTruncation_HeroSubtitle() {
        // Given
        String orgName = "Test Org";
        String language = "vi";

        // Mock very long subtitle
        when(openAIClient.generateText(anyString()))
                .thenReturn(Mono.just("Short title"))
                .thenReturn(Mono.just("This is a very long hero subtitle that definitely exceeds the maximum limit of one hundred and fifty characters for sure and should be truncated properly"))
                .thenReturn(Mono.just("Short"))
                .thenReturn(Mono.just("About us"))
                .thenReturn(Mono.just("Mission"))
                .thenReturn(Mono.just("Vision"));

        // When
        LandingPageContent content = contentGenerationService.generateLandingPageContent(
                logoAnalysis, orgName, language
        );

        // Then
        assertThat(content.getHeroSubtitle().length()).isLessThanOrEqualTo(150);
        assertThat(content.getHeroSubtitle()).endsWith("...");
    }

    @Test
    void testTruncation_Tagline() {
        // Given
        String orgName = "Test Org";
        String language = "vi";

        // Mock very long tagline
        when(openAIClient.generateText(anyString()))
                .thenReturn(Mono.just("Short title"))
                .thenReturn(Mono.just("Short subtitle"))
                .thenReturn(Mono.just("This tagline is way too long for display"))
                .thenReturn(Mono.just("About us"))
                .thenReturn(Mono.just("Mission"))
                .thenReturn(Mono.just("Vision"));

        // When
        LandingPageContent content = contentGenerationService.generateLandingPageContent(
                logoAnalysis, orgName, language
        );

        // Then
        assertThat(content.getTagline().length()).isLessThanOrEqualTo(30);
        assertThat(content.getTagline()).endsWith("...");
    }

    @Test
    void testQuoteRemoval() {
        // Given
        String orgName = "Test Org";
        String language = "vi";

        // Mock responses with quotes
        when(openAIClient.generateText(anyString()))
                .thenReturn(Mono.just("\"Hero Title with Quotes\""))
                .thenReturn(Mono.just("\"Hero Subtitle with Quotes\""))
                .thenReturn(Mono.just("\"Tagline\""))
                .thenReturn(Mono.just("About us"))
                .thenReturn(Mono.just("Mission"))
                .thenReturn(Mono.just("Vision"));

        // When
        LandingPageContent content = contentGenerationService.generateLandingPageContent(
                logoAnalysis, orgName, language
        );

        // Then
        assertThat(content.getHeroTitle()).doesNotContain("\"");
        assertThat(content.getHeroSubtitle()).doesNotContain("\"");
        assertThat(content.getTagline()).doesNotContain("\"");
    }
}
