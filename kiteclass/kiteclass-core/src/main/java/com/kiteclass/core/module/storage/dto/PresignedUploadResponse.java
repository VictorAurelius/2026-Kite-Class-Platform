package com.kiteclass.core.module.storage.dto;

import java.time.Instant;

/**
 * Response DTO for presigned upload URL generation.
 *
 * <p>Contains upload URL and metadata:
 * <ul>
 *   <li>fileId: Database ID of uploaded_files record</li>
 *   <li>uploadUrl: Presigned PUT URL for client to upload to S3</li>
 *   <li>expiresAt: URL expiration time (30 minutes from generation)</li>
 * </ul>
 *
 * <p>Client workflow:
 * <ol>
 *   <li>Call POST /api/v1/storage/upload-url → receive PresignedUploadResponse</li>
 *   <li>Upload file to uploadUrl using HTTP PUT</li>
 *   <li>Call POST /api/v1/storage/{fileId}/confirm to finalize upload</li>
 * </ol>
 *
 * @param fileId    Database ID of file record
 * @param uploadUrl Presigned S3 PUT URL
 * @param expiresAt URL expiration timestamp
 * @author KiteClass Team
 * @since 2.10.1
 */
public record PresignedUploadResponse(
    Long fileId,
    String uploadUrl,
    Instant expiresAt
) {
}
