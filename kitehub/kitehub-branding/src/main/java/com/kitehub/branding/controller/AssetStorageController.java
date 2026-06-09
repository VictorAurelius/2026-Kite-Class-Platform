package com.kitehub.branding.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.dto.BrandingAsset;
import com.kitehub.branding.service.BrandingJobService;
import com.kitehub.branding.service.S3StorageService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for asset storage operations.
 *
 * <p>SLO Tier D (uploads can be multi-MB; class-level Tier D budget covers
 * worst-case logo upload). See {@code documents/05-guides/api-performance-slo.md}.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/branding/assets")
@RequiredArgsConstructor
@Tag(name = "Asset Storage", description = "Upload, retrieve, and delete branding assets (S3)")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-d", "controller", "asset-storage"})
public class AssetStorageController {

    private final S3StorageService s3StorageService;
    private final BrandingJobService brandingJobService;
    private final ObjectMapper objectMapper;

    /**
     * Upload asset for instance.
     *
     * @param instanceId Instance UUID
     * @param assetType Asset type (profile, hero, logos, etc.)
     * @param file File to upload
     * @return Uploaded asset information
     */
    @PostMapping(value = "/{instanceId}/{assetType}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BrandingAsset> uploadAsset(
        @PathVariable UUID instanceId,
        @PathVariable String assetType,
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        log.info("Uploading {} asset for instance: {}", assetType, instanceId);

        String originalFilename = file.getOriginalFilename();
        String path = s3StorageService.generateAssetPath(instanceId, assetType, originalFilename);
        // Stable storage URL (path-based) — persisted to the job so delete/dedup can
        // always recover the object key (does NOT expire, unlike a presigned URL).
        String storageUrl = s3StorageService.uploadAsset(
            file.getInputStream(),
            path,
            file.getContentType(),
            file.getSize()
        );

        long uploadedAt = Instant.now().getEpochSecond();
        BrandingAsset stored = BrandingAsset.builder()
            .type(assetType)
            .variant(extractVariant(originalFilename))
            .url(storageUrl)
            .sizeBytes(file.getSize())
            .contentType(file.getContentType())
            .uploadedAt(uploadedAt)
            .build();

        // Persist asset to BrandingJob (GAP-1112 #2: replace-by-assetType dedup —
        // exactly 1 asset per (instanceId, assetType)).
        try {
            persistAssetToJob(instanceId, assetType, stored);
        } catch (Exception e) {
            log.error("Failed to persist asset to BrandingJob for instance: {}", instanceId, e);
            // Continue - upload succeeded even if persistence failed
        }

        // GAP-1112 #1: return a browser-loadable presigned GET URL for immediate
        // preview. The raw storage/MinIO URL cannot be loaded from a private bucket.
        BrandingAsset response = BrandingAsset.builder()
            .type(assetType)
            .variant(stored.getVariant())
            .url(s3StorageService.getPresignedAssetUrl(path))
            .sizeBytes(file.getSize())
            .contentType(file.getContentType())
            .uploadedAt(uploadedAt)
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get all assets for instance.
     *
     * @param instanceId Instance UUID
     * @return List of assets
     */
    @GetMapping("/{instanceId}")
    public ResponseEntity<List<BrandingAsset>> getAssets(@PathVariable UUID instanceId) {
        log.info("Getting assets for instance: {}", instanceId);

        try {
            // Query from BrandingJob entity
            List<BrandingJob> jobs = brandingJobService.getJobsByInstance(instanceId);

            if (jobs.isEmpty()) {
                log.debug("No branding jobs found for instance: {}", instanceId);
                return ResponseEntity.ok(Collections.emptyList());
            }

            // Get most recent completed job
            BrandingJob latestJob = jobs.stream()
                .filter(job -> job.getAssetsGenerated() != null && !job.getAssetsGenerated().isEmpty())
                .findFirst()
                .orElse(null);

            if (latestJob == null) {
                log.debug("No assets generated yet for instance: {}", instanceId);
                return ResponseEntity.ok(Collections.emptyList());
            }

            // Parse assetsGenerated JSON
            List<BrandingAsset> assets = parseAssetsJson(latestJob.getAssetsGenerated());
            // GAP-1112 #1: re-presign each stored (stable) URL so the browser can load
            // the preview for existing/post-deploy assets too.
            List<BrandingAsset> presigned = presignAssets(assets);
            log.info("Retrieved {} assets for instance: {}", presigned.size(), instanceId);

            return ResponseEntity.ok(presigned);

        } catch (Exception e) {
            log.error("Failed to retrieve assets for instance: {}", instanceId, e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    /**
     * Delete all assets for instance.
     *
     * @param instanceId Instance UUID
     * @return Success message
     */
    @DeleteMapping("/{instanceId}")
    public ResponseEntity<Map<String, String>> deleteAssets(@PathVariable UUID instanceId) {
        log.info("Deleting assets for instance: {}", instanceId);

        try {
            // Get all jobs for instance
            List<BrandingJob> jobs = brandingJobService.getJobsByInstance(instanceId);

            int deletedCount = 0;

            for (BrandingJob job : jobs) {
                if (job.getAssetsGenerated() != null && !job.getAssetsGenerated().isEmpty()) {
                    // Parse assets
                    List<BrandingAsset> assets = parseAssetsJson(job.getAssetsGenerated());

                    // Delete each asset from S3
                    for (BrandingAsset asset : assets) {
                        try {
                            String path = extractPathFromUrl(asset.getUrl());
                            s3StorageService.deleteAsset(path);
                            deletedCount++;
                        } catch (Exception e) {
                            log.warn("Failed to delete asset: {}", asset.getUrl(), e);
                        }
                    }

                    // Clear assetsGenerated in BrandingJob
                    brandingJobService.updateGeneratedAssets(job.getId(), null);
                }
            }

            log.info("Deleted {} assets for instance: {}", deletedCount, instanceId);

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", String.format("Deleted %d assets for instance %s", deletedCount, instanceId)
            ));

        } catch (Exception e) {
            log.error("Failed to delete assets for instance: {}", instanceId, e);
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", "Failed to delete assets: " + e.getMessage()
            ));
        }
    }

    /**
     * Extract variant from filename.
     *
     * @param filename Original filename
     * @return Variant name
     */
    private String extractVariant(String filename) {
        if (filename == null) {
            return "default";
        }

        String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
        return nameWithoutExt.replaceAll("\\d+_", ""); // Remove timestamp
    }

    /**
     * Persist asset to BrandingJob.
     *
     * <p><b>Dedup policy (GAP-1112 #2 LOGO + GAP-1116 PORTRAIT):</b></p>
     * <ul>
     *   <li><b>PORTRAIT</b> — accumulates 1..N per instance: NEVER deduped. A centre
     *       uploads many teacher portraits; a solo teacher uploads one. The portrait
     *       count is driven by the wizard user-type axis (GAP-1115).</li>
     *   <li><b>Every other type (LOGO, HERO, ...)</b> — replace-by-assetType: exactly 1
     *       asset per {@code (instanceId, assetType)}. Re-uploading removes + S3-deletes
     *       the prior one (no duplicate rows / orphan objects).</li>
     * </ul>
     *
     * @param instanceId Instance UUID
     * @param assetType Asset type whose previous version(s) get replaced (except PORTRAIT)
     * @param asset Asset to persist (carries the stable storage URL)
     */
    private void persistAssetToJob(UUID instanceId, String assetType, BrandingAsset asset) throws IOException {
        List<BrandingJob> jobs = brandingJobService.getJobsByInstance(instanceId);

        BrandingJob job;

        if (jobs.isEmpty()) {
            // No job exists - create a draft job automatically
            log.info("Auto-creating draft BrandingJob for instance: {}", instanceId);
            job = brandingJobService.createJob(instanceId, "Draft", "vi", asset.getUrl());
        } else {
            // Get most recent job
            job = jobs.get(0);
        }

        // Parse existing assets (mutable copy — parseAssetsJson may return an
        // immutable empty list).
        List<BrandingAsset> assets = new ArrayList<>(parseAssetsJson(job.getAssetsGenerated()));

        // GAP-1112 #2 + GAP-1116: replace-by-assetType for EVERY type EXCEPT PORTRAIT.
        // PORTRAIT accumulates 1..N per instance (centre = many teacher portraits, solo
        // = one), driven by the wizard user-type axis (GAP-1115). All other types keep
        // exactly 1 per (instanceId, assetType): remove + S3-delete the prior same-type
        // asset (case-insensitive match).
        boolean isPortrait = "PORTRAIT".equalsIgnoreCase(assetType);
        if (!isPortrait) {
            assets.removeIf(existing -> {
                boolean sameType = existing.getType() != null
                    && existing.getType().equalsIgnoreCase(assetType);
                if (sameType) {
                    try {
                        s3StorageService.deleteAsset(extractPathFromUrl(existing.getUrl()));
                    } catch (Exception e) {
                        log.warn("Failed to delete replaced {} asset: {}", assetType, existing.getUrl(), e);
                    }
                }
                return sameType;
            });
        }

        // Add new asset
        assets.add(asset);

        // Serialize back to JSON
        String assetsJson = objectMapper.writeValueAsString(assets);
        brandingJobService.updateGeneratedAssets(job.getId(), assetsJson);

        log.debug("Persisted {} asset to BrandingJob: {} for instance: {} ({} total, dedup={})",
            assetType, job.getId(), instanceId, assets.size(), !isPortrait);
    }

    /**
     * Re-presign stored asset URLs for browser preview (GAP-1112 #1). Storage keeps
     * a stable, non-expiring path-based URL; this swaps it for a short-lived
     * presigned GET URL only on the response. URLs whose object key cannot be
     * extracted (e.g. external/legacy URLs) are returned unchanged.
     */
    private List<BrandingAsset> presignAssets(List<BrandingAsset> assets) {
        List<BrandingAsset> result = new ArrayList<>(assets.size());
        for (BrandingAsset asset : assets) {
            String url = asset.getUrl();
            // Only re-presign URLs that carry a recoverable object key.
            if (url != null && url.contains("/instances/")) {
                try {
                    String presignedUrl = s3StorageService.getPresignedAssetUrl(extractPathFromUrl(url));
                    asset.setUrl(presignedUrl);
                } catch (Exception e) {
                    log.warn("Failed to presign asset URL {} — keeping stored URL", url, e);
                }
            }
            result.add(asset);
        }
        return result;
    }

    /**
     * Parse assetsGenerated JSON to list of BrandingAsset.
     *
     * @param assetsJson JSON string
     * @return List of assets
     */
    private List<BrandingAsset> parseAssetsJson(String assetsJson) {
        if (assetsJson == null || assetsJson.isEmpty()) {
            return Collections.emptyList();
        }

        // GAP-1107 #2: legacy/old-mock rows persisted assetsGenerated as a theme
        // metadata OBJECT ({slug,templateId,frontendUrl,...}) rather than a
        // BrandingAsset[] array. Detect the shape up front so a non-array legacy
        // row degrades to "no assets" (debug log) instead of an error-level
        // MismatchedInputException stack trace on every getAssets call.
        if (!assetsJson.trim().startsWith("[")) {
            log.debug("assetsGenerated is not a JSON array (legacy theme-metadata shape) — returning no assets");
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(assetsJson, new TypeReference<List<BrandingAsset>>() {});
        } catch (Exception e) {
            log.error("Failed to parse assetsGenerated JSON: {}", assetsJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * Extract S3 path from asset URL.
     *
     * @param url Asset URL
     * @return S3 object key
     */
    private String extractPathFromUrl(String url) {
        // Handle CDN URLs (e.g., https://cdn.kiteclass.com/instances/xxx/...)
        // Handle S3 URLs (e.g., https://bucket.s3.region.amazonaws.com/instances/xxx/...)
        // Handle presigned URLs (e.g., https://bucket.s3.region.amazonaws.com/instances/xxx?...)
        // Handle mock URLs (e.g., https://mock-cdn.kiteclass.com/instances/xxx/...)

        String path = url;

        // Remove query parameters
        int queryIndex = path.indexOf('?');
        if (queryIndex > 0) {
            path = path.substring(0, queryIndex);
        }

        // Extract path after domain
        int instancesIndex = path.indexOf("/instances/");
        if (instancesIndex > 0) {
            return path.substring(instancesIndex + 1); // Remove leading slash
        }

        // Fallback: return full URL
        log.warn("Could not extract S3 path from URL: {}", url);
        return url;
    }
}
