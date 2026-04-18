package com.kitehub.branding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.client.AIClient;
import com.kitehub.branding.dto.LandingPageContent;
import com.kitehub.branding.dto.LogoAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for ContentGenerationService (reactive — returns {@link Mono}).
 *
 * <p>GAP-002: service no longer calls {@code .block()}. Tests use
 * {@code Mono#block()} only in assertions to materialize results — which is
 * acceptable in test code.</p>
 *
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ContentGenerationServiceTest {

    @Mock
    private AIClient aiClient;

    private ContentGenerationService contentGenerationService;

    private LogoAnalysis logoAnalysis;

    @BeforeEach
    void setUp() {
        // ObjectMapper is real — we don't need to mock JSON parsing.
        contentGenerationService = new ContentGenerationService(aiClient, new ObjectMapper());
        logoAnalysis = LogoAnalysis.builder()
                .primaryColor("#FF5722")
                .secondaryColor("#2196F3")
                .accentColor("#4CAF50")
                .theme("MODERN")
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

        // Mock AI responses — Mono.zip subscribes in parallel, so consecutive
        // thenReturn stubs would map 1:1 to the 7 generators (6 strings + features).
        // We instead use a sequential stub to keep predictable ordering.
        when(aiClient.generateText(anyString()))
                .thenReturn(Mono.just("Trung Tâm Tiếng Anh Hàng Đầu Việt Nam"))
                .thenReturn(Mono.just("Phương pháp học hiện đại, giáo viên giàu kinh nghiệm"))
                .thenReturn(Mono.just("Học Vui, Tiến Xa"))
                .thenReturn(Mono.just("Chúng tôi là trung tâm tiếng Anh hàng đầu.\n\nVới đội ngũ giáo viên chuyên nghiệp.\n\nCam kết chất lượng đầu ra."))
                .thenReturn(Mono.just("Đem đến giáo dục chất lượng cao cho mọi học viên"))
                .thenReturn(Mono.just("Trở thành trung tâm tiếng Anh số 1 Việt Nam"))
                .thenReturn(Mono.just("not-valid-json"));  // features → triggers mock fallback

        // When
        LandingPageContent content = contentGenerationService.generateLandingPageContent(
                logoAnalysis, orgName, language
        ).block();

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

        // Fallback kicks in when JSON parse fails → mock features.
        assertThat(content.getFeatures()).hasSize(4);
        assertThat(content.getFeatures().get(0).getTitle()).isEqualTo("Học Trực Tuyến Linh Hoạt");

        assertThat(content.getCtaText()).isEqualTo("Đăng Ký Học Thử Miễn Phí");
    }

    @Test
    void testGenerateLandingPageContent_English() {
        // Given
        String orgName = "Kite English Center";
        String language = "en";

        when(aiClient.generateText(anyString()))
                .thenReturn(Mono.just("Top English Learning Center in Vietnam"))
                .thenReturn(Mono.just("Modern teaching methods, experienced teachers"))
                .thenReturn(Mono.just("Learn Smart, Go Far"))
                .thenReturn(Mono.just("We are a leading English center.\n\nWith professional teachers.\n\nQuality guarantee."))
                .thenReturn(Mono.just("Provide high-quality education for all students"))
                .thenReturn(Mono.just("Become the #1 English center in Vietnam"))
                .thenReturn(Mono.just("invalid-json"));  // triggers fallback

        // When
        LandingPageContent content = contentGenerationService.generateLandingPageContent(
                logoAnalysis, orgName, language
        ).block();

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

        when(aiClient.generateText(anyString()))
                .thenReturn(Mono.just("This is a very long hero title that exceeds sixty characters limit"))
                .thenReturn(Mono.just("Short subtitle"))
                .thenReturn(Mono.just("Short"))
                .thenReturn(Mono.just("About us"))
                .thenReturn(Mono.just("Mission"))
                .thenReturn(Mono.just("Vision"))
                .thenReturn(Mono.just("invalid-json"));

        // When
        LandingPageContent content = contentGenerationService.generateLandingPageContent(
                logoAnalysis, orgName, language
        ).block();

        // Then
        assertThat(content.getHeroTitle().length()).isLessThanOrEqualTo(60);
        assertThat(content.getHeroTitle()).endsWith("...");
    }

    @Test
    void testTruncation_HeroSubtitle() {
        // Given
        String orgName = "Test Org";
        String language = "vi";

        when(aiClient.generateText(anyString()))
                .thenReturn(Mono.just("Short title"))
                .thenReturn(Mono.just("This is a very long hero subtitle that definitely exceeds the maximum limit of one hundred and fifty characters for sure and should be truncated properly"))
                .thenReturn(Mono.just("Short"))
                .thenReturn(Mono.just("About us"))
                .thenReturn(Mono.just("Mission"))
                .thenReturn(Mono.just("Vision"))
                .thenReturn(Mono.just("invalid-json"));

        // When
        LandingPageContent content = contentGenerationService.generateLandingPageContent(
                logoAnalysis, orgName, language
        ).block();

        // Then
        assertThat(content.getHeroSubtitle().length()).isLessThanOrEqualTo(150);
        assertThat(content.getHeroSubtitle()).endsWith("...");
    }

    @Test
    void testTruncation_Tagline() {
        // Given
        String orgName = "Test Org";
        String language = "vi";

        when(aiClient.generateText(anyString()))
                .thenReturn(Mono.just("Short title"))
                .thenReturn(Mono.just("Short subtitle"))
                .thenReturn(Mono.just("This tagline is way too long for display"))
                .thenReturn(Mono.just("About us"))
                .thenReturn(Mono.just("Mission"))
                .thenReturn(Mono.just("Vision"))
                .thenReturn(Mono.just("invalid-json"));

        // When
        LandingPageContent content = contentGenerationService.generateLandingPageContent(
                logoAnalysis, orgName, language
        ).block();

        // Then
        assertThat(content.getTagline().length()).isLessThanOrEqualTo(30);
        assertThat(content.getTagline()).endsWith("...");
    }

    @Test
    void testQuoteRemoval() {
        // Given
        String orgName = "Test Org";
        String language = "vi";

        when(aiClient.generateText(anyString()))
                .thenReturn(Mono.just("\"Hero Title with Quotes\""))
                .thenReturn(Mono.just("\"Hero Subtitle with Quotes\""))
                .thenReturn(Mono.just("\"Tagline\""))
                .thenReturn(Mono.just("About us"))
                .thenReturn(Mono.just("Mission"))
                .thenReturn(Mono.just("Vision"))
                .thenReturn(Mono.just("invalid-json"));

        // When
        LandingPageContent content = contentGenerationService.generateLandingPageContent(
                logoAnalysis, orgName, language
        ).block();

        // Then
        assertThat(content.getHeroTitle()).doesNotContain("\"");
        assertThat(content.getHeroSubtitle()).doesNotContain("\"");
        assertThat(content.getTagline()).doesNotContain("\"");
    }
}
