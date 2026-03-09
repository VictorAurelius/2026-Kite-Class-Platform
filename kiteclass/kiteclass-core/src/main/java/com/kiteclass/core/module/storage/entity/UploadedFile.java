package com.kiteclass.core.module.storage.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.storage.constant.AccessLevel;
import com.kiteclass.core.module.storage.constant.FileType;
import com.kiteclass.core.module.storage.constant.StorageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entity representing an uploaded file in the system.
 *
 * <p>Stores metadata about files uploaded to S3/MinIO storage:
 * <ul>
 *   <li>Uploader information (Gateway user ID, no FK)</li>
 *   <li>File metadata (name, size, MIME type)</li>
 *   <li>Storage path in S3/MinIO</li>
 *   <li>Access control (PUBLIC/PRIVATE/TENANT)</li>
 *   <li>Upload lifecycle (PENDING → CONFIRMED/EXPIRED)</li>
 * </ul>
 *
 * <p>Multi-tenant isolation is enforced via instanceId field and Hibernate filter.
 *
 * <p>Soft delete is supported - deleted files are kept for 30 days before S3 cleanup.
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@Entity
@Table(name = "uploaded_files", indexes = {
    @Index(name = "idx_uploaded_files_instance_id", columnList = "instance_id"),
    @Index(name = "idx_uploaded_files_status", columnList = "status"),
    @Index(name = "idx_uploaded_files_expires_at", columnList = "expires_at"),
    @Index(name = "idx_uploaded_files_uploader_id", columnList = "uploader_id"),
    @Index(name = "idx_uploaded_files_deleted", columnList = "deleted"),
    @Index(name = "idx_uploaded_files_deleted_at", columnList = "deleted_at"),
    @Index(name = "idx_uploaded_files_instance_status", columnList = "instance_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedFile extends BaseEntity {

    /**
     * Gateway user ID who uploaded this file.
     * No FK constraint - user exists in Gateway service.
     */
    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    /**
     * File type classification (IMAGE, DOCUMENT, VIDEO, AUDIO, OTHER).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private FileType fileType;

    /**
     * Original filename from upload.
     */
    @Column(name = "original_name", nullable = false, length = 500)
    private String originalName;

    /**
     * Storage path in S3/MinIO.
     * Format: {instanceId}/uploads/{year}/{month}/{uuid}.ext
     */
    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;

    /**
     * File size in bytes.
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * MIME type (e.g., image/jpeg, application/pdf).
     */
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    /**
     * Access control level.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 20)
    @Builder.Default
    private AccessLevel accessLevel = AccessLevel.PRIVATE;

    /**
     * Upload status (PENDING, CONFIRMED, EXPIRED, DELETED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StorageStatus status = StorageStatus.PENDING;

    /**
     * Expiration time for PENDING uploads (30 min TTL).
     * Null for CONFIRMED uploads.
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Timestamp when file was soft deleted (for S3 cleanup after 30 days).
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Marks file as confirmed (upload completed successfully).
     */
    public void confirmUpload() {
        this.status = StorageStatus.CONFIRMED;
        this.expiresAt = null;
    }

    /**
     * Marks file as expired (upload window exceeded).
     */
    public void markAsExpired() {
        this.status = StorageStatus.EXPIRED;
    }

    /**
     * Soft deletes the file and sets deletedAt timestamp.
     * File will be cleaned from S3 after 30 days.
     */
    public void softDelete() {
        markAsDeleted();
        this.status = StorageStatus.DELETED;
        this.deletedAt = Instant.now();
    }

    /**
     * Checks if file upload is pending.
     *
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return this.status == StorageStatus.PENDING;
    }

    /**
     * Checks if file is confirmed.
     *
     * @return true if status is CONFIRMED
     */
    public boolean isConfirmed() {
        return this.status == StorageStatus.CONFIRMED;
    }

    /**
     * Checks if file has expired.
     *
     * @return true if status is EXPIRED or expires_at is past
     */
    public boolean isExpired() {
        return this.status == StorageStatus.EXPIRED ||
               (this.expiresAt != null && Instant.now().isAfter(this.expiresAt));
    }

    /**
     * Checks if file is publicly accessible.
     *
     * @return true if accessLevel is PUBLIC
     */
    public boolean isPublic() {
        return this.accessLevel == AccessLevel.PUBLIC;
    }

    /**
     * Checks if file is private (uploader-only access).
     *
     * @return true if accessLevel is PRIVATE
     */
    public boolean isPrivate() {
        return this.accessLevel == AccessLevel.PRIVATE;
    }

    /**
     * Checks if file is tenant-scoped (all users in tenant can access).
     *
     * @return true if accessLevel is TENANT
     */
    public boolean isTenantScoped() {
        return this.accessLevel == AccessLevel.TENANT;
    }
}
