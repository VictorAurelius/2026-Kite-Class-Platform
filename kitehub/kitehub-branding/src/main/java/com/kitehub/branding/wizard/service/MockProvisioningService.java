package com.kitehub.branding.wizard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.dto.BrandingAsset;
import com.kitehub.branding.lifecycle.InstanceLifecycleService;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.service.BrandingJobService;
import com.kitehub.branding.wizard.dto.BrandColours;
import com.kitehub.branding.wizard.quality.BrandColoursDeriver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Phase 1 MOCK tenant-provisioning driver for the AI Branding wizard deploy step
 * (GAP-1021 + GAP-272e). Advances a {@link BrandingJob} through the deploy
 * lifecycle so the SSE deploy-stream emits live progress and the instance state
 * machine reaches {@code DEPLOYED} — WITHOUT standing up real isolated tenant
 * infrastructure.
 *
 * <h2>MOCK BOUNDARY (deferred to follow-up gaps)</h2>
 * <ul>
 *   <li><b>GAP-1055</b> — real per-tenant DB / MinIO bucket / DNS subdomain
 *       provisioning. Here {@code frontendUrl} is a placeholder
 *       {@code https://{slug}.kiteclass.vn}; no DNS record or isolated infra is
 *       created.</li>
 *   <li><b>GAP-811 / GAP-1077</b> — Host-based subdomain render of the tenant
 *       site. The deploy "completes" without a live subdomain serving.</li>
 * </ul>
 *
 * <p>What this DOES do (real, persisted):
 * <ol>
 *   <li>Drives {@link BrandingJob#getStatus()} QUEUED → PROCESSING → COMPLETED
 *       with progress %, so the {@code DeployStreamController} poller emits
 *       {@code state-change} / {@code progress} / {@code complete} SSE events.</li>
 *   <li>Drives the §6 instance lifecycle GENERATING → DEPLOYED via
 *       {@link BrandingJobService#updateJobProgress} (which owns the lifecycle
 *       hinge — see {@code ai-branding-guidelines.md} §6).</li>
 *   <li>Persists the approved theme JSON (template + approved resources + brand
 *       colours + frontendUrl) onto the job via
 *       {@link BrandingJobService#updateGeneratedAssets} — the mock equivalent of
 *       "instance active branding" (GAP-1021 pt1).</li>
 *   <li>Records a {@code deploy-completed} lifecycle marker carrying the
 *       placeholder {@code frontendUrl}.</li>
 * </ol>
 *
 * <p>Runs on a separate thread via {@link Async} (enabled by {@code @EnableAsync}
 * on the application class) so the approve endpoint returns {@code 202} immediately
 * and the SSE stream observes progression concurrently. Each step delegates to
 * {@link BrandingJobService} methods which manage their own transactions.</p>
 *
 * @since GAP-1021 (Phase 1 deploy pipeline mock)
 */
@Slf4j
@Service
public class MockProvisioningService {

    private final BrandingJobService brandingJobService;
    private final BrandingJobRepository jobRepository;
    private final InstanceLifecycleService lifecycleService;
    private final BrandColoursDeriver coloursDeriver;
    private final ObjectMapper objectMapper;

    /**
     * Delay between deploy steps (ms). Long enough for the 2s SSE poller to
     * observe each status/progress change. Configurable for tests.
     */
    private final long stepDelayMs;

    public MockProvisioningService(
            BrandingJobService brandingJobService,
            BrandingJobRepository jobRepository,
            InstanceLifecycleService lifecycleService,
            BrandColoursDeriver coloursDeriver,
            ObjectMapper objectMapper,
            @Value("${kitehub.branding.mock-provision.step-delay-ms:2200}") long stepDelayMs) {
        this.brandingJobService = brandingJobService;
        this.jobRepository = jobRepository;
        this.lifecycleService = lifecycleService;
        this.coloursDeriver = coloursDeriver;
        this.objectMapper = objectMapper;
        this.stepDelayMs = stepDelayMs;
    }

    /**
     * Drive the mock deploy lifecycle for {@code jobId} asynchronously.
     *
     * @param jobId              the branding job (must exist)
     * @param slug               tenant slug → placeholder frontendUrl
     * @param templateId         selected template id (recorded in theme)
     * @param approvedResources  resources the owner approved (recorded in theme)
     */
    @Async
    public void provisionAsync(UUID jobId, String slug, String templateId, List<String> approvedResources) {
        log.info("MOCK provision started: job={} slug={}", jobId, slug);
        try {
            // Step 1 — INITIALIZING → GENERATING (QUEUED → PROCESSING).
            brandingJobService.updateJobProgress(jobId, JobStatus.PROCESSING, 35, "Đang khởi tạo trang web");
            sleep();

            // Step 2 — generating assets (progress only, status stays PROCESSING).
            brandingJobService.updateJobProgress(jobId, JobStatus.PROCESSING, 70, "Đang tạo giao diện thương hiệu");
            sleep();

            // Step 3 — persist approved resources as a BrandingAsset[] (mock
            // "instance active branding"). MUST be a JSON array so
            // AssetStorageController.getAssets (TypeReference<List<BrandingAsset>>)
            // can parse it — a metadata OBJECT threw MismatchedInputException and
            // surfaced "0 assets" post-deploy (GAP-1107 #2).
            String frontendUrl = buildFrontendUrl(slug);
            persistAssets(jobId, slug, templateId, approvedResources);

            // Step 4 — GENERATING → DEPLOYED (PROCESSING → COMPLETED). Lifecycle
            // hinge in updateJobProgress drives instance state to DEPLOYED.
            brandingJobService.updateJobProgress(jobId, JobStatus.COMPLETED, 100, "Đã triển khai");

            recordDeployMarker(jobId, frontendUrl, templateId, slug);
            log.info("MOCK provision complete: job={} frontendUrl={}", jobId, frontendUrl);
        } catch (Exception ex) {
            log.error("MOCK provision failed: job={} err={}", jobId, ex.getMessage(), ex);
            brandingJobService.markJobFailed(jobId,
                    "Triển khai mock thất bại: " + ex.getMessage());
        }
    }

    private String buildFrontendUrl(String slug) {
        String safe = (slug == null || slug.isBlank()) ? "tenant" : slug.trim();
        return "https://" + safe + ".kiteclass.vn"; // MOCK placeholder (GAP-1055/811/1077)
    }

    /** Default resource set when the approve request carries no explicit list. */
    static final List<String> DEFAULT_RESOURCES = List.of("logo", "colors", "banner", "hero");

    /**
     * Persist the approved resources onto the job as a {@code BrandingAsset[]}
     * (the mock "active branding"). Persisted shape MUST be a JSON array so
     * {@link com.kitehub.branding.controller.AssetStorageController} can parse it
     * with {@code TypeReference<List<BrandingAsset>>}. The earlier mock wrote a
     * theme-metadata OBJECT which threw {@code MismatchedInputException} →
     * "0 assets" post-deploy (GAP-1107 #2). The {@code frontendUrl} + brand colours
     * live on the {@code deploy-completed} lifecycle marker, not here.
     * Real theme-table persistence is deferred (see {@link BrandColoursDeriver}
     * TODO + GAP-1055).
     */
    private void persistAssets(UUID jobId, String slug, String templateId,
                               List<String> approvedResources) {
        Optional<BrandingJob> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            return;
        }
        BrandColours colours = coloursDeriver.derive(jobOpt.get());
        List<BrandingAsset> assets = buildDeployedAssets(slug, templateId, approvedResources, colours);
        try {
            brandingJobService.updateGeneratedAssets(jobId, objectMapper.writeValueAsString(assets));
        } catch (Exception ex) {
            log.warn("Failed to serialize mock assets for job {}: {}", jobId, ex.getMessage());
        }
    }

    /**
     * Build one mock {@link BrandingAsset} per approved resource (falling back to
     * {@link #DEFAULT_RESOURCES} when none were approved) so the post-deploy
     * {@code /branding} page renders a non-empty asset set (GAP-1107 #2 /
     * GAP-1108). URLs are mock-CDN placeholders (real per-tenant asset storage =
     * GAP-1055). Package-private + static for unit-test round-trip verification.
     */
    static List<BrandingAsset> buildDeployedAssets(String slug, String templateId,
                                                   List<String> approvedResources,
                                                   BrandColours colours) {
        List<String> resources = (approvedResources == null || approvedResources.isEmpty())
                ? DEFAULT_RESOURCES
                : approvedResources;
        String safeSlug = (slug == null || slug.isBlank()) ? "tenant" : slug.trim();
        String tpl = (templateId == null || templateId.isBlank()) ? "default" : templateId;
        long now = Instant.now().getEpochSecond();
        List<BrandingAsset> assets = new ArrayList<>();
        for (String resource : resources) {
            if (resource == null || resource.isBlank()) {
                continue;
            }
            String type = canonicalAssetType(resource);
            boolean isColours = "COLORS".equals(type);
            String variant = isColours && colours != null ? colours.primary() : tpl;
            assets.add(BrandingAsset.builder()
                    .type(type)
                    .variant(variant)
                    .url(mockAssetUrl(safeSlug, resource, isColours))
                    .sizeBytes(0L)
                    .contentType(isColours ? "application/json" : "image/svg+xml")
                    .uploadedAt(now)
                    .build());
        }
        return assets;
    }

    /** Normalise a wizard resource key to the canonical uppercase asset type. */
    private static String canonicalAssetType(String resource) {
        String r = resource.trim().toLowerCase(Locale.ROOT);
        return switch (r) {
            case "logo" -> "LOGO";
            case "colors", "color", "colours", "palette" -> "COLORS";
            case "banner" -> "BANNER";
            case "hero" -> "HERO";
            case "profile" -> "PROFILE";
            case "og", "og_image", "ogimage" -> "OG_IMAGE";
            default -> r.toUpperCase(Locale.ROOT);
        };
    }

    /** Mock CDN URL — keeps the {@code /instances/} segment so delete-path extraction works. */
    private static String mockAssetUrl(String slug, String resource, boolean isColours) {
        String ext = isColours ? "json" : "svg";
        return "https://mock-cdn.kiteclass.com/instances/" + slug
                + "/branding/" + resource.trim().toLowerCase(Locale.ROOT) + "." + ext;
    }

    /**
     * Append a {@code deploy-completed} lifecycle marker carrying the
     * {@code frontendUrl} + templateId + slug so the post-deploy {@code /branding}
     * deploy-status card (GAP-1108) can surface the live landing link + summary.
     */
    private void recordDeployMarker(UUID jobId, String frontendUrl, String templateId, String slug) {
        jobRepository.findById(jobId).ifPresent(job -> {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("jobId", jobId);
            meta.put("frontendUrl", frontendUrl);
            meta.put("templateId", templateId);
            meta.put("slug", slug);
            meta.put("mock", true);
            try {
                lifecycleService.recordMarker(
                        job.getInstanceId(), "deploy-completed",
                        InstanceLifecycleService.Actor.system("mock-provisioning"), meta);
            } catch (Exception ex) {
                log.warn("Failed to record deploy marker for job {}: {}", jobId, ex.getMessage());
            }
        });
    }

    private void sleep() {
        if (stepDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(stepDelayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
