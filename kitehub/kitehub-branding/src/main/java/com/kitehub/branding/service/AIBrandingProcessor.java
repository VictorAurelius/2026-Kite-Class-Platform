package com.kitehub.branding.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.client.AIClient;
import com.kitehub.branding.client.OpenAIClient;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.GenerationMode;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.domain.enums.OrgType;
import com.kitehub.branding.dto.BrandingAsset;
import com.kitehub.branding.dto.BrandingJobMessage;
import com.kitehub.branding.service.banner.BannerComposition;
import com.kitehub.branding.service.banner.BannerHtmlComposer;
import com.kitehub.branding.service.banner.BannerRenderer;
import com.kitehub.branding.wizard.dto.BrandColours;
import com.kitehub.branding.wizard.quality.BrandColoursDeriver;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI branding processor — the real generation flow (GAP-1135, ADR-037 Amendment).
 *
 * <p>Replaces the prior MVP mock ({@code asset = logoUrl + simulateProcessing})
 * with a real pipeline:</p>
 * <ol>
 *   <li><b>Copy</b> — {@link AIClient#generateText} (Gemini free-tier via
 *       {@code ResilientAIClient}); guarded by the {@link AIInputCapService}
 *       input-cap (ai-branding-guidelines §2.5).</li>
 *   <li><b>Banner — TEMPLATE</b> (default FREE/BASIC/PREMIUM): compose deterministic
 *       HTML ({@link BannerHtmlComposer}) → rasterise via {@link BannerRenderer}
 *       seam → falls back to logo/placeholder when no Playwright runtime is wired.</li>
 *   <li><b>Banner — FULL_AI</b> (PREMIUM limited + ENTERPRISE unlimited, §2.4 +
 *       SUB-22, GAP-1137): {@link OpenAIClient#generateImage}, gated by the
 *       per-tier {@link FullAiQuotaService} monthly cost quota; on quota-exceeded
 *       / failure / no key → fall back to TEMPLATE. Each attempt emits the
 *       {@code ai.fullai.call} counter tagged by tier + outcome.</li>
 * </ol>
 *
 * <p>Graceful degradation: with no provider key, Gemini/OpenAI clients run in MOCK
 * mode + {@code ResilientAIClient}'s circuit breaker absorbs failures, so the
 * pipeline always finishes via the template/placeholder path (never crashes).</p>
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIBrandingProcessor {

    /** Bound each blocking AI call so a slow/unreachable provider can't stall the worker. */
    private static final Duration AI_CALL_TIMEOUT = Duration.ofSeconds(90);
    // G1 walk 2026-06-12: 1536x1024 = landscape hợp lệ của gpt-image-1 (1792x1024 là size dall-e-3 cũ).
    private static final String DEFAULT_BANNER_SIZE = "1536x1024";

    private final BrandingJobService jobService;
    private final ObjectMapper objectMapper;
    /** Resilient (circuit-breaker) provider — Gemini/Ollama/OpenAI per {@code ai.provider}. */
    private final AIClient aiClient;
    /** Bare OpenAI client for the FULL_AI (ENTERPRISE) image path. */
    private final OpenAIClient openAIClient;
    private final AIInputCapService inputCapService;
    private final BannerHtmlComposer bannerComposer;
    private final BannerRenderer bannerRenderer;
    private final BrandColoursDeriver coloursDeriver;
    /** GAP-1137 — FULL_AI monthly cost quota gate (PREMIUM limited / ENTERPRISE unlimited). */
    private final FullAiQuotaService fullAiQuotaService;
    private final MeterRegistry meterRegistry;

    /**
     * Process a branding job through the real generation pipeline.
     *
     * @param message job message (carries jobId, instance, org, logo, tier, orgType)
     * @throws Exception if a step fails irrecoverably (routes to DLQ via the consumer)
     */
    public void processJob(BrandingJobMessage message) throws Exception {
        UUID jobId = message.getJobId();
        log.info("Processing branding job: {} (real generation, provider={})",
                jobId, aiClient.getProviderName());

        Map<String, String> assets = new HashMap<>();

        try {
            BrandingJob job = jobService.getJob(jobId, message.getInstanceId());

            String tier = blankToNull(message.getTier());
            GenerationMode mode = GenerationMode.forTier(tier);
            String orgType = job != null && job.getOrgType() != null
                    ? job.getOrgType() : blankToNull(message.getOrgType());
            String language = blankTo(message.getLanguage(), "vi");
            String orgName = blankTo(message.getOrganizationName(), "Trung tâm giáo dục");
            String logoUrl = message.getLogoUrl();

            BrandColours colours = deriveColours(job, jobId, orgName);
            List<String> portraits = extractPortraits(job);

            // Step 1 — brand context (20%)
            updateProgress(message, 20, "Phân tích thương hiệu");
            assets.put("logoAnalysis", "Brand context for " + orgName
                    + " (provider=" + aiClient.getProviderName() + ")");

            // Step 2 — copy via Gemini, input-cap guarded (50%)
            updateProgress(message, 50, "Soạn nội dung");
            String copy = generateCopy(tier, orgName, language);
            assets.put("marketingCopy", copy);
            assets.put("heroTitle", deriveHeroTitle(orgName));

            // Step 3 — banner (TEMPLATE default / FULL_AI for ENTERPRISE) (80%)
            updateProgress(message, 80, "Dựng banner");
            BannerResult banner = generateBanner(mode, tier, orgName, copy, logoUrl,
                    portraits, themeIconFor(orgType), colours, message.getInstanceId());

            assets.put("generationMode", banner.mode().name());
            assets.put("hero", banner.imageUrl());
            assets.put("banner", banner.imageUrl());
            assets.put("facebookCover", banner.imageUrl());
            assets.put("ogImage", banner.imageUrl());
            if (banner.html() != null) {
                // Persist the composed HTML for transparency (the real TEMPLATE artifact).
                assets.put("bannerHtml", banner.html());
            }
            if (logoUrl != null && !logoUrl.isBlank()) {
                assets.put("logoLight", logoUrl);
                assets.put("logoDark", logoUrl);
            }
            for (int i = 0; i < portraits.size(); i++) {
                assets.put("portrait" + (i + 1), portraits.get(i));
            }
            assets.put("brandPrimary", colours.primary());
            assets.put("brandAccent", colours.accent());

            // Step 4 — finalize (100%)
            updateProgress(message, 100, "Hoàn tất");
            String assetsJson = objectMapper.writeValueAsString(assets);
            jobService.updateGeneratedAssets(jobId, assetsJson);

            log.info("Job {} completed: mode={}, {} assets ({} portraits)",
                    jobId, banner.mode(), assets.size(), portraits.size());

        } catch (Exception e) {
            log.error("Job {} processing failed", jobId, e);
            throw e;
        }
    }

    // ---- copy ----------------------------------------------------------------

    /**
     * Generate marketing copy via the resilient AI client (Gemini). The input-cap
     * (ai-branding-guidelines §2.5) guards the callsite; on rejection or any
     * provider failure the static Vietnamese template copy is used so generation
     * always finishes.
     */
    private String generateCopy(String tier, String orgName, String language) {
        String prompt = buildCopyPrompt(orgName, language);

        // §2.5 — every AI callsite passes through the per-tier input cap first.
        ResponseEntity<Object> capRejection = inputCapService.checkInputSize(tier, orgName, language, prompt);
        if (capRejection != null) {
            log.warn("Copy generation skipped — input cap exceeded (tier={}) → static template copy", tier);
            return staticCopy(orgName);
        }

        try {
            String copy = aiClient.generateText(prompt).block(AI_CALL_TIMEOUT);
            return (copy == null || copy.isBlank()) ? staticCopy(orgName) : copy.trim();
        } catch (Exception e) {
            log.warn("Copy generation failed → static template copy: {}", e.getMessage());
            return staticCopy(orgName);
        }
    }

    // ---- banner --------------------------------------------------------------

    /**
     * Generate the banner. FULL_AI (ENTERPRISE) attempts GPT image-gen and falls
     * back to TEMPLATE on failure / no key. TEMPLATE composes deterministic HTML
     * and rasterises via the {@link BannerRenderer} seam (currently a stub that
     * falls back to logo/placeholder).
     */
    private BannerResult generateBanner(GenerationMode mode, String tier, String orgName,
                                        String copy, String logoUrl, List<String> portraits,
                                        String themeIcon, BrandColours colours, UUID instanceId) {
        if (mode == GenerationMode.FULL_AI) {
            // GAP-1137 — PREMIUM monthly cost quota gate (ENTERPRISE unlimited).
            if (!fullAiQuotaService.canUseFullAi(instanceId, tier)) {
                recordFullAiCall(tier, "quota_exceeded");
                log.info("FULL_AI monthly quota exhausted for instance {} (tier={}) → TEMPLATE fallback",
                        instanceId, tier);
            } else {
                String imagePrompt = buildImagePrompt(orgName, copy, colours);
                // §2.5 — cap the image prompt too (Enterprise cap may be -1 = unlimited).
                ResponseEntity<Object> capRejection = inputCapService.checkInputSize(tier, imagePrompt);
                if (capRejection == null) {
                    try {
                        String url = openAIClient.generateImage(imagePrompt, DEFAULT_BANNER_SIZE)
                                .block(AI_CALL_TIMEOUT);
                        if (url != null && !url.isBlank()) {
                            fullAiQuotaService.recordFullAiUsage(instanceId, tier);
                            recordFullAiCall(tier, "success");
                            log.info("FULL_AI banner generated for instance {}", instanceId);
                            return new BannerResult(GenerationMode.FULL_AI, url, null);
                        }
                        recordFullAiCall(tier, "empty_result");
                    } catch (Exception e) {
                        recordFullAiCall(tier, "error");
                        log.warn("FULL_AI banner failed → TEMPLATE fallback: {}", e.getMessage());
                    }
                } else {
                    recordFullAiCall(tier, "input_cap");
                    log.warn("FULL_AI image prompt exceeded input cap → TEMPLATE fallback");
                }
            }
        }

        // TEMPLATE path (default, or FULL_AI fallback).
        BannerComposition composition = bannerComposer.compose(
                orgName, copy, logoUrl, portraits, themeIcon, colours);
        String rendered = null;
        try {
            rendered = bannerRenderer.render(composition, instanceId);
        } catch (Exception e) {
            log.warn("Banner render seam threw → placeholder fallback: {}", e.getMessage());
        }
        String imageUrl = rendered != null ? rendered : templatePlaceholder(logoUrl, colours);
        return new BannerResult(GenerationMode.TEMPLATE, imageUrl, composition.html());
    }

    // ---- helpers -------------------------------------------------------------

    /** Emit the GAP-1137 FULL_AI cost counter for one attempt, tagged by tier + outcome. */
    private void recordFullAiCall(String tier, String outcome) {
        meterRegistry.counter("ai.fullai.call",
                "tier", tier == null ? "unknown" : tier,
                "outcome", outcome).increment();
    }

    private BrandColours deriveColours(BrandingJob job, UUID jobId, String orgName) {
        BrandingJob source = job;
        if (source == null) {
            // Transient job carries just enough for the deterministic palette hash.
            source = new BrandingJob();
            source.setId(jobId);
            source.setOrganizationName(orgName);
        }
        return coloursDeriver.derive(source);
    }

    /**
     * Extract uploaded PORTRAIT asset URLs (GAP-1134) from the job's persisted
     * BrandingAsset[] (if present). A non-array shape (legacy theme-metadata or a
     * prior generation Map) yields no portraits.
     */
    private List<String> extractPortraits(BrandingJob job) {
        List<String> portraits = new ArrayList<>();
        if (job == null) {
            return portraits;
        }
        String json = job.getAssetsGenerated();
        if (json == null || json.isBlank() || !json.trim().startsWith("[")) {
            return portraits;
        }
        try {
            List<BrandingAsset> existing =
                    objectMapper.readValue(json, new TypeReference<List<BrandingAsset>>() {});
            for (BrandingAsset asset : existing) {
                if (asset.getType() != null && "PORTRAIT".equalsIgnoreCase(asset.getType())
                        && asset.getUrl() != null) {
                    portraits.add(asset.getUrl());
                }
            }
        } catch (Exception e) {
            log.debug("Could not parse uploaded assets for portraits: {}", e.getMessage());
        }
        return portraits;
    }

    private String themeIconFor(String orgType) {
        OrgType type = OrgType.fromNullable(orgType);
        if (type == null) {
            return "📚";
        }
        return switch (type) {
            case SOLO_TEACHER -> "👩‍🏫";
            case SMALL_CENTER -> "📖";
            case LARGE_CENTER -> "🏫";
        };
    }

    private String buildCopyPrompt(String orgName, String language) {
        return String.format("""
                Viết nội dung marketing ngắn gọn (%s) cho trung tâm giáo dục "%s".
                Yêu cầu: 1 câu slogan hấp dẫn (≤ 60 ký tự) + 1 câu mô tả giá trị (≤ 150 ký tự).
                Văn phong chuyên nghiệp, thân thiện. Trả về văn bản thuần (không Markdown),
                KHÔNG bịa số liệu / chứng nhận / cảm nhận học viên giả.
                """, "vi".equalsIgnoreCase(language) ? "tiếng Việt" : language, orgName);
    }

    private String buildImagePrompt(String orgName, String copy, BrandColours colours) {
        return String.format("""
                Professional education-centre hero banner for "%s". Brand colours %s / %s.
                Warm, inviting, modern learning environment. No garbled text overlay.
                Tagline context: %s
                """, orgName, colours.primary(), colours.accent(),
                copy == null ? "" : copy.substring(0, Math.min(copy.length(), 120)));
    }

    private String deriveHeroTitle(String orgName) {
        return orgName == null || orgName.isBlank() ? "Trung tâm giáo dục" : orgName.trim();
    }

    private String staticCopy(String orgName) {
        return "Chào mừng đến với " + (orgName == null || orgName.isBlank()
                ? "trung tâm của chúng tôi" : orgName.trim())
                + " — chương trình học chất lượng cao cùng đội ngũ giảng viên tận tâm.";
    }

    /** Deterministic template placeholder when no rasteriser is wired (honest stub). */
    private String templatePlaceholder(String logoUrl, BrandColours colours) {
        if (logoUrl != null && !logoUrl.isBlank()) {
            return logoUrl;
        }
        String hex = colours.primary().replace("#", "");
        return "https://placehold.co/1200x630/" + hex + "/white?text=Banner";
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Update job progress.
     *
     * @param message  job message
     * @param progress progress percentage
     * @param step     current step description
     */
    private void updateProgress(BrandingJobMessage message, int progress, String step) {
        jobService.updateJobProgress(message.getJobId(), JobStatus.PROCESSING, progress, step);
        log.debug("Job {} progress: {}% - {}", message.getJobId(), progress, step);
    }

    /** Result of banner generation — chosen mode + final image URL + composed HTML (TEMPLATE). */
    private record BannerResult(GenerationMode mode, String imageUrl, String html) {
    }
}
