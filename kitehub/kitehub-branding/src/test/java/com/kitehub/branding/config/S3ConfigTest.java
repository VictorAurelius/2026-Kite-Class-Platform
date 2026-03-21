package com.kitehub.branding.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for S3Config.
 * Verifies path-style access configuration for MinIO compatibility.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "storage.s3.access-key=test-access-key",
    "storage.s3.secret-key=test-secret-key",
    "storage.s3.region=ap-southeast-1",
    "storage.s3.bucket=test-bucket",
    "storage.s3.endpoint=http://localhost:9000",
    "storage.s3.cdn-domain=localhost:9100",
    "storage.s3.mock-mode=false"
})
@DisplayName("S3Config Integration Tests")
class S3ConfigTest {

    @Autowired(required = false)
    private S3Client s3Client;

    @Autowired(required = false)
    private S3Presigner s3Presigner;

    @Autowired
    private S3Config s3Config;

    @Test
    @DisplayName("Should create S3Client bean when mock-mode is false")
    void shouldCreateS3ClientWhenMockModeFalse() {
        assertThat(s3Client).isNotNull();
    }

    @Test
    @DisplayName("Should create S3Presigner bean when mock-mode is false")
    void shouldCreateS3PresignerWhenMockModeFalse() {
        assertThat(s3Presigner).isNotNull();
    }

    @Test
    @DisplayName("Should load S3Config properties correctly")
    void shouldLoadS3ConfigPropertiesCorrectly() {
        assertThat(s3Config.getAccessKey()).isEqualTo("test-access-key");
        assertThat(s3Config.getSecretKey()).isEqualTo("test-secret-key");
        assertThat(s3Config.getRegion()).isEqualTo("ap-southeast-1");
        assertThat(s3Config.getBucket()).isEqualTo("test-bucket");
        assertThat(s3Config.getEndpoint()).isEqualTo("http://localhost:9000");
        assertThat(s3Config.getCdnDomain()).isEqualTo("localhost:9100");
        assertThat(s3Config.isMockMode()).isFalse();
    }
}

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "storage.s3.mock-mode=true"
})
@DisplayName("S3Config Mock Mode Tests")
class S3ConfigMockModeTest {

    @Autowired(required = false)
    private S3Client s3Client;

    @Autowired(required = false)
    private S3Presigner s3Presigner;

    @Test
    @DisplayName("Should NOT create S3Client bean when mock-mode is true")
    void shouldNotCreateS3ClientWhenMockModeTrue() {
        assertThat(s3Client).isNull();
    }

    @Test
    @DisplayName("Should NOT create S3Presigner bean when mock-mode is true")
    void shouldNotCreateS3PresignerWhenMockModeTrue() {
        assertThat(s3Presigner).isNull();
    }
}
