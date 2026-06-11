package com.kitehub.branding.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.dto.BrandingAsset;
import com.kitehub.branding.service.BrandingJobService;
import com.kitehub.branding.service.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GAP-1112 #2 — re-uploading the same {@code assetType} must REPLACE the prior
 * asset (S3 object + persisted JSON row) so a job holds exactly 1 asset per
 * {@code (instanceId, assetType)} instead of accumulating duplicates.
 *
 * <p>Uses a REAL {@link ObjectMapper} so the parse + serialize round-trip of
 * {@code assetsGenerated} is exercised end-to-end (the broader
 * {@code AssetStorageControllerTest} mocks the mapper).</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssetStorageController — replace-by-assetType dedup (GAP-1112 #2)")
class AssetStorageControllerDedupTest {

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private BrandingJobService brandingJobService;

    @Captor
    private ArgumentCaptor<String> assetsJsonCaptor;

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
        job.setStatus(JobStatus.COMPLETED);
    }

    private MockMultipartFile pngFile(String name) {
        return new MockMultipartFile("file", name, "image/png", "binary-data".getBytes());
    }

    @Test
    @DisplayName("Re-upload same assetType -> exactly 1 asset row remains (old deleted)")
    void reUploadSameAssetTypeReplacesPrevious() throws Exception {
        // Given: job already holds ONE LOGO asset
        String oldUrl = "https://cdn.kiteclass.com/instances/" + instanceId + "/branding/LOGO/logo_old.png";
        BrandingAsset existing = BrandingAsset.builder()
            .type("LOGO").variant("logo_old").url(oldUrl)
            .sizeBytes(111L).contentType("image/png").uploadedAt(1L).build();
        job.setAssetsGenerated(objectMapper.writeValueAsString(List.of(existing)));

        String newPath = "instances/" + instanceId + "/branding/LOGO/logo_new.png";
        String newUrl = "https://cdn.kiteclass.com/" + newPath;

        when(brandingJobService.getJobsByInstance(instanceId)).thenReturn(List.of(job));
        when(s3StorageService.generateAssetPath(eq(instanceId), eq("LOGO"), anyString())).thenReturn(newPath);
        when(s3StorageService.uploadAsset(any(), eq(newPath), eq("image/png"), anyLong())).thenReturn(newUrl);
        when(s3StorageService.getPresignedAssetUrl(newPath)).thenReturn(newUrl + "?X-Amz-Signature=x");

        // When: re-upload a LOGO
        ResponseEntity<BrandingAsset> response = controller.uploadAsset(instanceId, "LOGO", pngFile("logo.png"));

        // Then: the prior LOGO S3 object is deleted
        verify(s3StorageService).deleteAsset("instances/" + instanceId + "/branding/LOGO/logo_old.png");

        // And: persisted assetsGenerated holds exactly ONE LOGO (the new one)
        verify(brandingJobService).updateGeneratedAssets(eq(job.getId()), assetsJsonCaptor.capture());
        List<BrandingAsset> persisted = objectMapper.readValue(
            assetsJsonCaptor.getValue(), new TypeReference<List<BrandingAsset>>() {});

        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).getType()).isEqualTo("LOGO");
        assertThat(persisted.get(0).getUrl()).isEqualTo(newUrl);   // stored = stable URL, not presigned
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Upload different assetType -> both kept (no replacement)")
    void uploadDifferentAssetTypeKeepsBoth() throws Exception {
        // Given: job already holds ONE LOGO asset
        String logoUrl = "https://cdn.kiteclass.com/instances/" + instanceId + "/branding/LOGO/logo_1.png";
        BrandingAsset existingLogo = BrandingAsset.builder()
            .type("LOGO").variant("logo_1").url(logoUrl)
            .sizeBytes(111L).contentType("image/png").uploadedAt(1L).build();
        job.setAssetsGenerated(objectMapper.writeValueAsString(List.of(existingLogo)));

        String heroPath = "instances/" + instanceId + "/branding/HERO/hero_1.png";
        String heroUrl = "https://cdn.kiteclass.com/" + heroPath;

        when(brandingJobService.getJobsByInstance(instanceId)).thenReturn(List.of(job));
        when(s3StorageService.generateAssetPath(eq(instanceId), eq("HERO"), anyString())).thenReturn(heroPath);
        when(s3StorageService.uploadAsset(any(), eq(heroPath), eq("image/png"), anyLong())).thenReturn(heroUrl);
        when(s3StorageService.getPresignedAssetUrl(heroPath)).thenReturn(heroUrl + "?X-Amz-Signature=x");

        // When: upload a HERO (different type)
        controller.uploadAsset(instanceId, "HERO", pngFile("hero.png"));

        // Then: no asset deleted (different type)
        verify(s3StorageService, never()).deleteAsset(anyString());

        // And: both LOGO + HERO persisted
        verify(brandingJobService).updateGeneratedAssets(eq(job.getId()), assetsJsonCaptor.capture());
        List<BrandingAsset> persisted = objectMapper.readValue(
            assetsJsonCaptor.getValue(), new TypeReference<List<BrandingAsset>>() {});

        assertThat(persisted).hasSize(2);
        assertThat(persisted).extracting(BrandingAsset::getType)
            .containsExactlyInAnyOrder("LOGO", "HERO");
    }

    @Test
    @DisplayName("Re-upload same assetType 3x -> still exactly 1 asset row")
    void reUploadThreeTimesStillSingleAsset() throws Exception {
        // Start with no assets
        job.setAssetsGenerated(null);

        when(brandingJobService.getJobsByInstance(instanceId)).thenReturn(List.of(job));
        when(s3StorageService.generateAssetPath(eq(instanceId), eq("LOGO"), anyString()))
            .thenAnswer(inv -> "instances/" + instanceId + "/branding/LOGO/logo_" + System.nanoTime() + ".png");
        when(s3StorageService.uploadAsset(any(), anyString(), eq("image/png"), anyLong()))
            .thenAnswer(inv -> "https://cdn.kiteclass.com/" + inv.getArgument(1));
        when(s3StorageService.getPresignedAssetUrl(anyString())).thenReturn("https://signed");
        // Mirror persistence back onto the job so each subsequent upload sees prior state
        org.mockito.Mockito.doAnswer(inv -> {
            job.setAssetsGenerated(inv.getArgument(1));
            return null;
        }).when(brandingJobService).updateGeneratedAssets(eq(job.getId()), anyString());

        // When: upload LOGO three times
        for (int i = 0; i < 3; i++) {
            controller.uploadAsset(instanceId, "LOGO", pngFile("logo.png"));
        }

        // Then: the final persisted state has exactly 1 LOGO (never accumulates)
        List<BrandingAsset> persisted = objectMapper.readValue(
            job.getAssetsGenerated(), new TypeReference<List<BrandingAsset>>() {});
        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).getType()).isEqualTo("LOGO");
    }
}
