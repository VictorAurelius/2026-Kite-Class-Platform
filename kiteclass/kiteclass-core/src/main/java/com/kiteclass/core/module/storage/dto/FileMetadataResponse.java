package com.kiteclass.core.module.storage.dto;

import com.kiteclass.core.module.storage.constant.AccessLevel;
import com.kiteclass.core.module.storage.constant.FileType;
import com.kiteclass.core.module.storage.constant.StorageStatus;

import java.time.Instant;

/**
 * Response DTO for file metadata.
 *
 * <p>Contains complete file information:
 * <ul>
 *   <li>id: File database ID</li>
 *   <li>uploaderId: User who uploaded the file</li>
 *   <li>originalName: Original filename</li>
 *   <li>fileSize: File size in bytes</li>
 *   <li>mimeType: MIME type</li>
 *   <li>fileType: File classification</li>
 *   <li>accessLevel: Access control level</li>
 *   <li>status: Upload status (PENDING, CONFIRMED, EXPIRED, DELETED)</li>
 *   <li>createdAt: Upload timestamp</li>
 * </ul>
 *
 * @param id           File ID
 * @param uploaderId   Uploader user ID
 * @param originalName Original filename
 * @param fileSize     File size in bytes
 * @param mimeType     MIME type
 * @param fileType     File type classification
 * @param accessLevel  Access control level
 * @param status       Upload status
 * @param createdAt    Upload timestamp
 * @author KiteClass Team
 * @since 2.10.1
 */
public record FileMetadataResponse(
    Long id,
    Long uploaderId,
    String originalName,
    Long fileSize,
    String mimeType,
    FileType fileType,
    AccessLevel accessLevel,
    StorageStatus status,
    Instant createdAt
) {
}
