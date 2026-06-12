package com.kitehub.branding.wizard;

import com.kitehub.branding.client.ResilientAIClient;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.GenerationMode;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.service.BrandingJobService;
import com.kitehub.branding.service.FullAiQuotaService;
import com.kitehub.branding.service.S3StorageService;
import com.kitehub.branding.service.banner.BannerComposition;
import com.kitehub.branding.service.banner.BannerHtmlComposer;
import com.kitehub.branding.service.banner.BannerRenderer;
import com.kitehub.branding.wizard.dto.ApproveDeployRequest;
import com.kitehub.branding.wizard.dto.BrandColours;
import com.kitehub.branding.wizard.dto.BrandingJobResponse;
import com.kitehub.branding.wizard.dto.CreateWizardJobRequest;
import com.kitehub.branding.wizard.dto.PreviewBannerRequest;
import com.kitehub.branding.wizard.quality.BrandColoursDeriver;
import com.kitehub.branding.wizard.quality.QualityScoreAggregator;
import com.kitehub.branding.wizard.quality.dto.QualityScoreResponse;
import com.kitehub.branding.wizard.service.MockProvisioningService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * v1 jobs/{id} controller — Wave 34 Bucket B.
 *
 * <p>Serves {@code GET /api/v1/branding/jobs/{jobId}} per
 * {@code documents/01-business/kitehub/ai-branding/api-contract.md}.
 * Returns {@link BrandingJobResponse} with the {@code brandColors} field
 * (sub-GAP-272k).</p>
 *
 * <p>The legacy controller {@code BrandingJobController} at
 * {@code /api/platform/branding/jobs/{id}} returns the entity directly and
 * is left untouched — Bucket B owns the v1 path and the new response DTO.</p>
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/branding/jobs")
@RequiredArgsConstructor
@Tag(name = "Branding Jobs v1",
        description = "v1 job lookup with brandColors (sub-GAP-272k)")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
        extraTags = {"slo", "tier-b", "controller", "branding-job-v1"})
public class BrandingJobV1Controller {

    private final BrandingJobRepository jobRepository;
    private final BrandColoursDeriver coloursDeriver;
    private final BrandingJobService brandingJobService;
    private final MockProvisioningService mockProvisioningService;
    /** GAP-1217 — quality gate aggregator (GAP-272c, Wave 34 #906); enforce ≥70 before deploy. */
    private final QualityScoreAggregator qualityScoreAggregator;
    /** GAP-1141 — Step 7 live banner preview (compose → sidecar render, no Gemini/DB/quota). */
    private final BannerHtmlComposer bannerHtmlComposer;
    private final BannerRenderer bannerRenderer;
    /** GAP-1147 — FULL_AI preview tier-gate + monthly quota meter (PREMIUM cap / ENTERPRISE ∞). */
    private final FullAiQuotaService fullAiQuotaService;
    /**
     * GAP-1135 / GAP-1147 (branding-100 Bucket E) — real FULL_AI banner image-gen.
     * When FULL_AI is granted the preview now calls {@code generateImage} (GPT image-gen
     * via the {@link ResilientAIClient} circuit breaker) instead of re-rendering the
     * TEMPLATE composer, so the FULL_AI banner genuinely differs from the template output.
     */
    private final ResilientAIClient resilientAiClient;

    /**
     * GAP-1218: FULL_AI image-gen wiring flag. Until the real GPT-5.5 banner
     * generator lands (GAP-1135, branding-100 Bucket E), FULL_AI requests resolve
     * to TEMPLATE with {@code fallbackReason=NOT_AVAILABLE} and the monthly quota
     * is NOT consumed — charging quota for template-identical output is a
     * consumer-trust violation (Luật Quảng cáo VN). Bucket E flips this to
     * {@code true} when wiring the generator.
     */
    @Value("${branding.full-ai.image-gen-enabled:false}")
    private boolean fullAiImageGenEnabled;
    /** GAP-1146b — inline portrait/logo as data: URIs so the in-container Playwright can render them. */
    private final S3StorageService s3StorageService;

    /** Safe fallback palette when the preview request carries no (or invalid) colours. */
    private static final BrandColours DEFAULT_PREVIEW_COLOURS = new BrandColours(
            "#1E40AF", "#F59E0B", "#F59E0B", "#0F172A", "#FFFFFF", BrandColours.Source.TEMPLATE);

    /** Read access — owner + staff personas (mirrors getJob). */
    private static final String READ_AUTHZ =
            "hasAnyRole('OWNER','MANAGER','TEACHER','ACCOUNTANT','PLATFORM_ADMIN','ADMIN','STAFF')";

    /** Write access — owner-tier only per GAP-562 (mirrors BrandingWizardController). */
    private static final String WRITE_AUTHZ =
            "hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')";

    /**
     * Create a wizard branding job (Phase 1 MOCK — GAP-1021). Called by the FE
     * when it enters Step 6 so {@code wizardState.jobId} becomes non-empty and the
     * preview + deploy-stream hooks enable. Persists a real {@link BrandingJob}
     * (status {@code QUEUED}) tied to the caller's REAL instance (JWT tenant claim)
     * — NO heavy AI pipeline enqueue (see {@link BrandingJobService#createWizardJob}).
     *
     * @param req wizard selections (organizationName/slug drives preview palette)
     * @return {@code 201} with {@link BrandingJobResponse} (jobId + brandColors)
     */
    @PostMapping
    @PreAuthorize(WRITE_AUTHZ)
    public ResponseEntity<?> createJob(@RequestBody(required = false) CreateWizardJobRequest req) {
        CreateWizardJobRequest body = req == null
                ? new CreateWizardJobRequest(null, null, null, null, null, null, null, null, null, null)
                : req;
        // GAP-1021 runtime fix: bind the job to the caller's REAL instance (JWT tenant
        // claim = instance id). A synthetic random UUID violated fk_branding_job_instance → 500.
        String tenantId = MDC.get("tenantId");
        UUID instanceId = null;
        if (tenantId != null && !tenantId.isBlank()) {
            try {
                instanceId = UUID.fromString(tenantId.trim());
            } catch (IllegalArgumentException ignored) {
                // invalid tenant claim → 400 below
            }
        }
        if (instanceId == null) {
            log.warn("Wizard job create rejected — missing/invalid tenant context: {}", tenantId);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "TENANT_CONTEXT_REQUIRED",
                    "message", "Không xác định được trung tâm từ phiên đăng nhập"));
        }
        String orgName = firstNonBlank(body.organizationName(), body.slug(), "Trung tâm mới");
        // GAP-1133: carry the wizard user-type axis (orgType) onto the job. Parsed
        // tolerantly so a stray value never 500s; persisted as the canonical enum name.
        com.kitehub.branding.domain.enums.OrgType orgType =
                com.kitehub.branding.domain.enums.OrgType.fromNullable(body.orgType());
        BrandingJob job = brandingJobService.createWizardJob(
                instanceId, orgName, body.language(), body.logoUrl(),
                orgType == null ? null : orgType.name(),
                body.tone(), body.templateId()); // GAP-1146 — tone/template drive palette
        BrandColours colours = coloursDeriver.derive(job);
        log.info("Wizard job created: {} status={}", job.getId(), job.getStatus());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BrandingJobResponse.from(job, colours, tenantId));
    }

    /**
     * Compose + rasterise a live TEMPLATE-mode banner preview (GAP-1141). Called
     * by Step 7 to show the owner the real banner WebP before deploy. Stateless:
     * NO Gemini call, NO DB write, NO FULL_AI quota consumed — the FE passes its
     * already-computed palette + copy and the backend just composes + renders.
     *
     * <p>By default preview is {@code TEMPLATE} (free, never burns quota). GAP-1147
     * adds an opt-in {@code mode:"FULL_AI"} path for PREMIUM/ENTERPRISE: it is
     * tier-gated ({@link GenerationMode#forTier}) + metered by
     * {@link FullAiQuotaService}. When the caller is ineligible (FREE/BASIC) or the
     * PREMIUM monthly quota is exhausted, the response falls back to {@code TEMPLATE}
     * with a {@code fallbackReason} — the gate is enforced SERVER-SIDE so a tampered
     * FE can never bypass it. Phase 1 generation is the mock TEMPLATE composer, so a
     * FULL_AI banner renders the same pixels today; the quota + mode contract is real
     * and ready for GAP-1135 real image-gen.</p>
     *
     * @param req  preview inputs (orgName + copy + logo + portraits + icon + palette + mode)
     * @param tier subscription tier (gateway-injected {@code X-Subscription-Tier}; FREE default)
     * @return {@code 200} with {@code {bannerUrl, mode, fallbackReason?}}
     */
    @PostMapping("/preview-banner")
    @PreAuthorize(WRITE_AUTHZ)
    public ResponseEntity<?> previewBanner(
            @RequestBody(required = false) PreviewBannerRequest req,
            @RequestHeader(value = "X-Subscription-Tier", required = false, defaultValue = "FREE")
            String tier) {
        PreviewBannerRequest body = req == null
                ? new PreviewBannerRequest(null, null, null, null, null, null, null)
                : req;
        BrandColours colours = body.colours() != null ? body.colours() : DEFAULT_PREVIEW_COLOURS;
        // Ephemeral object-key namespace — preview artifacts are throwaway, not tied
        // to a persisted job. Prefer the real tenant claim when present.
        UUID instanceId = resolveTenantOrEphemeral();

        // GAP-1147: resolve the effective mode SERVER-SIDE before rendering so the
        // quota is only consumed when FULL_AI is genuinely granted.
        String resolvedMode = "TEMPLATE";
        String fallbackReason = null;
        // GAP-1135: when FULL_AI is granted, this holds the real image-gen banner URL
        // (different pixels than the TEMPLATE composer). Null → fall back to TEMPLATE render.
        String fullAiBannerUrl = null;
        if ("FULL_AI".equalsIgnoreCase(body.mode())) {
            if (GenerationMode.forTier(tier) != GenerationMode.FULL_AI) {
                fallbackReason = "TIER_NOT_ELIGIBLE"; // FREE/BASIC → TEMPLATE
            } else if (!fullAiImageGenEnabled || isAiMockMode()) {
                // GAP-1218 + GAP-1135: image-gen chưa wire (flag off) HOẶC provider chạy
                // mock-mode (chưa có API key thật) → KHÔNG trừ lượt, label thật. Điều kiện
                // FULL_AI granted = flag bật && có key thật (!mockMode).
                fallbackReason = "NOT_AVAILABLE";
            } else if (!fullAiQuotaService.canUseFullAi(instanceId, tier)) {
                fallbackReason = "QUOTA_EXHAUSTED"; // PREMIUM monthly cap spent → TEMPLATE
            } else {
                // GAP-1135: gọi image-gen THẬT (GPT image-gen qua ResilientAIClient circuit
                // breaker). Prompt compose từ org/copy/portrait/palette per ai-branding-
                // guidelines §2.3 (backend composes the fixed prompt; user never writes it).
                String aiUrl = generateFullAiBanner(body, colours, instanceId);
                if (aiUrl == null || aiUrl.isBlank()) {
                    // Generation failed → fall back to TEMPLATE WITHOUT charging quota
                    // (consumer-trust: no charge for output the user can't get).
                    fallbackReason = "GENERATION_FAILED";
                } else {
                    fullAiQuotaService.recordFullAiUsage(instanceId, tier);
                    resolvedMode = "FULL_AI";
                    fullAiBannerUrl = aiUrl;
                }
            }
        }

        String bannerUrl;
        if ("FULL_AI".equals(resolvedMode) && fullAiBannerUrl != null) {
            // FULL_AI granted + rendered → surface the AI image directly (skip TEMPLATE compose).
            bannerUrl = fullAiBannerUrl;
        } else {
            // GAP-1146b: inline portrait + logo as data: URIs. The browser-presigned URLs
            // point at the host port (localhost:9100), unreachable from the in-container
            // Playwright renderer → the portrait silently dropped out of the banner. Inlining
            // the bytes (fetched via the internal S3 client) makes the render host-agnostic.
            String inlinedLogo = s3StorageService.inlineImageDataUri(body.logoUrl());
            List<String> inlinedPortraits = body.portraitUrls() == null
                    ? null
                    : body.portraitUrls().stream().map(s3StorageService::inlineImageDataUri).toList();
            BannerComposition composition = bannerHtmlComposer.compose(
                    body.organizationName(), body.copy(), inlinedLogo,
                    inlinedPortraits, body.themeIcon(), colours);
            bannerUrl = bannerRenderer.render(composition, instanceId);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("bannerUrl", bannerUrl); // nullable — FE falls back to logo/placeholder
        out.put("mode", resolvedMode);
        if (fallbackReason != null) {
            out.put("fallbackReason", fallbackReason);
        }
        log.debug("Preview banner composed for '{}' (mode={}, fallback={}, rendered={})",
                body.organizationName(), resolvedMode, fallbackReason, bannerUrl != null);
        return ResponseEntity.ok(out);
    }

    /**
     * GAP-1135: whether the AI provider is running in mock mode (no real API key).
     * The {@link ResilientAIClient} provider name wraps the delegate, e.g.
     * {@code "resilient(openai-mock)"} when {@code OPENAI_API_KEY} is a placeholder.
     * In mock mode FULL_AI must resolve to {@code NOT_AVAILABLE} (no real output, no quota
     * charge) even when the {@code image-gen-enabled} flag is on — the flag enables the
     * wiring, the real key enables genuine generation.
     */
    private boolean isAiMockMode() {
        String provider = resilientAiClient.getProviderName();
        return provider == null || provider.contains("mock");
    }

    /**
     * GAP-1135: compose the fixed FULL_AI banner prompt from the wizard selections and
     * call the real image generator (GPT image-gen via {@link ResilientAIClient} — circuit
     * breaker falls back to a placeholder URL if the provider errors). Returns the generated
     * banner URL, or {@code null} when generation yields nothing usable so the caller can
     * fall back to TEMPLATE without charging quota.
     *
     * <p>Per {@code ai-branding-guidelines.md} §2.3 the backend builds the prompt — the user
     * never writes free-form prompt text (constrained presets only).</p>
     */
    private String generateFullAiBanner(PreviewBannerRequest body, BrandColours colours,
            UUID instanceId) {
        String prompt = buildFullAiBannerPrompt(body, colours);
        try {
            // G1 walk 2026-06-12: (1) STRICT call — không placeholder-fallback; lỗi propagate
            // để caller trả GENERATION_FAILED + KHÔNG trừ quota (GAP-1218). (2) Size
            // 1536x1024 = landscape hợp lệ của gpt-image-1 (1792x1024 là size dall-e-3 cũ).
            String result = resilientAiClient.generateImageStrict(prompt, "1536x1024")
                    .block(Duration.ofSeconds(120));
            return persistFullAiBanner(result, instanceId);
        } catch (Exception ex) {
            log.warn("FULL_AI banner generation failed → fallback TEMPLATE (no quota charge): {}",
                    ex.getMessage());
            return null;
        }
    }

    /**
     * gpt-image-1 trả base64 (data-URI từ OpenAIClient) — persist vào MinIO rồi trả
     * presigned URL (data-URI ~2-3MB không phù hợp giữ trong FE state / job assets).
     * URL thường (dall-e legacy) passthrough.
     */
    private String persistFullAiBanner(String urlOrDataUri, UUID instanceId) {
        if (urlOrDataUri == null || !urlOrDataUri.startsWith("data:image/")) {
            return urlOrDataUri;
        }
        int comma = urlOrDataUri.indexOf(',');
        byte[] bytes = java.util.Base64.getDecoder().decode(urlOrDataUri.substring(comma + 1));
        String tenant = instanceId == null ? "unknown" : instanceId.toString();
        String path = "instances/" + tenant + "/branding/full-ai-banner-"
                + System.currentTimeMillis() + ".png";
        s3StorageService.uploadAsset(
                new java.io.ByteArrayInputStream(bytes), path, "image/png", bytes.length);
        return s3StorageService.getPresignedAssetUrl(path);
    }

    /** Build the fixed FULL_AI banner prompt from constrained wizard inputs (no free text). */
    private static String buildFullAiBannerPrompt(PreviewBannerRequest body, BrandColours colours) {
        String org = body.organizationName() != null && !body.organizationName().isBlank()
                ? body.organizationName() : "Trung tâm giáo dục";
        StringBuilder prompt = new StringBuilder();
        prompt.append("Professional marketing banner for the Vietnamese education centre \"")
                .append(org).append("\".");
        if (body.copy() != null && !body.copy().isBlank()) {
            prompt.append(" Headline message: \"").append(body.copy()).append("\".");
        }
        if (body.themeIcon() != null && !body.themeIcon().isBlank()) {
            prompt.append(" Theme/subject: ").append(body.themeIcon()).append(".");
        }
        if (body.portraitUrls() != null && !body.portraitUrls().isEmpty()) {
            prompt.append(" Feature a friendly, professional teacher portrait.");
        }
        if (colours != null && colours.primary() != null) {
            prompt.append(" Brand colour palette primary ").append(colours.primary());
            if (colours.accent() != null) {
                prompt.append(", accent ").append(colours.accent());
            }
            prompt.append('.');
        }
        prompt.append(" Clean modern layout, high contrast, Vietnamese-friendly typography,"
                + " trustworthy and welcoming tone. No lorem ipsum, no watermark.");
        return prompt.toString();
    }

    /** Resolve the JWT tenant claim to a UUID, or a throwaway id for ephemeral previews. */
    private static UUID resolveTenantOrEphemeral() {
        String tenantId = MDC.get("tenantId");
        if (tenantId != null && !tenantId.isBlank()) {
            try {
                return UUID.fromString(tenantId.trim());
            } catch (IllegalArgumentException ignored) {
                // fall through to ephemeral key
            }
        }
        return UUID.randomUUID();
    }

    /**
     * Approve the generated theme + trigger MOCK deploy provisioning (GAP-1021).
     * Persists the approved theme as the instance active branding then drives the
     * lifecycle NOT_STARTED→INITIALIZING→GENERATING→DEPLOYED asynchronously — the
     * SSE {@code deploy-stream} surfaces live progress. Returns {@code 202}
     * immediately so the stream can observe progression.
     *
     * <p><b>Mock boundary:</b> real per-tenant infra is deferred to GAP-1055;
     * {@code frontendUrl} is a placeholder. See {@link MockProvisioningService}.</p>
     *
     * @param jobId branding job to deploy
     * @param req   approved resources + slug (for frontendUrl)
     * @return {@code 202} with {jobId, status, frontendUrl}, or {@code 404}
     */
    @PostMapping("/{jobId}/approve")
    @PreAuthorize(WRITE_AUTHZ)
    public ResponseEntity<?> approve(@PathVariable UUID jobId,
                                     @RequestBody(required = false) ApproveDeployRequest req) {
        Optional<BrandingJob> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "JOB_NOT_FOUND",
                    "jobId", jobId.toString()
            ));
        }
        BrandingJob job = jobOpt.get();

        // GAP-1217 — quality gate BEFORE deploy. Asset score <70 must never auto-deploy
        // (ai-branding-guidelines.md §5). Failing the gate marks the job FAILED with the
        // reason + score so the wizard surfaces it at the approve step instead of shipping
        // a low-quality landing. Reuses the GAP-272c aggregator (no rebuild).
        QualityScoreResponse quality = qualityScoreAggregator.aggregate(job);
        if (!quality.passed()) {
            String reason = "Điểm chất lượng " + quality.score() + "/100 dưới ngưỡng "
                    + quality.threshold() + " — không thể triển khai tự động";
            brandingJobService.markJobFailed(jobId, reason);
            Map<String, Object> failBody = new LinkedHashMap<>();
            failBody.put("error", "QUALITY_GATE_FAILED");
            failBody.put("jobId", jobId.toString());
            failBody.put("score", quality.score());
            failBody.put("threshold", quality.threshold());
            failBody.put("issues", quality.issues());
            failBody.put("message", reason);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(failBody);
        }

        String slug = req != null ? req.slug() : null;
        if (slug == null || slug.isBlank()) {
            slug = "tenant";
        }
        List<String> approved = req != null && req.approvedResources() != null
                ? req.approvedResources() : List.of();
        String templateId = req != null ? req.templateId() : null;

        // Async — returns 202 immediately; SSE deploy-stream observes progression.
        mockProvisioningService.provisionAsync(jobId, slug, templateId, approved);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobId", jobId.toString());
        body.put("status", "INITIALIZING");
        body.put("frontendUrl", "https://" + slug + ".kitehub.me");
        // GAP-1217 — surface the passing score at the approve/deploy step.
        body.put("qualityScore", quality.score());
        body.put("message", "Đang triển khai (mock provisioning)");
        return ResponseEntity.accepted().body(body);
    }

    private static String firstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                return c;
            }
        }
        return "Trung tâm mới";
    }

    @GetMapping("/{jobId}")
    @PreAuthorize(READ_AUTHZ)
    public ResponseEntity<?> getJob(@PathVariable UUID jobId) {
        Optional<BrandingJob> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "JOB_NOT_FOUND",
                    "jobId", jobId.toString()
            ));
        }
        BrandingJob job = jobOpt.get();
        BrandColours colours = coloursDeriver.derive(job);
        // GAP-390-A: resolve tenantId from request-scoped MDC (populated by gateway tenant filter
        // per logback-spring.xml MDC keys). Falls back to null when request lacks tenant context.
        String tenantId = MDC.get("tenantId");
        BrandingJobResponse response = BrandingJobResponse.from(job, colours, tenantId);
        log.debug("v1 job lookup: {} status={} brandColors.source={}",
                jobId, response.status(), colours.source());
        return ResponseEntity.ok(response);
    }
}
