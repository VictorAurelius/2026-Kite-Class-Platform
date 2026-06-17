package com.kiteclass.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Configuration for AWS S3 / MinIO client.
 *
 * <p>Provides:
 * <ul>
 *   <li>S3Client - for object operations (PutObject, GetObject, DeleteObject, HeadObject)</li>
 *   <li>S3Presigner - for generating presigned URLs (upload/download)</li>
 * </ul>
 *
 * <p>Configured via {@link StorageProperties} from application.yml.
 *
 * <p>Compatible with both AWS S3 and MinIO (S3-compatible storage). Credential and
 * endpoint resolution adapt to environment:
 * <ul>
 *   <li><b>MinIO / dev</b> — {@code access-key-id} + {@code secret-access-key} non-blank
 *       (e.g. {@code minioadmin}) → static credentials; {@code endpoint} non-blank
 *       (e.g. {@code http://localhost:9000}) → endpoint override.</li>
 *   <li><b>AWS S3 / production</b> — {@code access-key-id} blank → AWS default credential
 *       chain ({@link DefaultCredentialsProvider} = EC2 instance role / IAM); {@code endpoint}
 *       blank → SDK default regional endpoint ({@code https://s3.<region>.amazonaws.com}).</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final StorageProperties storageProperties;

    /**
     * Resolves the credential provider for S3 access.
     *
     * <p>When {@code access-key-id} is blank (production AWS), falls back to the AWS default
     * credential chain so the EC2 instance role / IAM profile is used — no static keys to
     * manage or leak. When non-blank (MinIO / dev), uses the static credentials verbatim.
     *
     * @return IAM-role-aware credential provider for production, static for MinIO
     */
    private AwsCredentialsProvider resolveCredentialsProvider() {
        String accessKeyId = storageProperties.getAccessKeyId();
        if (accessKeyId == null || accessKeyId.isBlank()) {
            log.info("S3 credentials: access-key-id blank -> using AWS default credential chain (IAM role)");
            return DefaultCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId, storageProperties.getSecretAccessKey())
        );
    }

    /**
     * Whether a custom endpoint override is configured.
     *
     * <p>Blank endpoint (production AWS S3) → SDK uses the default regional endpoint.
     * Non-blank (MinIO / dev) → endpoint override applied.
     */
    private boolean hasCustomEndpoint() {
        String endpoint = storageProperties.getEndpoint();
        return endpoint != null && !endpoint.isBlank();
    }

    /**
     * Creates S3Client for object storage operations.
     *
     * <p>Configured for MinIO compatibility with path-style access; on AWS S3 the
     * endpoint override is skipped (blank endpoint) so the SDK resolves the regional
     * endpoint from {@code region}.
     *
     * @return configured S3Client
     */
    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
            .region(Region.of(storageProperties.getRegion()))
            .credentialsProvider(resolveCredentialsProvider())
            .forcePathStyle(storageProperties.isPathStyleAccessEnabled());

        if (hasCustomEndpoint()) {
            builder.endpointOverride(URI.create(storageProperties.getEndpoint()));
        }

        return builder.build();
    }

    /**
     * Creates S3Presigner for generating presigned URLs.
     *
     * <p>Presigned URLs allow temporary direct upload/download to/from S3
     * without exposing AWS credentials to clients.
     *
     * @return configured S3Presigner
     */
    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
            .region(Region.of(storageProperties.getRegion()))
            .credentialsProvider(resolveCredentialsProvider());

        if (hasCustomEndpoint()) {
            builder.endpointOverride(URI.create(storageProperties.getEndpoint()));
        }

        return builder.build();
    }

    /**
     * Presigner for browser-facing presigned URLs (GAP-804 Bug #13).
     *
     * <p>Uses {@link StorageProperties#getPublicEndpoint()} so the signed host
     * resolves from a browser (e.g. http://localhost:9100), and forces path-style
     * access so the URL is {@code endpoint/bucket/key} — NOT {@code bucket.endpoint/key}
     * (virtual-host style does not resolve for localhost/MinIO).
     *
     * <p>On AWS S3 the public endpoint is blank → no override; presigned URLs are signed
     * against the default regional endpoint with path-style
     * ({@code https://s3.<region>.amazonaws.com/bucket/key}), which resolves in browsers.
     *
     * @return presigner that signs against the public endpoint with path-style URLs
     */
    @Bean("brandingPresigner")
    public S3Presigner brandingPresigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
            .region(Region.of(storageProperties.getRegion()))
            .credentialsProvider(resolveCredentialsProvider())
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build());

        String publicEndpoint = storageProperties.getPublicEndpoint();
        if (publicEndpoint != null && !publicEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(publicEndpoint));
        }

        return builder.build();
    }
}
