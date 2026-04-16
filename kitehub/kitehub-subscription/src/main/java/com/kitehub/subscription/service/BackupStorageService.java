package com.kitehub.subscription.service;

import com.kitehub.subscription.config.S3Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;

/**
 * Service for storing and retrieving database backups in S3/MinIO.
 * Supports mock mode for local development without real S3.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class BackupStorageService {

    private final S3Config s3Config;
    @Nullable
    private final S3Client s3Client;
    @Nullable
    private final S3Presigner s3Presigner;

    private static final Duration PRESIGN_EXPIRY = Duration.ofHours(1);

    public BackupStorageService(
            S3Config s3Config,
            @Nullable S3Client s3Client,
            @Nullable S3Presigner s3Presigner) {
        this.s3Config = s3Config;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Upload a backup file to S3/MinIO.
     *
     * @param key S3 object key (path)
     * @param data input stream of backup data
     * @param contentLength size in bytes
     */
    public void uploadBackup(String key, InputStream data, long contentLength) {
        if (s3Config.isMockMode()) {
            log.info("[MOCK] Would upload backup to s3://{}/{} ({} bytes)",
                s3Config.getBucket(), key, contentLength);
            return;
        }

        if (s3Client == null) {
            throw new IllegalStateException("S3Client is not configured");
        }

        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(s3Config.getBucket())
            .key(key)
            .contentLength(contentLength)
            .contentType("application/octet-stream")
            .build();

        s3Client.putObject(request, RequestBody.fromInputStream(data, contentLength));
        log.info("Uploaded backup to s3://{}/{} ({} bytes)", s3Config.getBucket(), key, contentLength);
    }

    /**
     * Download a backup file from S3/MinIO.
     *
     * @param key S3 object key
     * @return InputStream of the backup data
     */
    public InputStream downloadBackup(String key) {
        if (s3Config.isMockMode()) {
            throw new UnsupportedOperationException("Download not available in mock mode");
        }

        if (s3Client == null) {
            throw new IllegalStateException("S3Client is not configured");
        }

        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(s3Config.getBucket())
            .key(key)
            .build();

        return s3Client.getObject(request);
    }

    /**
     * Delete a backup file from S3/MinIO.
     *
     * @param key S3 object key
     */
    public void deleteBackup(String key) {
        if (s3Config.isMockMode()) {
            log.info("[MOCK] Would delete backup s3://{}/{}", s3Config.getBucket(), key);
            return;
        }

        if (s3Client == null) {
            throw new IllegalStateException("S3Client is not configured");
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
            .bucket(s3Config.getBucket())
            .key(key)
            .build();

        s3Client.deleteObject(request);
        log.info("Deleted backup s3://{}/{}", s3Config.getBucket(), key);
    }

    /**
     * Generate a presigned URL for downloading a backup.
     *
     * @param key S3 object key
     * @return Presigned URL valid for 1 hour
     */
    public String getBackupUrl(String key) {
        if (s3Config.isMockMode()) {
            return String.format("mock://s3/%s/%s", s3Config.getBucket(), key);
        }

        if (s3Presigner == null) {
            throw new IllegalStateException("S3Presigner is not configured");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(s3Config.getBucket())
            .key(key)
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(PRESIGN_EXPIRY)
            .getObjectRequest(getObjectRequest)
            .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
