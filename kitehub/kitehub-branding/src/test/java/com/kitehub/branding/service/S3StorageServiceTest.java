package com.kitehub.branding.service;

import com.kitehub.branding.config.S3Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for S3StorageService.
 * Verifies HTTP/HTTPS protocol selection based on CDN domain.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("S3StorageService Unit Tests")
class S3StorageServiceTest {

    @Mock
    private S3Config s3Config;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private S3StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new S3StorageService(s3Config, s3Client, s3Presigner);
    }

    @Test
    @DisplayName("Should use HTTP protocol for localhost CDN domain")
    void shouldUseHttpForLocalhost() {
        // Given
        when(s3Config.getCdnDomain()).thenReturn("localhost:9100");
        when(s3Config.isMockMode()).thenReturn(false);

        String path = "instances/" + UUID.randomUUID() + "/branding/LOGO/test.png";

        // When
        String url = storageService.getAssetUrl(path);

        // Then
        assertThat(url).startsWith("http://");
        assertThat(url).contains("localhost:9100");
        assertThat(url).contains(path);
    }

    @Test
    @DisplayName("Should use HTTPS protocol for production CDN domain")
    void shouldUseHttpsForProduction() {
        // Given
        when(s3Config.getCdnDomain()).thenReturn("cdn.kiteclass.com");
        when(s3Config.isMockMode()).thenReturn(false);

        String path = "instances/" + UUID.randomUUID() + "/branding/LOGO/test.png";

        // When
        String url = storageService.getAssetUrl(path);

        // Then
        assertThat(url).startsWith("https://");
        assertThat(url).contains("cdn.kiteclass.com");
        assertThat(url).contains(path);
    }

    @Test
    @DisplayName("Should use HTTP for localhost with port 9000")
    void shouldUseHttpForLocalhostWithDifferentPort() {
        // Given
        when(s3Config.getCdnDomain()).thenReturn("localhost:9000");
        when(s3Config.isMockMode()).thenReturn(false);

        String path = "test-path/file.jpg";

        // When
        String url = storageService.getAssetUrl(path);

        // Then
        assertThat(url).startsWith("http://");
        assertThat(url).contains("localhost:9000");
    }

    @Test
    @DisplayName("Should use HTTPS for custom production domain")
    void shouldUseHttpsForCustomProductionDomain() {
        // Given
        when(s3Config.getCdnDomain()).thenReturn("assets.example.com");
        when(s3Config.isMockMode()).thenReturn(false);

        String path = "test-path/file.jpg";

        // When
        String url = storageService.getAssetUrl(path);

        // Then
        assertThat(url).startsWith("https://");
        assertThat(url).contains("assets.example.com");
    }

    @Test
    @DisplayName("Should return mock URL when mock mode is enabled")
    void shouldReturnMockUrlWhenMockModeEnabled() {
        // Given
        when(s3Config.isMockMode()).thenReturn(true);

        String path = "test-path/file.jpg";

        // When
        String url = storageService.getAssetUrl(path);

        // Then
        assertThat(url).startsWith("https://mock-cdn.kiteclass.com/");
        assertThat(url).contains(path);
    }

    @Test
    @DisplayName("Should generate correct asset path with timestamp")
    void shouldGenerateCorrectAssetPathWithTimestamp() {
        // Given
        UUID instanceId = UUID.randomUUID();
        String assetType = "LOGO";
        String filename = "logo.png";

        // When
        String path = storageService.generateAssetPath(instanceId, assetType, filename);

        // Then
        assertThat(path).startsWith("instances/" + instanceId + "/branding/" + assetType + "/logo_");
        assertThat(path).endsWith(".png");
    }

    // ------------------------------------------------------------------
    // GAP-1112 #1 — presigned GET URL for browser-loadable preview
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getPresignedAssetUrl: mock mode returns deterministic mock URL")
    void presignedMockMode() {
        // Given
        when(s3Config.isMockMode()).thenReturn(true);
        String path = "instances/" + UUID.randomUUID() + "/branding/LOGO/logo_1.png";

        // When
        String url = storageService.getPresignedAssetUrl(path);

        // Then
        assertThat(url).startsWith("https://mock-cdn.kiteclass.com/");
        assertThat(url).contains(path);
    }

    @Test
    @DisplayName("getPresignedAssetUrl: real mode returns the signed URL from the presigner")
    void presignedRealMode() throws Exception {
        // Given
        when(s3Config.isMockMode()).thenReturn(false);
        when(s3Config.getBucket()).thenReturn("kite-branding");

        String signed = "https://minio.local/instances/abc/branding/LOGO/logo_1.png?X-Amz-Signature=deadbeef";
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL(signed));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
            .thenReturn(presignedRequest);

        // When
        String url = storageService.getPresignedAssetUrl("instances/abc/branding/LOGO/logo_1.png");

        // Then — signed, time-limited URL the browser can load directly
        assertThat(url).isEqualTo(signed);
        assertThat(url).contains("X-Amz-Signature");
    }

    @Test
    @DisplayName("getPresignedAssetUrl: falls back to raw asset URL when presigner unavailable")
    void presignedFallbackWhenPresignerNull() {
        // Given — service constructed without a presigner bean
        S3StorageService noPresigner = new S3StorageService(s3Config, s3Client, null);
        when(s3Config.isMockMode()).thenReturn(false);
        when(s3Config.getCdnDomain()).thenReturn("localhost:9100");

        // When
        String url = noPresigner.getPresignedAssetUrl("instances/abc/branding/LOGO/logo_1.png");

        // Then — graceful fallback to the (raw) CDN URL rather than throwing
        assertThat(url).startsWith("http://localhost:9100/");
        assertThat(url).contains("instances/abc/branding/LOGO/logo_1.png");
    }

    @Test
    @DisplayName("getPresignedAssetUrl: presigner failure falls back to raw asset URL")
    void presignedFallbackOnPresignerError() {
        // Given
        when(s3Config.isMockMode()).thenReturn(false);
        when(s3Config.getCdnDomain()).thenReturn("cdn.kiteclass.com");
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
            .thenThrow(new RuntimeException("presign boom"));

        // When
        String url = storageService.getPresignedAssetUrl("instances/abc/branding/LOGO/logo_1.png");

        // Then — exception swallowed, raw URL returned
        assertThat(url).startsWith("https://cdn.kiteclass.com/");
    }

    @Test
    @DisplayName("Should generate asset path with complex filename")
    void shouldGenerateAssetPathWithComplexFilename() {
        // Given
        UUID instanceId = UUID.randomUUID();
        String assetType = "HERO";
        String filename = "my-hero-image.webp";

        // When
        String path = storageService.generateAssetPath(instanceId, assetType, filename);

        // Then
        assertThat(path).contains("instances/" + instanceId + "/branding/" + assetType);
        assertThat(path).contains("my-hero-image_");
        assertThat(path).endsWith(".webp");
    }
}
