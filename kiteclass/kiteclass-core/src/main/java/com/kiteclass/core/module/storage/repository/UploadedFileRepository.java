package com.kiteclass.core.module.storage.repository;

import com.kiteclass.core.module.storage.constant.StorageStatus;
import com.kiteclass.core.module.storage.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UploadedFile entity.
 *
 * <p>Provides data access methods including:
 * <ul>
 *   <li>Find by ID (excluding soft-deleted records)</li>
 *   <li>Find expired pending uploads (for cleanup scheduler)</li>
 *   <li>Find deleted files (for S3 cleanup scheduler)</li>
 *   <li>Calculate used bytes per tenant (for quota management)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    /**
     * Finds an uploaded file by ID, excluding soft-deleted records.
     *
     * @param id the file ID
     * @return Optional containing the file if found and not deleted
     */
    Optional<UploadedFile> findByIdAndDeletedFalse(Long id);

    /**
     * Finds all PENDING files that have expired (expiresAt before given time).
     *
     * <p>Used by cleanup scheduler to mark expired uploads as EXPIRED.
     *
     * @param expiryTime the expiry cutoff time
     * @return list of expired pending files
     */
    List<UploadedFile> findByStatusAndExpiresAtBeforeAndDeletedFalse(
        StorageStatus status,
        Instant expiryTime
    );

    /**
     * Finds all soft-deleted files older than given timestamp.
     *
     * <p>Used by cleanup scheduler to delete files from S3 after 30-day grace period.
     *
     * @param deletedBefore cutoff timestamp (deletedAt must be before this)
     * @return list of files eligible for S3 cleanup
     */
    List<UploadedFile> findByDeletedTrueAndDeletedAtBefore(Instant deletedBefore);

    /**
     * Calculates total storage usage in bytes for a tenant.
     *
     * <p>Sums fileSize of all CONFIRMED files (excludes PENDING, EXPIRED, DELETED).
     *
     * <p>Used for quota recalculation.
     *
     * @param instanceId the tenant instance ID
     * @return total used bytes (0 if no files)
     */
    @Query(value = """
            SELECT COALESCE(SUM(f.file_size), 0)
            FROM uploaded_files f
            WHERE f.instance_id = :instanceId
            AND f.status = 'CONFIRMED'
            AND f.deleted = false
            """,
            nativeQuery = true)
    Long calculateUsedBytes(@Param("instanceId") UUID instanceId);

    /**
     * Finds all confirmed files for a tenant (for listing).
     *
     * @param instanceId the tenant instance ID
     * @return list of confirmed files
     */
    List<UploadedFile> findByInstanceIdAndStatusAndDeletedFalse(
        UUID instanceId,
        StorageStatus status
    );

    /**
     * Counts files by uploader and status.
     *
     * @param uploaderId the uploader user ID
     * @param status the file status
     * @return count of files
     */
    long countByUploaderIdAndStatusAndDeletedFalse(Long uploaderId, StorageStatus status);
}
