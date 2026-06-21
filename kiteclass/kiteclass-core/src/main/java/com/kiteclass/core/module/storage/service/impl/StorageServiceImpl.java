package com.kiteclass.core.module.storage.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.config.StorageProperties;
import com.kiteclass.core.module.storage.constant.StorageStatus;
import com.kiteclass.core.module.storage.constant.StorageTier;
import com.kiteclass.core.module.storage.dto.FileMetadataResponse;
import com.kiteclass.core.module.storage.dto.PresignedUploadRequest;
import com.kiteclass.core.module.storage.dto.PresignedUploadResponse;
import com.kiteclass.core.module.storage.dto.QuotaUsageResponse;
import com.kiteclass.core.module.storage.entity.StorageQuota;
import com.kiteclass.core.module.storage.entity.UploadedFile;
import com.kiteclass.core.module.storage.mapper.StorageMapper;
import com.kiteclass.core.module.storage.repository.StorageQuotaRepository;
import com.kiteclass.core.module.storage.repository.UploadedFileRepository;
import com.kiteclass.core.module.storage.service.LessonMaterialAccessGuard;
import com.kiteclass.core.module.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of StorageService interface.
 *
 * <p>Handles:
 * <ul>
 *   <li>Presigned URL generation for S3 upload/download</li>
 *   <li>File type validation (whitelist check)</li>
 *   <li>Storage quota enforcement</li>
 *   <li>Multi-tenant file isolation</li>
 *   <li>Access control (PUBLIC/PRIVATE/TENANT)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.validation.annotation.Validated
public class StorageServiceImpl implements StorageService {

    private final UploadedFileRepository uploadedFileRepository;
    private final StorageQuotaRepository storageQuotaRepository;
    private final StorageMapper storageMapper;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    /**
     * GAP-1307 cross-module LMS enrollment paywall hook, injected OPTIONALLY via
     * {@link ObjectProvider}. A sliced Spring context that scans storage but not LMS (no
     * provider value) still loads and simply skips the paywall — unlike the reverted #2416
     * attempt which hard-required the bean and broke full/sliced context loads
     * (e.g. {@code OpenApiSpecExportTest}). Production full context resolves the LMS-side
     * {@code LessonMaterialAccessGuardImpl} → paywall enforced.
     */
    private final ObjectProvider<LessonMaterialAccessGuard> lessonMaterialAccessGuardProvider;

    // Constants
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(30);
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(5);
    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024; // 100 MB

    /**
     * Whitelist of allowed MIME types.
     * Prevents upload of potentially dangerous files (executables, scripts).
     *
     * <p>GAP-1527 / GAP-1489 (OWASP A05 — stored-XSS): {@code image/svg+xml} removed.
     * SVG is active content (inline {@code <script>} / {@code onload}) and this is a
     * presigned-URL flow where the server never sees the bytes, so it cannot magic-byte
     * sniff or sanitize. Mirrors the branding allowlist hardening (GAP-1037). Tenants
     * needing vector logos go through the sanitized branding path, not generic storage.
     */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        // Images (raster only — no svg+xml, see class-level GAP-1489 note)
        "image/jpeg", "image/png", "image/gif", "image/webp",
        // Documents
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain", "text/csv",
        // Videos
        "video/mp4", "video/mpeg", "video/webm",
        // Audio
        "audio/mpeg", "audio/wav", "audio/ogg"
    );

    @Override
    @Transactional
    public PresignedUploadResponse generatePresignedUploadUrl(
        PresignedUploadRequest request,
        Long uploaderId,
        UUID tenantId
    ) {
        log.info("Generating presigned upload URL for user: {}, tenant: {}, file: {}",
            uploaderId, tenantId, request.fileName());

        // Validate file type
        validateFileType(request.mimeType());

        // Validate file size
        if (request.fileSize() > MAX_FILE_SIZE) {
            log.warn("File size exceeds maximum: {} bytes (max: {})", request.fileSize(), MAX_FILE_SIZE);
            throw new BusinessException("FILE_SIZE_EXCEEDS_MAXIMUM",
                HttpStatus.BAD_REQUEST, request.fileSize(), MAX_FILE_SIZE);
        }

        // Check quota
        ensureQuotaAvailable(tenantId, request.fileSize());

        // Generate storage path: {instanceId}/uploads/{year}/{month}/{uuid}.ext
        String storagePath = generateStoragePath(tenantId, request.fileName());
        log.debug("Generated storage path: {}", storagePath);

        // Create UploadedFile record (status=PENDING)
        UploadedFile file = UploadedFile.builder()
            .uploaderId(uploaderId)
            .fileType(request.fileType())
            .originalName(request.fileName())
            .storagePath(storagePath)
            .fileSize(request.fileSize())
            .mimeType(request.mimeType())
            .accessLevel(request.accessLevel())
            .status(StorageStatus.PENDING)
            .expiresAt(Instant.now().plus(UPLOAD_URL_TTL))
            .build();
        file.setInstanceId(tenantId);

        UploadedFile saved = uploadedFileRepository.save(file);
        log.info("Created PENDING upload file record with ID: {}", saved.getId());

        // Update quota immediately (PENDING files count towards quota to prevent abuse)
        updateQuotaUsage(tenantId, request.fileSize(), true);

        // Generate presigned PUT URL
        String uploadUrl = generatePresignedPutUrl(storagePath);

        return new PresignedUploadResponse(
            saved.getId(),
            uploadUrl,
            saved.getExpiresAt()
        );
    }

    @Override
    @Transactional
    public FileMetadataResponse confirmUpload(Long fileId, Long requesterId, boolean privileged) {
        log.info("Confirming upload for file ID: {} by requester: {}", fileId, requesterId);

        // Find file (must be PENDING). Hibernate tenantFilter scopes this to the caller's
        // tenant, so cross-tenant access already returns empty → 404.
        UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(fileId)
            .orElseThrow(() -> new EntityNotFoundException("FILE_NOT_FOUND", (Object) fileId));

        // GAP-1309: per-resource ownership authz (intra-tenant IDOR guard) — checked BEFORE
        // status/S3 work so a non-owner is rejected without side effects.
        verifyFileOwnership(file, requesterId, privileged);

        if (!file.isPending()) {
            log.warn("File {} is not PENDING (status: {})", fileId, file.getStatus());
            throw new BusinessException("FILE_NOT_PENDING", HttpStatus.CONFLICT, fileId);
        }

        if (file.isExpired()) {
            log.warn("File {} has expired (expiresAt: {})", fileId, file.getExpiresAt());
            throw new BusinessException("FILE_UPLOAD_EXPIRED", HttpStatus.GONE, fileId);
        }

        // Verify file exists in S3
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                .bucket(storageProperties.getBucketName())
                .key(file.getStoragePath())
                .build());
        } catch (NoSuchKeyException e) {
            log.error("File not found in S3: {}", file.getStoragePath());
            throw new BusinessException("FILE_NOT_FOUND_IN_S3", HttpStatus.NOT_FOUND, file.getStoragePath());
        }

        // Confirm upload (quota already updated when PENDING file was created)
        file.confirmUpload();
        UploadedFile confirmed = uploadedFileRepository.save(file);

        log.info("Confirmed upload for file ID: {}, size: {} bytes", fileId, file.getFileSize());
        return storageMapper.toMetadataResponse(confirmed);
    }

    @Override
    @Transactional(readOnly = true)
    public String generatePresignedDownloadUrl(Long fileId, Long requesterId, UUID tenantId, boolean elevatedRole) {
        log.info("Generating presigned download URL for file: {}, requester: {}", fileId, requesterId);

        // Find file (must be CONFIRMED)
        UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(fileId)
            .orElseThrow(() -> new EntityNotFoundException("FILE_NOT_FOUND", (Object) fileId));

        if (!file.isConfirmed()) {
            log.warn("File {} is not CONFIRMED (status: {})", fileId, file.getStatus());
            throw new BusinessException("FILE_NOT_CONFIRMED", HttpStatus.CONFLICT, fileId);
        }

        // Check access control (visibility model: PUBLIC / PRIVATE / TENANT)
        checkAccessPermission(file, requesterId, tenantId);

        // GAP-1307: LMS enrollment paywall — when the file backs a paid (non-trial) lesson,
        // a non-enrolled student must not bypass the paywall via the TENANT-scoped storage path.
        // Guard is OPTIONAL (ObjectProvider): absent in a sliced context lacking LMS → allow
        // (no paywall in that context); present in production full context → enforced.
        LessonMaterialAccessGuard guard = lessonMaterialAccessGuardProvider.getIfAvailable();
        if (guard != null) {
            guard.verifyLessonMaterialDownloadAccess(
                file.getId(), file.getUploaderId(), requesterId, elevatedRole);
        }

        // Generate presigned GET URL
        software.amazon.awssdk.services.s3.model.GetObjectRequest getObjectRequest =
            software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                .bucket(storageProperties.getBucketName())
                .key(file.getStoragePath())
                .build();

        software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest presignedRequest =
            s3Presigner.presignGetObject(software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.builder()
                .signatureDuration(DOWNLOAD_URL_TTL)
                .getObjectRequest(getObjectRequest)
                .build());

        String downloadUrl = presignedRequest.url().toString();
        log.debug("Generated presigned download URL for file: {}", fileId);

        return downloadUrl;
    }

    @Override
    @Transactional
    public void deleteFile(Long fileId, Long requesterId, boolean privileged) {
        log.info("Deleting file ID: {} by requester: {}", fileId, requesterId);

        UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(fileId)
            .orElseThrow(() -> new EntityNotFoundException("FILE_NOT_FOUND", (Object) fileId));

        // GAP-1309: per-resource ownership authz (intra-tenant IDOR guard).
        verifyFileOwnership(file, requesterId, privileged);

        // Update quota immediately if file was CONFIRMED
        if (file.isConfirmed()) {
            updateQuotaUsage(file.getInstanceId(), file.getFileSize(), false);
        }

        // Soft delete
        file.softDelete();
        uploadedFileRepository.save(file);

        log.info("Soft deleted file ID: {}, size: {} bytes", fileId, file.getFileSize());
    }

    @Override
    @Transactional(readOnly = true)
    public QuotaUsageResponse getQuotaUsage(UUID tenantId) {
        log.debug("Getting quota usage for tenant: {}", tenantId);

        StorageQuota quota = storageQuotaRepository.findByInstanceId(tenantId)
            .orElseGet(() -> createDefaultQuota(tenantId));

        return storageMapper.toQuotaResponse(quota);
    }

    @Override
    @Transactional
    public QuotaUsageResponse recalculateQuotaUsage(UUID tenantId) {
        log.info("Recalculating quota usage for tenant: {}", tenantId);

        StorageQuota quota = storageQuotaRepository.findByInstanceId(tenantId)
            .orElseGet(() -> createDefaultQuota(tenantId));

        // Calculate actual usage from database
        Long actualUsage = uploadedFileRepository.calculateUsedBytes(tenantId);
        quota.setUsedBytesAndRecalculate(actualUsage != null ? actualUsage : 0L);

        StorageQuota updated = storageQuotaRepository.save(quota);
        log.info("Recalculated quota for tenant: {}, usedBytes: {}", tenantId, updated.getUsedBytes());

        return storageMapper.toQuotaResponse(updated);
    }

    // === Private Helper Methods ===

    /**
     * Validates file type against whitelist.
     *
     * @param mimeType MIME type to validate
     * @throws BusinessException if MIME type not allowed
     */
    private void validateFileType(String mimeType) {
        if (!ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            log.warn("File type not allowed: {}", mimeType);
            throw new BusinessException("FILE_TYPE_NOT_ALLOWED", HttpStatus.BAD_REQUEST, mimeType);
        }
    }

    /**
     * Checks if tenant has enough quota for file upload.
     *
     * @param tenantId Tenant instance ID
     * @param fileSize File size in bytes
     * @throws BusinessException if quota exceeded
     */
    private void ensureQuotaAvailable(UUID tenantId, long fileSize) {
        StorageQuota quota = storageQuotaRepository.findByInstanceId(tenantId)
            .orElseGet(() -> createDefaultQuota(tenantId));

        if (quota.wouldExceedQuota(fileSize)) {
            log.warn("Quota exceeded for tenant: {}, available: {} bytes, required: {} bytes",
                tenantId, quota.getRemainingBytes(), fileSize);
            throw new BusinessException("STORAGE_QUOTA_EXCEEDED",
                HttpStatus.INSUFFICIENT_STORAGE, quota.getRemainingBytes(), fileSize);
        }
    }

    /**
     * Generates storage path for S3 object.
     * Format: {instanceId}/uploads/{year}/{month}/{uuid}.ext
     *
     * @param tenantId Tenant instance ID
     * @param fileName Original filename
     * @return Storage path
     */
    private String generateStoragePath(UUID tenantId, String fileName) {
        LocalDate now = LocalDate.now();
        String extension = extractFileExtension(fileName);
        String uniqueId = UUID.randomUUID().toString();

        return String.format("%s/uploads/%d/%02d/%s%s",
            tenantId,
            now.getYear(),
            now.getMonthValue(),
            uniqueId,
            extension);
    }

    /**
     * Extracts file extension from filename.
     *
     * @param fileName Filename
     * @return Extension with dot (e.g., ".jpg") or empty string
     */
    private String extractFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot > 0 && lastDot < fileName.length() - 1)
            ? fileName.substring(lastDot)
            : "";
    }

    /**
     * Generates presigned PUT URL for S3 upload.
     *
     * @param storagePath S3 object key
     * @return Presigned PUT URL
     */
    private String generatePresignedPutUrl(String storagePath) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(storageProperties.getBucketName())
            .key(storagePath)
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(UPLOAD_URL_TTL)
            .putObjectRequest(putObjectRequest)
            .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /**
     * Verifies the caller may mutate (confirm/delete) the file: only the uploader, or a
     * tenant-admin role (privileged), is allowed. GAP-1309 intra-tenant IDOR guard.
     *
     * <p>Cross-tenant access is already blocked upstream by the Hibernate {@code tenantFilter}
     * (the lookup returns empty → 404). This adds the missing INTRA-tenant per-resource check
     * so a non-uploader in the same tenant cannot confirm/delete another user's file by
     * enumerating {@code fileId}.
     *
     * @param file        File being mutated
     * @param requesterId Gateway user ID of the caller (X-User-Id)
     * @param privileged  true if caller holds a tenant-admin role (ADMIN/OWNER/PLATFORM_ADMIN)
     * @throws BusinessException (403 FILE_ACCESS_DENIED) if caller is neither uploader nor privileged
     */
    private void verifyFileOwnership(UploadedFile file, Long requesterId, boolean privileged) {
        if (privileged) {
            return; // tenant admin/owner may manage any file in the tenant
        }
        if (requesterId == null || !requesterId.equals(file.getUploaderId())) {
            log.warn("Access denied - file {} mutate by non-owner {} (uploader: {})",
                file.getId(), requesterId, file.getUploaderId());
            throw new BusinessException("FILE_ACCESS_DENIED", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Checks if requester has permission to access file.
     *
     * @param file        File to check
     * @param requesterId User requesting access
     * @param tenantId    Requester's tenant ID
     * @throws BusinessException if access denied
     */
    private void checkAccessPermission(UploadedFile file, Long requesterId, UUID tenantId) {
        if (file.isPublic()) {
            return; // PUBLIC files - anyone can access
        }

        if (file.isPrivate() && !file.getUploaderId().equals(requesterId)) {
            log.warn("Access denied - PRIVATE file {} requested by non-owner {}", file.getId(), requesterId);
            throw new BusinessException("FILE_ACCESS_DENIED", HttpStatus.FORBIDDEN);
        }

        if (file.isTenantScoped() && !file.getInstanceId().equals(tenantId)) {
            log.warn("Access denied - TENANT file {} requested by different tenant {}", file.getId(), tenantId);
            throw new BusinessException("FILE_ACCESS_DENIED", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Updates quota usage for tenant.
     *
     * @param tenantId Tenant instance ID
     * @param fileSize File size in bytes
     * @param add      true to add, false to subtract
     */
    private void updateQuotaUsage(UUID tenantId, long fileSize, boolean add) {
        StorageQuota quota = storageQuotaRepository.findByInstanceId(tenantId)
            .orElseGet(() -> createDefaultQuota(tenantId));

        if (add) {
            quota.addUsage(fileSize);
        } else {
            quota.subtractUsage(fileSize);
        }

        storageQuotaRepository.save(quota);
        log.debug("Updated quota for tenant: {}, change: {}{} bytes",
            tenantId, add ? "+" : "-", fileSize);
    }

    /**
     * Creates default FREE tier quota for new tenant.
     *
     * @param tenantId Tenant instance ID
     * @return Created quota
     */
    private StorageQuota createDefaultQuota(UUID tenantId) {
        log.info("Creating default FREE quota for tenant: {}", tenantId);

        StorageQuota quota = StorageQuota.builder()
            .instanceId(tenantId)
            .tier(StorageTier.FREE)
            .usedBytes(0L)
            .quotaBytes(StorageTier.FREE.getQuotaBytes())
            .build();

        return storageQuotaRepository.save(quota);
    }
}
