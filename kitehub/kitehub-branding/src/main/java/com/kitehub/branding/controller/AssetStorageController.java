package com.kitehub.branding.controller;

import com.kitehub.branding.dto.BrandingAsset;
import com.kitehub.branding.service.S3StorageService;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for asset storage operations.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/branding/assets")
@RequiredArgsConstructor
public class AssetStorageController {

    private final S3StorageService s3StorageService;

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

        return ResponseEntity.ok(asset);
    }

    /**
     * Get all assets for instance.
     * For MVP: Returns mock data. Full implementation in PR 4.9.
     *
     * @param instanceId Instance UUID
     * @return List of assets
     */
    @GetMapping("/{instanceId}")
    public ResponseEntity<List<BrandingAsset>> getAssets(@PathVariable UUID instanceId) {
        log.info("Getting assets for instance: {}", instanceId);

        // For MVP: Return mock data
        // TODO PR 4.9: Query from BrandingJob entity
        List<BrandingAsset> mockAssets = List.of(
            BrandingAsset.builder()
                .type("profile")
                .variant("cutout")
                .url(s3StorageService.getAssetUrl("instances/" + instanceId + "/branding/profile/cutout.png"))
                .sizeBytes(125000L)
                .contentType("image/png")
                .uploadedAt(Instant.now().getEpochSecond())
                .build(),
            BrandingAsset.builder()
                .type("hero")
                .variant("variant1")
                .url(s3StorageService.getAssetUrl("instances/" + instanceId + "/branding/hero/variant1.jpg"))
                .sizeBytes(350000L)
                .contentType("image/jpeg")
                .uploadedAt(Instant.now().getEpochSecond())
                .build()
        );

        return ResponseEntity.ok(mockAssets);
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

        // For MVP: Mock deletion
        // TODO PR 4.9: Delete from S3 and update BrandingJob entity
        log.info("Mock mode: Asset deletion simulated for instance {}", instanceId);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Assets deleted for instance " + instanceId
        ));
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
}
