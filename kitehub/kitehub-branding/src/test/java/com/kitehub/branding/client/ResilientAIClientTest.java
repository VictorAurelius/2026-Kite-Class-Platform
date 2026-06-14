package com.kitehub.branding.client;

import com.kitehub.branding.dto.LogoAnalysis;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying the {@link ResilientAIClient} decorator delegates happy-path
 * calls and that its fallback methods return domain-safe values suitable for
 * template-first routing (GAP-148).
 *
 * <p>Real circuit-breaker behavior (state transitions under load) is covered by
 * Resilience4j's own test suite + Spring integration tests when the stack runs.
 * Here we assert the wrapper wiring + fallback outputs — the pieces GAP-148
 * introduces.
 *
 * @since 3.21.0 (GAP-148, Wave 9-D)
 */
@ExtendWith(MockitoExtension.class)
class ResilientAIClientTest {

    @Mock
    private AIClient delegate;

    @Test
    @DisplayName("analyzeLogo delegates to wrapped client on happy path")
    void analyzeLogo_delegates() {
        LogoAnalysis result = LogoAnalysis.builder().primaryColor("#fff").theme("MODERN").build();
        when(delegate.analyzeLogo("http://logo.png", "Acme")).thenReturn(Mono.just(result));

        ResilientAIClient client = new ResilientAIClient(delegate);

        StepVerifier.create(client.analyzeLogo("http://logo.png", "Acme"))
                .expectNext(result)
                .verifyComplete();
        verify(delegate).analyzeLogo("http://logo.png", "Acme");
    }

    @Test
    @DisplayName("generateImage delegates to wrapped client on happy path")
    void generateImage_delegates() {
        when(delegate.generateImage("a prompt", "1024x1024"))
                .thenReturn(Mono.just("https://example.com/img.png"));

        ResilientAIClient client = new ResilientAIClient(delegate);

        StepVerifier.create(client.generateImage("a prompt", "1024x1024"))
                .expectNext("https://example.com/img.png")
                .verifyComplete();
    }

    @Test
    @DisplayName("generateText delegates to wrapped client on happy path")
    void generateText_delegates() {
        when(delegate.generateText(anyString())).thenReturn(Mono.just("hi"));

        ResilientAIClient client = new ResilientAIClient(delegate);

        StepVerifier.create(client.generateText("prompt"))
                .expectNext("hi")
                .verifyComplete();
    }

    @Test
    @DisplayName("getProviderName reports delegate wrapped")
    void providerName_wrapsDelegate() {
        when(delegate.getProviderName()).thenReturn("openai-mock");
        ResilientAIClient client = new ResilientAIClient(delegate);
        assertThat(client.getProviderName()).isEqualTo("resilient(openai-mock)");
    }

    // ---- Fallbacks ------------------------------------------------------------
    // Invoke fallback via reflection since Resilience4j normally calls them.
    // This proves the fallbacks produce safe domain values without requiring a
    // full Resilience4j test harness.

    @Test
    @DisplayName("analyzeLogoFallback returns template-safe defaults")
    void analyzeLogoFallback_returnsDefaults() throws Exception {
        ResilientAIClient client = new ResilientAIClient(delegate);
        var method = ResilientAIClient.class.getDeclaredMethod(
                "analyzeLogoFallback", String.class, String.class, Throwable.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Mono<LogoAnalysis> mono = (Mono<LogoAnalysis>) method.invoke(
                client, "http://logo.png", "Acme", new RuntimeException("simulated"));

        LogoAnalysis analysis = mono.block();
        assertThat(analysis).isNotNull();
        assertThat(analysis.getTheme()).isEqualTo("MODERN");
        assertThat(analysis.getRawAnalysis()).contains("Fallback");
        assertThat(analysis.getBrandPersonality()).isNotEmpty();
    }

    @Test
    @DisplayName("generateImageFallback returns deterministic placeholder URL")
    void generateImageFallback_returnsPlaceholder() throws Exception {
        ResilientAIClient client = new ResilientAIClient(delegate);
        var method = ResilientAIClient.class.getDeclaredMethod(
                "generateImageFallback", String.class, String.class, Throwable.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Mono<String> mono = (Mono<String>) method.invoke(
                client, "prompt", "1792x1024", new RuntimeException("cb open"));

        assertThat(mono.block()).isEqualTo("https://placehold.co/1792x1024/2563EB/white?text=Template");
    }

    @Test
    @DisplayName("generateTextFallback returns Vietnamese default copy")
    void generateTextFallback_returnsDefaultCopy() throws Exception {
        ResilientAIClient client = new ResilientAIClient(delegate);
        var method = ResilientAIClient.class.getDeclaredMethod(
                "generateTextFallback", String.class, Throwable.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Mono<String> mono = (Mono<String>) method.invoke(
                client, "prompt", new RuntimeException("cb open"));

        String copy = mono.block();
        assertThat(copy).contains("Chào mừng");
    }

    @Test
    @DisplayName("CB_NAME constant matches application.yml instance key")
    void cbNameConstant_matchesYmlKey() {
        assertThat(ResilientAIClient.CB_NAME).isEqualTo("ai-provider");
    }

    // ---- GAP-1356: @Bulkhead bounds concurrent AI calls -----------------------

    @Test
    @DisplayName("GAP-1356: every external AI call method carries @Bulkhead(ai-provider)")
    void externalAiMethods_haveBulkhead() throws Exception {
        for (String method : new String[]{"analyzeLogo", "generateImage", "generateText"}) {
            Bulkhead bulkhead = ResilientAIClient.class
                    .getDeclaredMethod(method, method.equals("generateText")
                            ? new Class[]{String.class}
                            : new Class[]{String.class, String.class})
                    .getAnnotation(Bulkhead.class);
            assertThat(bulkhead)
                    .as("%s must be @Bulkhead-annotated", method)
                    .isNotNull();
            assertThat(bulkhead.name()).isEqualTo(ResilientAIClient.CB_NAME);
        }
        // strict image-gen path (no CB fallback) must also be bulkheaded
        Bulkhead strict = ResilientAIClient.class
                .getDeclaredMethod("generateImageStrict", String.class, String.class)
                .getAnnotation(Bulkhead.class);
        assertThat(strict).isNotNull();
        assertThat(strict.name()).isEqualTo(ResilientAIClient.CB_NAME);
    }
}
