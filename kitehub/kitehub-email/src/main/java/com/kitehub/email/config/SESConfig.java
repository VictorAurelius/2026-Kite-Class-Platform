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

        /**
         * Reply-To address attached to every outbound message (per GAP-657
         * §Step 3 — Wave 98 Bucket B1). Default {@code support@kitehub.me}.
         * Tenant replies route to support inbox, NOT the no-reply sender.
         *
         * @since Wave 98 Bucket B1 (GAP-657)
         */
        private String replyToEmail = "support@kitehub.me";

        /**
         * Unsubscribe mailto address used in {@code List-Unsubscribe} header
         * (GAP-657 §Step 3). Default {@code unsubscribe@kitehub.me}.
         *
         * @since Wave 98 Bucket B1 (GAP-657)
         */
        private String unsubscribeMailto = "unsubscribe@kitehub.me";

        /**
         * One-click unsubscribe HTTPS endpoint included alongside the mailto
         * in the {@code List-Unsubscribe} header. Token placeholder
         * {@code {token}} expanded per-recipient by the renderer/dispatcher.
         *
         * @since Wave 98 Bucket B1 (GAP-657)
         */
        private String unsubscribeUrlTemplate = "https://kitehub.me/unsubscribe?token={token}";

        /**
         * Bounce-handling config — SNS topic that receives bounce notifications
         * from SES, consumed by the email service to mark addresses as
         * undeliverable. See {@code email-ses-setup-runbook.md} §Bounce/Complaint.
         *
         * @since Wave 33 Bucket B (GAP-370)
         */
        private Bounce bounce = new Bounce();

        /**
         * Complaint-handling config — SNS topic for spam complaints.
         *
         * @since Wave 33 Bucket B (GAP-370)
         */
        private Complaint complaint = new Complaint();

        /**
         * SES sending-rate caps used by the application-side limiter (in
         * addition to SES account-level limits).
         *
         * @since Wave 33 Bucket B (GAP-370)
         */
        private Rate rate = new Rate();

        @Data
        public static class Bounce {
            /** SNS topic ARN that SES publishes bounce notifications to. */
            private String topicArn;
        }

        @Data
        public static class Complaint {
            /** SNS topic ARN that SES publishes complaint notifications to. */
            private String topicArn;
        }

        @Data
        public static class Rate {
            /**
             * Application-side cap on emails/second. Must be ≤ the SES account
             * sending rate. Default 10/s — safe for a freshly-out-of-sandbox
             * account.
             */
            private int maxPerSecond = 10;

            /**
             * Daily cap on total emails. Default 50_000/day — matches the
             * baseline production tier; runbook covers warmup schedule.
             */
            private int maxPerDay = 50_000;
        }
    }
}
