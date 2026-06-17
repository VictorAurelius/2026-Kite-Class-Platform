package com.kitehub.subscription.config;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Configuration for AWS S3 / MinIO storage for database backups.
 * Reuses the same pattern as kitehub-branding S3Config.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "storage.s3")
@Data
public class S3Config {

    private String accessKey;
    private String secretKey;
    private String region;
    private String bucket;
    private String endpoint;
    private boolean mockMode = false;

    /**
     * Create S3Client bean.
     * Only created when mock-mode is false (or not specified).
     *
     * @return Configured S3Client
     */
    @Bean
    @ConditionalOnProperty(name = "storage.s3.mock-mode", havingValue = "false", matchIfMissing = true)
    public S3Client s3Client() {
        software.amazon.awssdk.services.s3.S3ClientBuilder builder = S3Client.builder()
            .region(Region.of(region));

        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint))
                   .forcePathStyle(true);
        }

        // Only use static credentials when BOTH are set + non-blank (MinIO/LocalStack).
        // Blank → leave default credentials provider chain (EC2 instance role / IAM)
        // so production S3 works without static keys. Matches kiteclass-core S3Config.
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }

        return builder.build();
    }

    /**
     * Create S3Presigner bean for presigned URLs.
     * Only created when mock-mode is false (or not specified).
     *
     * @return Configured S3Presigner
     */
    @Bean
    @ConditionalOnProperty(name = "storage.s3.mock-mode", havingValue = "false", matchIfMissing = true)
    public S3Presigner s3Presigner() {
        software.amazon.awssdk.services.s3.S3Configuration s3Configuration =
            software.amazon.awssdk.services.s3.S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();

        S3Presigner.Builder builder = S3Presigner.builder()
            .region(Region.of(region))
            .serviceConfiguration(s3Configuration);

        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        // Only use static credentials when BOTH are set + non-blank (MinIO/LocalStack).
        // Blank → leave default credentials provider chain (EC2 instance role / IAM)
        // so production S3 works without static keys. Matches kiteclass-core S3Config.
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }

        return builder.build();
    }
}
