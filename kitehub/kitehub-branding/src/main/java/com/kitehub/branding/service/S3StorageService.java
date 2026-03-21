package com.kitehub.branding.service;

import com.kitehub.branding.config.S3Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Service for S3 asset storage operations.
 * Supports mock mode for testing without real S3.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class S3StorageService {

    private final S3Config s3Config;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3StorageService(
        S3Config s3Config,
        @Autowired(required = false) S3Client s3Client,
        @Autowired(required = false) S3Presigner s3Presigner
    ) {
        this.s3Config = s3Config;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Upload asset to S3.
     *
     * @param inputStream File input stream
     * @param path S3 object key (path)
     * @param contentType MIME type
     * @param contentLength File size in bytes
     * @return Asset URL (CDN or S3)
     */
    public String uploadAsset(InputStream inputStream, String path, String contentType, long contentLength) {
        log.info("Uploading asset to S3: {}", path);

        if (s3Config.isMockMode()) {
            return mockUpload(path);
        }

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(s3Config.getBucket())
                .key(path)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));

            String url = getAssetUrl(path);
            log.info("Asset uploaded successfully: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Failed to upload asset to S3: {}", path, e);
            throw new RuntimeException("Failed to upload asset to S3", e);
        }
    }

    /**
     * Get asset URL (CDN or S3).
     *
     * @param path S3 object key
     * @return Asset URL
     */
    public String getAssetUrl(String path) {
        if (s3Config.isMockMode()) {
            return mockUrl(path);
        }

        // Use CDN domain if configured
        if (s3Config.getCdnDomain() != null && !s3Config.getCdnDomain().isEmpty()) {
            // Use http:// for localhost (MinIO), https:// for production CDN
            String protocol = s3Config.getCdnDomain().contains("localhost") ? "http" : "https";
            return String.format("%s://%s/%s", protocol, s3Config.getCdnDomain(), path);
        }

        // Generate presigned URL (valid for 1 hour)
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(s3Config.getBucket())
                .key(path)
                .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getRequest)
                .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", path, e);
            // Fallback to S3 URL
            return String.format("https://%s.s3.%s.amazonaws.com/%s",
                s3Config.getBucket(), s3Config.getRegion(), path);
        }
    }

    /**
     * Delete asset from S3.
     *
     * @param path S3 object key
     */
    public void deleteAsset(String path) {
        log.info("Deleting asset from S3: {}", path);

        if (s3Config.isMockMode()) {
            log.info("Mock mode: Asset deletion simulated for {}", path);
            return;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(s3Config.getBucket())
                .key(path)
                .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Asset deleted successfully: {}", path);
        } catch (Exception e) {
            log.error("Failed to delete asset from S3: {}", path, e);
            throw new RuntimeException("Failed to delete asset from S3", e);
        }
    }

    /**
     * Generate S3 path for instance branding asset.
     *
     * @param instanceId Instance UUID
     * @param assetType Asset type (profile, hero, logos, etc.)
     * @param filename Filename with extension
     * @return S3 object key
     */
    public String generateAssetPath(UUID instanceId, String assetType, String filename) {
        // Add timestamp for versioning
        long timestamp = Instant.now().getEpochSecond();
        return String.format("instances/%s/branding/%s/%s_%d.%s",
            instanceId, assetType, getFileNameWithoutExtension(filename), timestamp, getFileExtension(filename));
    }

    /**
     * Mock upload for testing.
     *
     * @param path S3 object key
     * @return Mock URL
     */
    private String mockUpload(String path) {
        log.info("Mock mode: Asset upload simulated for {}", path);
        return mockUrl(path);
    }

    /**
     * Generate mock URL for testing.
     *
     * @param path S3 object key
     * @return Mock URL
     */
    private String mockUrl(String path) {
        return String.format("https://mock-cdn.kiteclass.com/%s", path);
    }

    /**
     * Extract filename without extension.
     *
     * @param filename Filename
     * @return Filename without extension
     */
    private String getFileNameWithoutExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(0, lastDot) : filename;
    }

    /**
     * Extract file extension.
     *
     * @param filename Filename
     * @return File extension
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }
}
