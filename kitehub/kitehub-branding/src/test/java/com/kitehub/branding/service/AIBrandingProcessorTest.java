package com.kitehub.branding.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.client.AIClient;
import com.kitehub.branding.client.OpenAIClient;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.dto.BrandingJobMessage;
import com.kitehub.branding.service.banner.BannerComposition;
import com.kitehub.branding.service.banner.BannerHtmlComposer;
import com.kitehub.branding.service.banner.BannerRenderer;
import com.kitehub.branding.wizard.dto.BrandColours;
import com.kitehub.branding.wizard.quality.BrandColoursDeriver;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AIBrandingProcessor} real generation flow (GAP-1117):
 * TEMPLATE/FULL_AI routing, FULL_AI→TEMPLATE fallback, input-cap guard, portraits.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AIBrandingProcessorTest {

    @Mock private BrandingJobService jobService;
    @Mock private AIClient aiClient;
    @Mock private OpenAIClient openAIClient;
    @Mock private AIInputCapService inputCapService;
    @Mock private BannerHtmlComposer bannerComposer;
    @Mock private BannerRenderer bannerRenderer;
    @Mock private BrandColoursDeriver coloursDeriver;
    @Mock private FullAiQuotaService fullAiQuotaService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private AIBrandingProcessor processor;

    private UUID jobId;
    private UUID instanceId;
    private BrandingJob job;

    private static final BrandColours COLOURS = new BrandColours(
            "#1a73e8", "#fbbc04", "#10B981", "#1f2937", "#ffffff",
            BrandColours.Source.TEMPLATE);

    @BeforeEach
    void setUp() {
        processor = new AIBrandingProcessor(jobService, objectMapper, aiClient, openAIClient,
                inputCapService, bannerComposer, bannerRenderer, coloursDeriver,
                fullAiQuotaService, meterRegistry);
        // GAP-1119 — FULL_AI-eligible tiers pass the quota gate by default; the
        // quota-exhausted path is exercised explicitly below.
        when(fullAiQuotaService.canUseFullAi(any(), any())).thenReturn(true);

        jobId = UUID.randomUUID();
        instanceId = UUID.randomUUID();
        job = new BrandingJob();
        job.setId(jobId);
        job.setInstanceId(instanceId);
        job.setOrganizationName("Trung tâm ABC");
        job.setOrgType("SMALL_CENTER");

        when(jobService.getJob(jobId, instanceId)).thenReturn(job);
        when(aiClient.getProviderName()).thenReturn("gemini-mock");
        when(aiClient.generateText(anyString())).thenReturn(Mono.just("AI copy slogan."));
        when(coloursDeriver.derive(any())).thenReturn(COLOURS);
        when(bannerComposer.compose(any(), any(), any(), any(), any(), any()))
                .thenReturn(new BannerComposition("<html>banner</html>", 1200, 630));
        when(bannerRenderer.render(any(), any())).thenReturn(null); // stub seam → fallback
    }

    private BrandingJobMessage message(String tier) {
        BrandingJobMessage m = new BrandingJobMessage();
        m.setJobId(jobId);
        m.setInstanceId(instanceId);
        m.setOrganizationName("Trung tâm ABC");
        m.setLanguage("vi");
        m.setLogoUrl("https://cdn/instances/x/logo.png");
        m.setTier(tier);
        return m;
    }

    private Map<String, String> captureAssets() throws Exception {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jobService).updateGeneratedAssets(eq(jobId), captor.capture());
        return objectMapper.readValue(captor.getValue(), new TypeReference<Map<String, String>>() {});
    }

    @Test
    @DisplayName("FREE tier → TEMPLATE: composes HTML, no FULL_AI image call")
    void freeTierUsesTemplate() throws Exception {
        processor.processJob(message("FREE"));

        verify(bannerComposer).compose(any(), any(), any(), any(), any(), any());
        verify(openAIClient, never()).generateImage(any(), any());
        verify(aiClient).generateText(anyString());

        Map<String, String> assets = captureAssets();
        assertThat(assets.get("generationMode")).isEqualTo("TEMPLATE");
        assertThat(assets.get("marketingCopy")).isEqualTo("AI copy slogan.");
        assertThat(assets).containsKey("bannerHtml");
        // Renderer stub → banner falls back to the uploaded logo URL.
        assertThat(assets.get("hero")).isEqualTo("https://cdn/instances/x/logo.png");
    }

    @Test
    @DisplayName("null tier → TEMPLATE (default)")
    void nullTierDefaultsToTemplate() throws Exception {
        processor.processJob(message(null));

        Map<String, String> assets = captureAssets();
        assertThat(assets.get("generationMode")).isEqualTo("TEMPLATE");
        verify(openAIClient, never()).generateImage(any(), any());
    }

    @Test
    @DisplayName("ENTERPRISE tier → FULL_AI: calls OpenAI image-gen")
    void enterpriseTierUsesFullAi() throws Exception {
        when(openAIClient.generateImage(anyString(), anyString()))
                .thenReturn(Mono.just("https://openai/banner.png"));

        processor.processJob(message("ENTERPRISE"));

        verify(openAIClient).generateImage(anyString(), anyString());
        Map<String, String> assets = captureAssets();
        assertThat(assets.get("generationMode")).isEqualTo("FULL_AI");
        assertThat(assets.get("hero")).isEqualTo("https://openai/banner.png");
    }

    @Test
    @DisplayName("ENTERPRISE FULL_AI failure → falls back to TEMPLATE")
    void enterpriseFullAiFailureFallsBackToTemplate() throws Exception {
        when(openAIClient.generateImage(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("OpenAI down")));

        processor.processJob(message("ENTERPRISE"));

        verify(openAIClient).generateImage(anyString(), anyString());
        verify(bannerComposer).compose(any(), any(), any(), any(), any(), any());
        Map<String, String> assets = captureAssets();
        assertThat(assets.get("generationMode")).isEqualTo("TEMPLATE");
    }

    @Test
    @DisplayName("GAP-1119: PREMIUM tier → FULL_AI when monthly quota available")
    void premiumTierUsesFullAiWhenQuotaAvailable() throws Exception {
        when(openAIClient.generateImage(anyString(), anyString()))
                .thenReturn(Mono.just("https://openai/premium-banner.png"));

        processor.processJob(message("PREMIUM"));

        verify(openAIClient).generateImage(anyString(), anyString());
        verify(fullAiQuotaService).recordFullAiUsage(instanceId, "PREMIUM");
        Map<String, String> assets = captureAssets();
        assertThat(assets.get("generationMode")).isEqualTo("FULL_AI");
        assertThat(assets.get("hero")).isEqualTo("https://openai/premium-banner.png");
    }

    @Test
    @DisplayName("GAP-1119: PREMIUM quota exhausted → TEMPLATE fallback, no image call")
    void premiumQuotaExhaustedFallsBackToTemplate() throws Exception {
        when(fullAiQuotaService.canUseFullAi(instanceId, "PREMIUM")).thenReturn(false);

        processor.processJob(message("PREMIUM"));

        verify(openAIClient, never()).generateImage(any(), any());
        verify(fullAiQuotaService, never()).recordFullAiUsage(any(), any());
        verify(bannerComposer).compose(any(), any(), any(), any(), any(), any());
        Map<String, String> assets = captureAssets();
        assertThat(assets.get("generationMode")).isEqualTo("TEMPLATE");
    }

    @Test
    @DisplayName("GAP-1119: BASIC tier not FULL_AI-eligible → TEMPLATE (quota gate not consulted)")
    void basicTierNotEligibleUsesTemplate() throws Exception {
        processor.processJob(message("BASIC"));

        verify(openAIClient, never()).generateImage(any(), any());
        Map<String, String> assets = captureAssets();
        assertThat(assets.get("generationMode")).isEqualTo("TEMPLATE");
    }

    @Test
    @DisplayName("input-cap rejection → static copy, AI text not called")
    void inputCapRejectionUsesStaticCopy() throws Exception {
        // The copy callsite passes (tier, orgName, language, prompt) = 3 varargs.
        when(inputCapService.checkInputSize(any(), any(), any(), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "AI_INPUT_TOO_LONG")));

        processor.processJob(message("FREE"));

        verify(aiClient, never()).generateText(anyString());
        Map<String, String> assets = captureAssets();
        assertThat(assets.get("marketingCopy")).contains("Chào mừng");
    }

    @Test
    @DisplayName("uploaded PORTRAIT assets feed the banner composer (GAP-1116 → GAP-1117)")
    void portraitsFeedBannerComposer() throws Exception {
        job.setAssetsGenerated("[{\"type\":\"PORTRAIT\",\"url\":\"https://cdn/p1.png\"},"
                + "{\"type\":\"PORTRAIT\",\"url\":\"https://cdn/p2.png\"},"
                + "{\"type\":\"LOGO\",\"url\":\"https://cdn/logo.png\"}]");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> portraitsCaptor = ArgumentCaptor.forClass(List.class);

        processor.processJob(message("FREE"));

        verify(bannerComposer).compose(any(), any(), any(), portraitsCaptor.capture(), any(), any());
        assertThat(portraitsCaptor.getValue()).containsExactly("https://cdn/p1.png", "https://cdn/p2.png");

        Map<String, String> assets = captureAssets();
        assertThat(assets.get("portrait1")).isEqualTo("https://cdn/p1.png");
        assertThat(assets.get("portrait2")).isEqualTo("https://cdn/p2.png");
    }

    @Test
    @DisplayName("completes (no crash) when job lookup returns null")
    void completesWhenJobNull() throws Exception {
        when(jobService.getJob(jobId, instanceId)).thenReturn(null);

        processor.processJob(message("FREE"));

        Map<String, String> assets = captureAssets();
        assertThat(assets.get("generationMode")).isEqualTo("TEMPLATE");
    }
}
