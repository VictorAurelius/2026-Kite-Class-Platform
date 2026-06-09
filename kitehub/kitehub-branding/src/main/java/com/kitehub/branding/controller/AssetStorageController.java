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
        String url = s3StorageService.uploadAsset(
            file.getInputStream(),
            path,
            file.getContentType(),
            file.getSize()
        );

        BrandingAsset asset = BrandingAsset.builder()
            .type(assetType)
            .variant(extractVariant(originalFilename))
            .url(url)
            .sizeBytes(file.getSize())
            .contentType(file.getContentType())
            .uploadedAt(Instant.now().getEpochSecond())
            .build();

        // Persist asset to BrandingJob
        try {
            persistAssetToJob(instanceId, asset);
        } catch (Exception e) {
            log.error("Failed to persist asset to BrandingJob for instance: {}", instanceId, e);
            // Continue - upload succeeded even if persistence failed
        }

        return ResponseEntity.ok(asset);
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
            log.info("Retrieved {} assets for instance: {}", assets.size(), instanceId);

            return ResponseEntity.ok(assets);

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
     * @param instanceId Instance UUID
     * @param asset Asset to persist
     */
    private void persistAssetToJob(UUID instanceId, BrandingAsset asset) throws IOException {
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

        // Parse existing assets
        List<BrandingAsset> assets = new ArrayList<>();
        if (job.getAssetsGenerated() != null && !job.getAssetsGenerated().isEmpty()) {
            assets = parseAssetsJson(job.getAssetsGenerated());
        }

        // Add new asset
        assets.add(asset);

        // Serialize back to JSON
        String assetsJson = objectMapper.writeValueAsString(assets);
        brandingJobService.updateGeneratedAssets(job.getId(), assetsJson);

        log.debug("Persisted asset to BrandingJob: {} for instance: {}", job.getId(), instanceId);
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
