package com.kitehub.branding.config;

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
 * Configuration for AWS S3 storage.
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
    private String endpoint; // For MinIO/LocalStack
    private String cdnDomain;
    // Browser-reachable endpoint for presigned GET URLs. The internal `endpoint`
    // (kite-minio:9000) is unreachable from the host browser (ERR_NAME_NOT_RESOLVED),
    // so presign against the mapped host port instead (e.g. http://localhost:9100).
    // Blank → falls back to `endpoint`.
    private String publicEndpoint;
    private boolean mockMode = false; // For testing without real S3

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

        // Use custom endpoint if provided (MinIO/LocalStack)
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint))
                   .forcePathStyle(true); // Required for MinIO/LocalStack
        }

        // Set credentials
        if (accessKey != null && secretKey != null) {
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

        // S3Presigner uses S3Configuration to enable path-style access
        software.amazon.awssdk.services.s3.S3Configuration s3Config =
            software.amazon.awssdk.services.s3.S3Configuration.builder()
                .pathStyleAccessEnabled(true) // Required for MinIO/LocalStack
                .build();

        software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder builder = S3Presigner.builder()
            .region(Region.of(region))
            .serviceConfiguration(s3Config);

        // Presigned URLs are handed to the browser, so sign them against the
        // host-reachable endpoint when configured (internal kite-minio:9000 is
        // unresolvable from the host). Signature binds the Host header, so it must
        // match the URL the browser actually requests.
        String presignEndpoint = (publicEndpoint != null && !publicEndpoint.isEmpty())
                ? publicEndpoint : endpoint;
        if (presignEndpoint != null && !presignEndpoint.isEmpty()) {
            builder.endpointOverride(URI.create(presignEndpoint));
        }

        if (accessKey != null && secretKey != null) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }

        return builder.build();
    }
}
