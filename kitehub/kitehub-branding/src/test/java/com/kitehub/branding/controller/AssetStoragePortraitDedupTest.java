package com.kitehub.branding.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.dto.BrandingAsset;
import com.kitehub.branding.service.BrandingJobService;
import com.kitehub.branding.service.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GAP-1116 — PORTRAIT accumulation vs LOGO replace-by-assetType dedup.
 *
 * <p>Uses a REAL {@link ObjectMapper} + a stateful job (each persisted JSON feeds
 * back into the next upload) so the dedup behaviour is exercised end-to-end.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AssetStorageController PORTRAIT dedup (GAP-1116)")
class AssetStoragePortraitDedupTest {

    @Mock private S3StorageService s3StorageService;
    @Mock private BrandingJobService brandingJobService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AssetStorageController controller;

    private UUID instanceId;
    private BrandingJob job;

    @BeforeEach
    void setUp() {
        controller = new AssetStorageController(s3StorageService, brandingJobService, objectMapper);

        instanceId = UUID.randomUUID();
        job = new BrandingJob();
        job.setId(UUID.randomUUID());
        job.setInstanceId(instanceId);

        when(brandingJobService.getJobsByInstance(instanceId))
                .thenReturn(Collections.singletonList(job));

        // S3 stubs: path/url carry "/instances/" so extractPathFromUrl can recover the key.
        when(s3StorageService.generateAssetPath(eq(instanceId), anyString(), anyString()))
                .thenAnswer(inv -> "instances/" + instanceId + "/branding/"
                        + inv.getArgument(1) + "/" + inv.getArgument(2));
        when(s3StorageService.uploadAsset(any(), anyString(), anyString(), anyLong()))
                .thenAnswer(inv -> "https://cdn/" + inv.getArgument(1));

        // Stateful persistence: write-back into the job so the next upload sees it.
        doAnswer(inv -> {
            job.setAssetsGenerated(inv.getArgument(1));
            return null;
        }).when(brandingJobService).updateGeneratedAssets(eq(job.getId()), any());
    }

    private void upload(String assetType, String filename) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "image/png", "data".getBytes());
        controller.uploadAsset(instanceId, assetType, file);
    }

    private List<BrandingAsset> persisted() throws Exception {
        String json = job.getAssetsGenerated();
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(json, new TypeReference<List<BrandingAsset>>() {});
    }

    private List<String> typesOf(List<BrandingAsset> assets) {
        return assets.stream().map(BrandingAsset::getType).collect(Collectors.toList());
    }

    @Test
    @DisplayName("N PORTRAIT uploads → N PORTRAIT assets kept (no dedup)")
    void portraitsAccumulate() throws Exception {
        upload("PORTRAIT", "p1.png");
        upload("PORTRAIT", "p2.png");
        upload("PORTRAIT", "p3.png");

        List<BrandingAsset> assets = persisted();
        assertThat(assets).hasSize(3);
        assertThat(typesOf(assets)).containsOnly("PORTRAIT");
        // PORTRAIT is never deduped → no S3 deletes.
        verify(s3StorageService, never()).deleteAsset(anyString());
    }

    @Test
    @DisplayName("LOGO re-upload → deduped to exactly 1 (replace-by-assetType)")
    void logoDedupesToOne() throws Exception {
        upload("LOGO", "logo-v1.png");
        upload("LOGO", "logo-v2.png");
        upload("LOGO", "logo-v3.png");

        List<BrandingAsset> assets = persisted();
        assertThat(assets).hasSize(1);
        assertThat(assets.get(0).getType()).isEqualTo("LOGO");
        assertThat(assets.get(0).getUrl()).contains("logo-v3.png");
        // 2 prior logos replaced → 2 S3 deletes.
        verify(s3StorageService, times(2)).deleteAsset(anyString());
    }

    @Test
    @DisplayName("Mixed: LOGO deduped to 1 while PORTRAITs accumulate")
    void mixedLogoDedupPortraitAccumulate() throws Exception {
        upload("LOGO", "logo-v1.png");
        upload("PORTRAIT", "p1.png");
        upload("PORTRAIT", "p2.png");
        upload("LOGO", "logo-v2.png");   // replaces logo-v1, keeps both portraits

        List<BrandingAsset> assets = persisted();
        assertThat(assets).hasSize(3);
        assertThat(typesOf(assets)).containsExactlyInAnyOrder("LOGO", "PORTRAIT", "PORTRAIT");

        BrandingAsset logo = assets.stream()
                .filter(a -> "LOGO".equals(a.getType())).findFirst().orElseThrow();
        assertThat(logo.getUrl()).contains("logo-v2.png");
        // Exactly 1 logo replacement → 1 S3 delete (portraits untouched).
        verify(s3StorageService, times(1)).deleteAsset(anyString());
    }

    @Test
    @DisplayName("PORTRAIT is case-insensitive (portrait / Portrait accumulate)")
    void portraitCaseInsensitive() throws Exception {
        upload("portrait", "p1.png");
        upload("Portrait", "p2.png");

        List<BrandingAsset> assets = persisted();
        assertThat(assets).hasSize(2);
        verify(s3StorageService, never()).deleteAsset(anyString());
    }
}
