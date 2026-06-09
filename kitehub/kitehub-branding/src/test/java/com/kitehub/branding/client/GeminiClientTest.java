package com.kitehub.branding.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GeminiClient} (GAP-1135). HTTP is mocked via a stubbed
 * {@link ExchangeFunction} — no live Gemini network.
 *
 * @since GAP-1135
 */
class GeminiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebClient webClientReturning(String jsonBody) {
        ExchangeFunction exchange = request -> Mono.just(
                ClientResponse.create(HttpStatusCode.valueOf(200))
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(jsonBody)
                        .build());
        return WebClient.builder().exchangeFunction(exchange).build();
    }

    @Test
    @DisplayName("generateText (real key) parses the first candidate's text")
    void generateTextParsesCandidate() {
        String body = "{\"candidates\":[{\"content\":{\"role\":\"model\",\"parts\":"
                + "[{\"text\":\"Học giỏi cùng trung tâm ABC\"}]}}]}";
        GeminiClient client = new GeminiClient(webClientReturning(body),
                "real-key-xyz", "gemini-1.5-flash", "gemini-1.5-flash", 30, objectMapper);

        String result = client.generateText("Soạn nội dung cho ABC").block();

        assertThat(result).isEqualTo("Học giỏi cùng trung tâm ABC");
        assertThat(client.getProviderName()).isEqualTo("gemini");
    }

    @Test
    @DisplayName("generateText falls back to mock copy when response has no candidates")
    void generateTextEmptyCandidatesFallback() {
        GeminiClient client = new GeminiClient(webClientReturning("{\"candidates\":[]}"),
                "real-key-xyz", "gemini-1.5-flash", "gemini-1.5-flash", 30, objectMapper);

        String result = client.generateText("anything").block();

        assertThat(result).contains("trung tâm giáo dục");
    }

    @Test
    @DisplayName("MOCK mode (no key) returns sample Vietnamese copy, never hits network")
    void mockModeReturnsSampleCopy() {
        // No WebClient call expected — pass a throwing exchange to prove it.
        WebClient throwing = WebClient.builder()
                .exchangeFunction(req -> Mono.error(new IllegalStateException("should not be called")))
                .build();
        GeminiClient client = new GeminiClient(throwing, "", "gemini-1.5-flash",
                "gemini-1.5-flash", 30, objectMapper);

        assertThat(client.isMockMode()).isTrue();
        assertThat(client.getProviderName()).isEqualTo("gemini-mock");
        assertThat(client.generateText("x").block()).contains("trung tâm giáo dục");
    }

    @Test
    @DisplayName("generateImage returns a deterministic placeholder (Gemini does not do images)")
    void generateImageReturnsPlaceholder() {
        GeminiClient client = new GeminiClient(webClientReturning("{}"),
                "real-key-xyz", "gemini-1.5-flash", "gemini-1.5-flash", 30, objectMapper);

        String url = client.generateImage("prompt", "1200x630").block();

        assertThat(url).contains("placehold.co");
        assertThat(url).contains("1200x630");
    }

    @Test
    @DisplayName("analyzeLogo (mock mode) returns the template default analysis")
    void analyzeLogoMockDefault() {
        GeminiClient client = new GeminiClient(webClientReturning("{}"),
                "", "gemini-1.5-flash", "gemini-1.5-flash", 30, objectMapper);

        var analysis = client.analyzeLogo("https://logo", "ABC").block();

        assertThat(analysis).isNotNull();
        assertThat(analysis.getPrimaryColor()).startsWith("#");
        assertThat(analysis.getTheme()).isEqualTo("MODERN");
    }
}
