package com.kitehub.branding.controller;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AssetStorageController.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssetStorageController Unit Tests")
class AssetStorageControllerTest {

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private BrandingJobService brandingJobService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AssetStorageController controller;

    private UUID instanceId;
    private BrandingJob brandingJob;

    @BeforeEach
    void setUp() {
        instanceId = UUID.randomUUID();

        brandingJob = new BrandingJob();
        brandingJob.setId(UUID.randomUUID());
        brandingJob.setInstanceId(instanceId);
        brandingJob.setOrganizationName("Test Org");
        brandingJob.setLanguage("en");
        brandingJob.setStatus(JobStatus.COMPLETED);
        brandingJob.setProgress(100);
        brandingJob.setCreatedAt(LocalDateTime.now());
        brandingJob.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should upload asset successfully")
    void shouldUploadAssetSuccessfully() throws Exception {
        // Given
        String assetType = "profile";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "cutout.png",
            "image/png",
            "image data".getBytes()
        );

        String expectedPath = "instances/" + instanceId + "/branding/profile/cutout_123456.png";
        String expectedUrl = "https://cdn.kiteclass.com/" + expectedPath;

        when(s3StorageService.generateAssetPath(eq(instanceId), eq(assetType), anyString()))
            .thenReturn(expectedPath);
        when(s3StorageService.uploadAsset(any(), eq(expectedPath), eq("image/png"), anyLong()))
            .thenReturn(expectedUrl);
        when(brandingJobService.getJobsByInstance(instanceId))
            .thenReturn(Collections.singletonList(brandingJob));

        // When
        ResponseEntity<BrandingAsset> response = controller.uploadAsset(instanceId, assetType, file);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo(assetType);
        assertThat(response.getBody().getUrl()).isEqualTo(expectedUrl);
        assertThat(response.getBody().getSizeBytes()).isEqualTo(file.getSize());

        verify(s3StorageService).uploadAsset(any(), eq(expectedPath), eq("image/png"), anyLong());
    }

    @Test
    @DisplayName("Should get assets from BrandingJob")
    @SuppressWarnings("unchecked")
    void shouldGetAssetsFromBrandingJob() throws Exception {
        // Given
        List<BrandingAsset> expectedAssets = new ArrayList<>();
        expectedAssets.add(BrandingAsset.builder()
            .type("profile")
            .variant("cutout")
            .url("https://cdn.kiteclass.com/instances/" + instanceId + "/profile.png")
            .sizeBytes(125000L)
            .contentType("image/png")
            .uploadedAt(System.currentTimeMillis() / 1000)
            .build());

        String assetsJson = "[{\"type\":\"profile\",\"variant\":\"cutout\"}]";
        brandingJob.setAssetsGenerated(assetsJson);

        when(brandingJobService.getJobsByInstance(instanceId))
            .thenReturn(Collections.singletonList(brandingJob));
        when(objectMapper.readValue(eq(assetsJson), any(com.fasterxml.jackson.core.type.TypeReference.class)))
            .thenReturn(expectedAssets);

        // When
        ResponseEntity<List<BrandingAsset>> response = controller.getAssets(instanceId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getType()).isEqualTo("profile");

        verify(brandingJobService).getJobsByInstance(instanceId);
    }

    @Test
    @DisplayName("Should return empty list when no jobs exist")
    void shouldReturnEmptyListWhenNoJobsExist() {
        // Given
        when(brandingJobService.getJobsByInstance(instanceId))
            .thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<BrandingAsset>> response = controller.getAssets(instanceId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when no assets generated")
    void shouldReturnEmptyListWhenNoAssetsGenerated() {
        // Given
        brandingJob.setAssetsGenerated(null);

        when(brandingJobService.getJobsByInstance(instanceId))
            .thenReturn(Collections.singletonList(brandingJob));

        // When
        ResponseEntity<List<BrandingAsset>> response = controller.getAssets(instanceId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("Should delete assets from S3 and clear BrandingJob")
    @SuppressWarnings("unchecked")
    void shouldDeleteAssetsFromS3AndClearBrandingJob() throws Exception {
        // Given
        List<BrandingAsset> assets = new ArrayList<>();
        assets.add(BrandingAsset.builder()
            .type("profile")
            .variant("cutout")
            .url("https://cdn.kiteclass.com/instances/" + instanceId + "/branding/profile/cutout.png")
            .build());
        assets.add(BrandingAsset.builder()
            .type("hero")
            .variant("variant1")
            .url("https://cdn.kiteclass.com/instances/" + instanceId + "/branding/hero/variant1.jpg")
            .build());

        String assetsJson = "[{\"type\":\"profile\"},{\"type\":\"hero\"}]";
        brandingJob.setAssetsGenerated(assetsJson);

        when(brandingJobService.getJobsByInstance(instanceId))
            .thenReturn(Collections.singletonList(brandingJob));
        when(objectMapper.readValue(eq(assetsJson), any(com.fasterxml.jackson.core.type.TypeReference.class)))
            .thenReturn(assets);

        // When
        ResponseEntity<Map<String, String>> response = controller.deleteAssets(instanceId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(s3StorageService, times(2)).deleteAsset(anyString());
        verify(brandingJobService).updateGeneratedAssets(eq(brandingJob.getId()), isNull());
    }

    @Test
    @DisplayName("Should handle deletion when no assets exist")
    void shouldHandleDeletionWhenNoAssetsExist() {
        // Given
        brandingJob.setAssetsGenerated(null);

        when(brandingJobService.getJobsByInstance(instanceId))
            .thenReturn(Collections.singletonList(brandingJob));

        // When
        ResponseEntity<Map<String, String>> response = controller.deleteAssets(instanceId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(s3StorageService, never()).deleteAsset(anyString());
    }

    @Test
    @DisplayName("Should continue deletion even if S3 delete fails")
    @SuppressWarnings("unchecked")
    void shouldContinueDeletionEvenIfS3DeleteFails() throws Exception {
        // Given
        List<BrandingAsset> assets = new ArrayList<>();
        assets.add(BrandingAsset.builder()
            .type("profile")
            .url("https://cdn.kiteclass.com/instances/" + instanceId + "/branding/profile/cutout.png")
            .build());

        String assetsJson = "[{\"type\":\"profile\"}]";
        brandingJob.setAssetsGenerated(assetsJson);

        when(brandingJobService.getJobsByInstance(instanceId))
            .thenReturn(Collections.singletonList(brandingJob));
        when(objectMapper.readValue(eq(assetsJson), any(com.fasterxml.jackson.core.type.TypeReference.class)))
            .thenReturn(assets);

        doThrow(new RuntimeException("S3 error")).when(s3StorageService).deleteAsset(anyString());

        // When
        ResponseEntity<Map<String, String>> response = controller.deleteAssets(instanceId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(brandingJobService).updateGeneratedAssets(eq(brandingJob.getId()), isNull());
    }
}
