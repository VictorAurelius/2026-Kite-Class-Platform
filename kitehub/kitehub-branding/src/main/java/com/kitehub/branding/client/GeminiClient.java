package com.kitehub.branding.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.dto.LogoAnalysis;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Strategy implementation of {@link AIClient} for Google Gemini's free-tier
 * Generative Language REST API (ADR-037 Amendment 2026-06-10).
 *
 * <p>Selected by {@code ai.provider=gemini} via
 * {@link com.kitehub.branding.config.AIProviderConfig#aiClient}. Gemini is the
 * PINNED free-tier provider for text/HTML copy generation; it also drives the
 * TEMPLATE banner copy (composed into HTML by {@code BannerHtmlComposer}).
 * Banner <em>image</em> generation is NOT a Gemini concern — {@link #generateImage}
 * returns a deterministic placeholder (the TEMPLATE path renders the real banner;
 * FULL_AI uses {@link OpenAIClient}).</p>
 *
 * <p><b>Graceful no-key behaviour:</b> when {@code GEMINI_API_KEY} is absent /
 * blank / a {@code mock|placeholder} sentinel, the client runs in MOCK mode and
 * returns a sensible Vietnamese default copy + template-default analysis instead
 * of crashing. Combined with {@link ResilientAIClient}'s circuit breaker, an
 * unreachable Gemini never cascades a 5xx into the generation pipeline.</p>
 *
 * @since GAP-1117 (Gemini free-tier provider)
 */
@Slf4j
public class GeminiClient implements AIClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String textModel;
    private final String visionModel;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;

    /**
     * Production constructor — builds the WebClient from the base URL.
     *
     * @param baseUrl        Gemini API base URL (e.g. https://generativelanguage.googleapis.com/v1beta)
     * @param apiKey         {@code GEMINI_API_KEY} (blank/mock → MOCK mode)
     * @param textModel      text model id (e.g. gemini-1.5-flash)
     * @param visionModel    vision/multimodal model id used for logo analysis
     * @param timeoutSeconds per-request timeout
     * @param objectMapper   Jackson mapper
     */
    public GeminiClient(String baseUrl, String apiKey, String textModel, String visionModel,
                        int timeoutSeconds, ObjectMapper objectMapper) {
        this(buildWebClient(baseUrl, timeoutSeconds), apiKey, textModel, visionModel,
                timeoutSeconds, objectMapper);
    }

    /**
     * Explicit-WebClient constructor — used by tests to inject a stubbed
     * {@link WebClient} (no live network).
     */
    GeminiClient(WebClient webClient, String apiKey, String textModel, String visionModel,
                 int timeoutSeconds, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.apiKey = apiKey;
        this.textModel = textModel;
        this.visionModel = visionModel;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
        log.info("GeminiClient initialized: textModel={}, visionModel={}, mock={}",
                textModel, visionModel, isMockMode());
    }

    private static WebClient buildWebClient(String baseUrl, int timeoutSeconds) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds + 5L));
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /** MOCK mode when no real key is configured — graceful no-key fallback. */
    boolean isMockMode() {
        return apiKey == null || apiKey.isBlank()
                || apiKey.startsWith("mock") || apiKey.startsWith("placeholder")
                || apiKey.equalsIgnoreCase("changeme");
    }

    @Override
    public Mono<LogoAnalysis> analyzeLogo(String imageUrl, String organizationName) {
        if (isMockMode()) {
            log.info("[Gemini][MOCK] analyzeLogo → template default for org={}", organizationName);
            return Mono.just(templateDefaultAnalysis());
        }

        // Text-only logo analysis (image base64 inline_data deferred). Ask the
        // multimodal model for a JSON brand-identity object derived from the org name.
        String prompt = buildLogoAnalysisPrompt(organizationName);
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(visionModel))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .map(this::parseGenerateContentResponse)
                .map(this::parseAnalysisOrDefault)
                .doOnSuccess(a -> log.info("[Gemini] logo analysis theme={}", a.getTheme()))
                .onErrorResume(e -> {
                    log.error("[Gemini] analyzeLogo failed → template default: {}", e.getMessage());
                    return Mono.just(templateDefaultAnalysis());
                });
    }

    @Override
    public Mono<String> generateImage(String prompt, String size) {
        // Gemini free-tier text model does not generate images. The TEMPLATE path
        // (HTML compose → Playwright) renders the banner; FULL_AI uses OpenAI.
        log.info("[Gemini] generateImage not supported → placeholder (size={})", size);
        String safeSize = size == null || size.isBlank() ? "1792x1024" : size;
        return Mono.just("https://placehold.co/" + safeSize + "/2563EB/white?text=Template");
    }

    @Override
    public Mono<String> generateText(String prompt) {
        if (isMockMode()) {
            log.info("[Gemini][MOCK] returning sample Vietnamese copy ({} chars prompt)",
                    prompt == null ? 0 : prompt.length());
            return Mono.just(mockCopy());
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(textModel))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .map(this::parseGenerateContentResponse)
                .doOnSuccess(text -> log.info("[Gemini] text generated: {} chars",
                        text == null ? 0 : text.length()))
                .doOnError(e -> log.error("[Gemini] text generation failed: {}", e.getMessage()));
    }

    @Override
    public String getProviderName() {
        return isMockMode() ? "gemini-mock" : "gemini";
    }

    /**
     * Extract the first candidate's concatenated text parts from a Gemini
     * {@code generateContent} response.
     */
    @SuppressWarnings("unchecked")
    private String parseGenerateContentResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                log.warn("[Gemini] response had no candidates → mock fallback");
                return mockCopy();
            }
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> part : parts) {
                Object text = part.get("text");
                if (text != null) {
                    sb.append(text);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[Gemini] failed to parse generateContent response → mock fallback", e);
            return mockCopy();
        }
    }

    private LogoAnalysis parseAnalysisOrDefault(String content) {
        try {
            String cleaned = content == null ? "" : content.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            LogoAnalysis analysis = objectMapper.readValue(cleaned.trim(), LogoAnalysis.class);
            analysis.setRawAnalysis(content);
            return analysis;
        } catch (Exception e) {
            log.warn("[Gemini] could not parse analysis JSON → template default: {}", e.getMessage());
            return templateDefaultAnalysis();
        }
    }

    private String buildLogoAnalysisPrompt(String organizationName) {
        return String.format("""
                Analyze the brand for "%s" (a Vietnamese education centre). Return ONLY valid
                JSON with these exact fields:
                {"primaryColor":"#RRGGBB","secondaryColor":"#RRGGBB","accentColor":"#RRGGBB",
                 "theme":"MODERN|CLASSIC|PLAYFUL|MINIMAL","typography":"...",
                 "targetAudience":"...","brandPersonality":["...","..."]}
                """, organizationName);
    }

    private LogoAnalysis templateDefaultAnalysis() {
        return LogoAnalysis.builder()
                .primaryColor("#2563EB")
                .secondaryColor("#64748B")
                .accentColor("#F59E0B")
                .theme("MODERN")
                .typography("Sans-serif")
                .targetAudience("Students and parents seeking quality education")
                .brandPersonality(List.of("Trustworthy", "Innovative", "Approachable"))
                .rawAnalysis("Gemini template default")
                .build();
    }

    private String mockCopy() {
        return "Chào mừng đến với trung tâm giáo dục hàng đầu. Chúng tôi mang đến "
                + "chương trình học chất lượng cao cùng đội ngũ giảng viên tận tâm, "
                + "giàu kinh nghiệm — chắp cánh cho hành trình học tập của bạn.";
    }
}
