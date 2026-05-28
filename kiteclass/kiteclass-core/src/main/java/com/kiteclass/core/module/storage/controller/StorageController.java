package com.kiteclass.core.module.storage.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.storage.dto.FileMetadataResponse;
import com.kiteclass.core.module.storage.dto.PresignedUploadRequest;
import com.kiteclass.core.module.storage.dto.PresignedUploadResponse;
import com.kiteclass.core.module.storage.dto.QuotaUsageResponse;
import com.kiteclass.core.module.storage.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for File Storage operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>POST /api/v1/storage/upload-url - Generate presigned upload URL</li>
 *   <li>POST /api/v1/storage/{fileId}/confirm - Confirm file upload</li>
 *   <li>GET /api/v1/storage/{fileId}/download-url - Generate presigned download URL</li>
 *   <li>DELETE /api/v1/storage/{fileId} - Delete file</li>
 *   <li>GET /api/v1/storage/quota - Get storage quota usage</li>
 * </ul>
 *
 * <p>Upload workflow:
 * <ol>
 *   <li>Client calls POST /upload-url → receives presigned URL + fileId</li>
 *   <li>Client uploads file directly to S3 using presigned URL (HTTP PUT)</li>
 *   <li>Client calls POST /{fileId}/confirm to finalize upload</li>
 * </ol>
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Tag(name = "Storage", description = "File storage management APIs")
public class StorageController {

    private final StorageService storageService;

    /**
     * Generates presigned upload URL for client to upload file to S3.
     *
     * <p>Client workflow:
     * <ol>
     *   <li>Call this endpoint with file metadata</li>
     *   <li>Receive presigned PUT URL + fileId</li>
     *   <li>Upload file to presigned URL using HTTP PUT</li>
     *   <li>Call /confirm endpoint with fileId</li>
     * </ol>
     *
     * @param request  Upload request with file metadata
     * @param userId   User ID from X-User-Id header
     * @param tenantId Tenant ID from X-Tenant-Id header
     * @return ApiResponse with presigned upload URL and file ID
     */
    @PostMapping("/upload-url")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Generate presigned upload URL",
        description = "Generates a presigned S3 PUT URL for client to upload file directly to S3. " +
                      "Returns fileId to use for confirmation after upload."
    )
    public ApiResponse<PresignedUploadResponse> generateUploadUrl(
        @Valid @RequestBody PresignedUploadRequest request,
        @Parameter(description = "User ID (from Gateway)")
        @RequestHeader(value = "X-User-Reference-Id", required = true) Long userId,
        @Parameter(description = "Tenant instance ID")
        @RequestHeader(value = "X-Tenant-Id", required = true) UUID tenantId
    ) {
        log.info("REST request to generate upload URL for user: {}, tenant: {}, file: {}",
            userId, tenantId, request.fileName());

        PresignedUploadResponse response = storageService.generatePresignedUploadUrl(
            request, userId, tenantId
        );

        return ApiResponse.success(response, "Presigned upload URL generated successfully");
    }

    /**
     * Confirms successful file upload to S3.
     *
     * <p>Call this endpoint after uploading file to S3 via presigned URL.
     * This marks the file as CONFIRMED and updates quota usage.
     *
     * @param fileId File database ID
     * @return ApiResponse with file metadata
     */
    @PostMapping("/{fileId}/confirm")
    @Operation(
        summary = "Confirm file upload",
        description = "Confirms that file was successfully uploaded to S3. " +
                      "Marks file as CONFIRMED and updates storage quota."
    )
    public ApiResponse<FileMetadataResponse> confirmUpload(
        @Parameter(description = "File ID")
        @PathVariable Long fileId
    ) {
        log.info("REST request to confirm upload for file ID: {}", fileId);

        FileMetadataResponse response = storageService.confirmUpload(fileId);

        return ApiResponse.success(response, "File upload confirmed successfully");
    }

    /**
     * Generates presigned download URL for accessing file from S3.
     *
     * <p>Checks access control before generating URL:
     * <ul>
     *   <li>PUBLIC: Anyone can download</li>
     *   <li>PRIVATE: Only uploader can download</li>
     *   <li>TENANT: Anyone in same tenant can download</li>
     * </ul>
     *
     * @param fileId      File database ID
     * @param requesterId User requesting download
     * @param tenantId    Requester's tenant ID
     * @return ApiResponse with presigned download URL (5min TTL)
     */
    @GetMapping("/{fileId}/download-url")
    @Operation(
        summary = "Generate presigned download URL",
        description = "Generates a presigned S3 GET URL for downloading file. " +
                      "Access control is enforced (PUBLIC/PRIVATE/TENANT)."
    )
    public ApiResponse<String> generateDownloadUrl(
        @Parameter(description = "File ID")
        @PathVariable Long fileId,
        @Parameter(description = "User ID (from Gateway)")
        @RequestHeader(value = "X-User-Reference-Id", required = true) Long requesterId,
        @Parameter(description = "Tenant instance ID")
        @RequestHeader(value = "X-Tenant-Id", required = true) UUID tenantId
    ) {
        log.info("REST request to generate download URL for file: {}, requester: {}",
            fileId, requesterId);

        String downloadUrl = storageService.generatePresignedDownloadUrl(
            fileId, requesterId, tenantId
        );

        return ApiResponse.success(downloadUrl, "Presigned download URL generated successfully");
    }

    /**
     * Soft deletes a file.
     *
     * <p>File is marked as deleted and scheduled for S3 cleanup after 30 days.
     * Quota usage is updated immediately.
     *
     * @param fileId File database ID
     * @return ApiResponse with success message
     */
    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Delete file",
        description = "Soft deletes file. File is marked as deleted and will be removed from S3 after 30 days."
    )
    public ApiResponse<Void> deleteFile(
        @Parameter(description = "File ID")
        @PathVariable Long fileId
    ) {
        log.info("REST request to delete file ID: {}", fileId);

        storageService.deleteFile(fileId);

        return ApiResponse.success(null, "File deleted successfully");
    }

    /**
     * Gets storage quota usage for tenant.
     *
     * @param tenantId Tenant instance ID
     * @return ApiResponse with quota usage details
     */
    @GetMapping("/quota")
    @Operation(
        summary = "Get storage quota usage",
        description = "Returns storage quota information for tenant: tier, used bytes, quota bytes, remaining bytes."
    )
    public ApiResponse<QuotaUsageResponse> getQuotaUsage(
        @Parameter(description = "Tenant instance ID")
        @RequestHeader(value = "X-Tenant-Id", required = true) UUID tenantId
    ) {
        log.info("REST request to get quota usage for tenant: {}", tenantId);

        QuotaUsageResponse response = storageService.getQuotaUsage(tenantId);

        return ApiResponse.success(response, "Quota usage retrieved successfully");
    }
}
