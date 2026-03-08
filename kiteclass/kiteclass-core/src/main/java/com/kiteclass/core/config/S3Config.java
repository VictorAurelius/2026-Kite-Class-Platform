package com.kiteclass.core.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
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
 * <p>Compatible with both AWS S3 and MinIO (S3-compatible storage).
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final StorageProperties storageProperties;

    /**
     * Creates S3Client for object storage operations.
     *
     * <p>Configured for MinIO compatibility with path-style access.
     *
     * @return configured S3Client
     */
    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            storageProperties.getAccessKeyId(),
            storageProperties.getSecretAccessKey()
        );

        return S3Client.builder()
            .region(Region.of(storageProperties.getRegion()))
            .endpointOverride(URI.create(storageProperties.getEndpoint()))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .forcePathStyle(storageProperties.isPathStyleAccessEnabled())
            .build();
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
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            storageProperties.getAccessKeyId(),
            storageProperties.getSecretAccessKey()
        );

        return S3Presigner.builder()
            .region(Region.of(storageProperties.getRegion()))
            .endpointOverride(URI.create(storageProperties.getEndpoint()))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();
    }
}
