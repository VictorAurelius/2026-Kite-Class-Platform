package com.kiteclass.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for S3/MinIO storage.
 *
 * <p>Loaded from application.yml under "storage.s3" prefix:
 * <pre>
 * storage:
 *   s3:
 *     endpoint: http://localhost:9000
 *     region: us-east-1
 *     access-key-id: minioadmin
 *     secret-access-key: minioadmin
 *     bucket-name: kiteclass-files
 *     path-style-access-enabled: true
 * </pre>
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "storage.s3")
public class StorageProperties {

    /**
     * S3/MinIO endpoint URL.
     * For MinIO: http://localhost:9000 (dev), http://minio:9000 (docker)
     * For AWS S3: https://s3.amazonaws.com or regional endpoint
     */
    private String endpoint;

    /**
     * Browser-reachable endpoint for presigned URLs (GAP-804 Bug #13).
     *
     * <p>{@link #endpoint} is the in-cluster address (e.g. http://kite-minio:9000)
     * used for server-side PutObject/GetObject. Presigned URLs handed to a browser
     * must point at a host the browser can resolve (e.g. http://localhost:9100 in
     * dev, or a public CDN/domain in prod). {@link #getPublicEndpoint()} falls back
     * to {@link #endpoint} when this is unset.
     */
    private String publicEndpoint;

    /**
     * AWS region.
     * Default: us-east-1 (required even for MinIO)
     */
    private String region = "us-east-1";

    /** Public endpoint for presigned URLs, falling back to {@link #endpoint}. */
    public String getPublicEndpoint() {
        return (publicEndpoint != null && !publicEndpoint.isBlank()) ? publicEndpoint : endpoint;
    }

    /**
     * S3/MinIO access key ID.
     */
    private String accessKeyId;

    /**
     * S3/MinIO secret access key.
     */
    private String secretAccessKey;

    /**
     * S3 bucket name for file storage.
     */
    private String bucketName;

    /**
     * Enable path-style access for MinIO compatibility.
     * MinIO requires path-style: http://endpoint/bucket/key
     * AWS S3 default: virtual-hosted-style: http://bucket.endpoint/key
     */
    private boolean pathStyleAccessEnabled = true;
}
