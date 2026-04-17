package com.kitehub.email.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * AWS SES configuration.
 *
 * @since 1.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SESConfig.SESProperties.class)
public class SESConfig {

    /**
     * Create SES client bean (only when not in mock mode).
     * Default behavior when property not specified depends on application.yml default.
     */
    @Bean
    @ConditionalOnProperty(name = "aws.ses.mock-mode", havingValue = "false", matchIfMissing = false)
    public SesClient sesClient(SESProperties properties) {
        log.info("Initializing AWS SES client for region: {}", properties.getRegion());

        software.amazon.awssdk.services.ses.SesClientBuilder builder = SesClient.builder()
                .region(Region.of(properties.getRegion()));

        // Use credentials if provided (for local dev)
        if (properties.getAccessKey() != null && !properties.getAccessKey().isEmpty()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                    properties.getAccessKey(),
                    properties.getSecretKey()
            );
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
            log.info("Using static credentials for SES");
        } else {
            log.info("Using default credentials chain for SES (IAM role)");
        }

        return builder.build();
    }

    /**
     * SES configuration properties.
     */
    @Data
    @ConfigurationProperties(prefix = "aws.ses")
    public static class SESProperties {
        private String region;
        private String fromEmail;
        private String fromName;
        private boolean mockMode;
        private String accessKey;
        private String secretKey;
    }
}
