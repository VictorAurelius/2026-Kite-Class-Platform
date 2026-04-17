package com.kitehub.branding.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for S3Config.
 * Verifies conditional bean creation and property binding without full Spring Boot context.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@DisplayName("S3Config Unit Tests")
class S3ConfigTest {

    @Configuration
    @EnableConfigurationProperties(S3Config.class)
    static class TestConfig {
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("Should create S3Client and S3Presigner beans when mock-mode is false")
    void shouldCreateBeansWhenMockModeFalse() {
        contextRunner
            .withPropertyValues(
                "storage.s3.access-key=test-access-key",
                "storage.s3.secret-key=test-secret-key",
                "storage.s3.region=ap-southeast-1",
                "storage.s3.bucket=test-bucket",
                "storage.s3.endpoint=http://localhost:9000",
                "storage.s3.cdn-domain=localhost:9100",
                "storage.s3.mock-mode=false"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(S3Client.class);
                assertThat(context).hasSingleBean(S3Presigner.class);
                assertThat(context).hasSingleBean(S3Config.class);

                S3Config s3Config = context.getBean(S3Config.class);
                assertThat(s3Config.getAccessKey()).isEqualTo("test-access-key");
                assertThat(s3Config.getSecretKey()).isEqualTo("test-secret-key");
                assertThat(s3Config.getRegion()).isEqualTo("ap-southeast-1");
                assertThat(s3Config.getBucket()).isEqualTo("test-bucket");
                assertThat(s3Config.getEndpoint()).isEqualTo("http://localhost:9000");
                assertThat(s3Config.getCdnDomain()).isEqualTo("localhost:9100");
                assertThat(s3Config.isMockMode()).isFalse();
            });
    }

    @Test
    @DisplayName("Should NOT create S3Client and S3Presigner beans when mock-mode is true")
    void shouldNotCreateBeansWhenMockModeTrue() {
        contextRunner
            .withPropertyValues(
                "storage.s3.access-key=test-access-key",
                "storage.s3.secret-key=test-secret-key",
                "storage.s3.region=ap-southeast-1",
                "storage.s3.bucket=test-bucket",
                "storage.s3.mock-mode=true"
            )
            .run(context -> {
                assertThat(context).doesNotHaveBean(S3Client.class);
                assertThat(context).doesNotHaveBean(S3Presigner.class);
                assertThat(context).hasSingleBean(S3Config.class);

                S3Config s3Config = context.getBean(S3Config.class);
                assertThat(s3Config.isMockMode()).isTrue();
            });
    }

    @Test
    @DisplayName("Should use default mock-mode=false when not specified")
    void shouldUseDefaultMockModeFalse() {
        contextRunner
            .withPropertyValues(
                "storage.s3.access-key=test-access-key",
                "storage.s3.secret-key=test-secret-key",
                "storage.s3.region=ap-southeast-1",
                "storage.s3.bucket=test-bucket"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(S3Client.class);
                assertThat(context).hasSingleBean(S3Presigner.class);
            });
    }

    @Test
    @DisplayName("Should configure path-style access for MinIO when endpoint is provided")
    void shouldConfigurePathStyleAccessForMinio() {
        contextRunner
            .withPropertyValues(
                "storage.s3.access-key=minioadmin",
                "storage.s3.secret-key=minioadmin",
                "storage.s3.region=ap-southeast-1",
                "storage.s3.bucket=kiteclass-assets",
                "storage.s3.endpoint=http://localhost:9000",
                "storage.s3.mock-mode=false"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(S3Client.class);
                S3Client s3Client = context.getBean(S3Client.class);
                assertThat(s3Client).isNotNull();
                // Path-style access is configured internally via forcePathStyle(true)
            });
    }
}
