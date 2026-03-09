package com.kiteclass.core.module.storage.service;

import com.kiteclass.core.module.storage.dto.FileMetadataResponse;
import com.kiteclass.core.module.storage.dto.PresignedUploadRequest;
import com.kiteclass.core.module.storage.dto.PresignedUploadResponse;
import com.kiteclass.core.module.storage.dto.QuotaUsageResponse;

import java.util.UUID;

/**
 * Service interface for file storage operations.
 *
 * <p>Provides methods for:
 * <ul>
 *   <li>Generating presigned URLs for upload/download</li>
 *   <li>Confirming successful uploads</li>
 *   <li>Deleting files</li>
 *   <li>Managing storage quota</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
public interface StorageService {

    /**
     * Generates presigned upload URL for client to upload file to S3.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Validate file type against whitelist</li>
     *   <li>Check storage quota (fail if would exceed limit)</li>
     *   <li>Create UploadedFile record (status=PENDING, 30min TTL)</li>
     *   <li>Generate presigned PUT URL</li>
     *   <li>Return fileId + uploadUrl + expiresAt</li>
     * </ol>
     *
     * @param request   Upload request with file metadata
     * @param uploaderId Gateway user ID
     * @param tenantId   Tenant instance ID
     * @return Presigned upload response
     */
    PresignedUploadResponse generatePresignedUploadUrl(
        PresignedUploadRequest request,
        Long uploaderId,
        UUID tenantId
    );

    /**
     * Confirms successful file upload to S3.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Find file by ID (must be PENDING, not expired)</li>
     *   <li>Verify file exists in S3 (HeadObjectRequest)</li>
     *   <li>Update status = CONFIRMED, expiresAt = null</li>
     *   <li>Update quota usage (+fileSize)</li>
     * </ol>
     *
     * @param fileId File database ID
     * @return File metadata response
     */
    FileMetadataResponse confirmUpload(Long fileId);

    /**
     * Generates presigned download URL for accessing file from S3.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Find file by ID (must be CONFIRMED)</li>
     *   <li>Check access control (PUBLIC/PRIVATE/TENANT)</li>
     *   <li>Generate presigned GET URL (5min TTL)</li>
     * </ol>
     *
     * @param fileId      File database ID
     * @param requesterId User requesting download
     * @param tenantId    Tenant instance ID
     * @return Presigned download URL
     */
    String generatePresignedDownloadUrl(Long fileId, Long requesterId, UUID tenantId);

    /**
     * Soft deletes a file.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Find file by ID</li>
     *   <li>Soft delete (deleted=true, deletedAt=now, status=DELETED)</li>
     *   <li>Update quota usage immediately (-fileSize if CONFIRMED)</li>
     * </ol>
     *
     * <p>File remains in S3 for 30 days before cleanup.
     *
     * @param fileId File database ID
     */
    void deleteFile(Long fileId);

    /**
     * Gets storage quota usage for tenant.
     *
     * <p>If quota doesn't exist, creates FREE tier quota.
     *
     * @param tenantId Tenant instance ID
     * @return Quota usage response
     */
    QuotaUsageResponse getQuotaUsage(UUID tenantId);

    /**
     * Recalculates storage quota usage from database.
     *
     * <p>Sums fileSize of all CONFIRMED files for tenant.
     *
     * @param tenantId Tenant instance ID
     * @return Updated quota usage
     */
    QuotaUsageResponse recalculateQuotaUsage(UUID tenantId);
}
