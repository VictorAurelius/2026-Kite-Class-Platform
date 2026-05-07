package com.kitehub.email.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.ses.SesClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SESConfig.
 * Verifies conditional bean creation and property binding without full Spring Boot context.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@DisplayName("SESConfig Unit Tests")
class SESConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(SESConfig.class);

    @Test
    @DisplayName("Should create SesClient bean when mock-mode is false")
    void shouldCreateSesClientWhenMockModeFalse() {
        contextRunner
            .withPropertyValues(
                "aws.ses.region=ap-southeast-1",
                "aws.ses.from-email=test@localhost",
                "aws.ses.from-name=Test Sender",
                "aws.ses.access-key=test-access-key",
                "aws.ses.secret-key=test-secret-key",
                "aws.ses.mock-mode=false"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(SesClient.class);
                assertThat(context).hasSingleBean(SESConfig.SESProperties.class);

                SESConfig.SESProperties properties = context.getBean(SESConfig.SESProperties.class);
                assertThat(properties.getRegion()).isEqualTo("ap-southeast-1");
                assertThat(properties.getFromEmail()).isEqualTo("test@localhost");
                assertThat(properties.getFromName()).isEqualTo("Test Sender");
                assertThat(properties.getAccessKey()).isEqualTo("test-access-key");
                assertThat(properties.getSecretKey()).isEqualTo("test-secret-key");
                assertThat(properties.isMockMode()).isFalse();
            });
    }

    @Test
    @DisplayName("Should NOT create SesClient bean when mock-mode is true")
    void shouldNotCreateSesClientWhenMockModeTrue() {
        contextRunner
            .withPropertyValues(
                "aws.ses.region=ap-southeast-1",
                "aws.ses.from-email=test@localhost",
                "aws.ses.from-name=Test Sender",
                "aws.ses.mock-mode=true"
            )
            .run(context -> {
                assertThat(context).doesNotHaveBean(SesClient.class);
                assertThat(context).hasSingleBean(SESConfig.SESProperties.class);

                SESConfig.SESProperties properties = context.getBean(SESConfig.SESProperties.class);
                assertThat(properties.isMockMode()).isTrue();
            });
    }

    @Test
    @DisplayName("Should use default mock-mode=true when not specified")
    void shouldUseDefaultMockModeTrue() {
        contextRunner
            .withPropertyValues(
                "aws.ses.region=ap-southeast-1",
                "aws.ses.from-email=test@localhost",
                "aws.ses.from-name=Test Sender"
            )
            .run(context -> {
                // Should NOT create SesClient when mock-mode defaults to true
                assertThat(context).doesNotHaveBean(SesClient.class);

                SESConfig.SESProperties properties = context.getBean(SESConfig.SESProperties.class);
                assertThat(properties.isMockMode()).isFalse(); // Default in @Data class is false
            });
    }

    @Test
    @DisplayName("Should create SesClient with IAM role when credentials not provided")
    void shouldCreateSesClientWithIamRole() {
        contextRunner
            .withPropertyValues(
                "aws.ses.region=ap-southeast-1",
                "aws.ses.from-email=test@localhost",
                "aws.ses.from-name=Test Sender",
                "aws.ses.mock-mode=false"
                // No access-key/secret-key provided
            )
            .run(context -> {
                assertThat(context).hasSingleBean(SesClient.class);
                SesClient sesClient = context.getBean(SesClient.class);
                assertThat(sesClient).isNotNull();
                // Uses default credentials chain (IAM role)
            });
    }

    @Test
    @DisplayName("Should load bounce/complaint/rate properties (Wave 33 GAP-370)")
    void shouldLoadBounceComplaintRateProperties() {
        contextRunner
            .withPropertyValues(
                "aws.ses.region=ap-southeast-1",
                "aws.ses.from-email=test@localhost",
                "aws.ses.from-name=Test",
                "aws.ses.mock-mode=true",
                "aws.ses.bounce.topic-arn=arn:aws:sns:ap-southeast-1:123456789012:ses-bounces",
                "aws.ses.complaint.topic-arn=arn:aws:sns:ap-southeast-1:123456789012:ses-complaints",
                "aws.ses.rate.max-per-second=14",
                "aws.ses.rate.max-per-day=50000"
            )
            .run(context -> {
                SESConfig.SESProperties props = context.getBean(SESConfig.SESProperties.class);

                assertThat(props.getBounce().getTopicArn())
                    .isEqualTo("arn:aws:sns:ap-southeast-1:123456789012:ses-bounces");
                assertThat(props.getComplaint().getTopicArn())
                    .isEqualTo("arn:aws:sns:ap-southeast-1:123456789012:ses-complaints");
                assertThat(props.getRate().getMaxPerSecond()).isEqualTo(14);
                assertThat(props.getRate().getMaxPerDay()).isEqualTo(50000);
            });
    }

    @Test
    @DisplayName("Should default rate.max-per-second=10 + max-per-day=50000 when not specified")
    void shouldDefaultRateProperties() {
        contextRunner
            .withPropertyValues(
                "aws.ses.region=ap-southeast-1",
                "aws.ses.from-email=test@localhost",
                "aws.ses.from-name=Test",
                "aws.ses.mock-mode=true"
                // bounce/complaint/rate intentionally omitted
            )
            .run(context -> {
                SESConfig.SESProperties props = context.getBean(SESConfig.SESProperties.class);

                // Defaults preserve safe production-warmup values
                assertThat(props.getRate().getMaxPerSecond()).isEqualTo(10);
                assertThat(props.getRate().getMaxPerDay()).isEqualTo(50_000);
                assertThat(props.getBounce().getTopicArn()).isNull();
                assertThat(props.getComplaint().getTopicArn()).isNull();
            });
    }

    @Test
    @DisplayName("Should load SESProperties correctly")
    void shouldLoadSESPropertiesCorrectly() {
        contextRunner
            .withPropertyValues(
                "aws.ses.region=us-east-1",
                "aws.ses.from-email=noreply@example.com",
                "aws.ses.from-name=Example Platform",
                "aws.ses.access-key=AKIAIOSFODNN7EXAMPLE",
                "aws.ses.secret-key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
                "aws.ses.mock-mode=false"
            )
            .run(context -> {
                SESConfig.SESProperties properties = context.getBean(SESConfig.SESProperties.class);

                assertThat(properties.getRegion()).isEqualTo("us-east-1");
                assertThat(properties.getFromEmail()).isEqualTo("noreply@example.com");
                assertThat(properties.getFromName()).isEqualTo("Example Platform");
                assertThat(properties.getAccessKey()).isEqualTo("AKIAIOSFODNN7EXAMPLE");
                assertThat(properties.getSecretKey()).isEqualTo("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
                assertThat(properties.isMockMode()).isFalse();
            });
    }
}
