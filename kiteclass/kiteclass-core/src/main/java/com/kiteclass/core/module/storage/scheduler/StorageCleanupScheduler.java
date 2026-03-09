package com.kiteclass.core.module.storage.scheduler;

import com.kiteclass.core.config.StorageProperties;
import com.kiteclass.core.module.storage.constant.StorageStatus;
import com.kiteclass.core.module.storage.entity.UploadedFile;
import com.kiteclass.core.module.storage.repository.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled jobs for storage cleanup.
 *
 * <p>Handles:
 * <ul>
 *   <li>Mark expired PENDING uploads as EXPIRED (every 10 minutes)</li>
 *   <li>Delete soft-deleted files from S3 after 30-day grace period (daily at 2 AM)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageCleanupScheduler {

    private final UploadedFileRepository uploadedFileRepository;
    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    private static final int SOFT_DELETE_GRACE_PERIOD_DAYS = 30;

    /**
     * Marks expired PENDING uploads as EXPIRED.
     *
     * <p>Runs every 10 minutes.
     *
     * <p>PENDING uploads have 30-minute TTL. After expiration:
     * <ul>
     *   <li>Status changed to EXPIRED</li>
     *   <li>File remains in database (for audit)</li>
     *   <li>S3 object may exist (orphaned) - will be cleaned by lifecycle policy</li>
     * </ul>
     */
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void markExpiredPendingUploads() {
        log.debug("Starting scheduled job: markExpiredPendingUploads");

        Instant now = Instant.now();
        List<UploadedFile> expiredFiles = uploadedFileRepository
            .findByStatusAndExpiresAtBeforeAndDeletedFalse(StorageStatus.PENDING, now);

        if (expiredFiles.isEmpty()) {
            log.debug("No expired PENDING uploads found");
            return;
        }

        log.info("Found {} expired PENDING uploads", expiredFiles.size());

        for (UploadedFile file : expiredFiles) {
            file.markAsExpired();
            uploadedFileRepository.save(file);
            log.debug("Marked file {} as EXPIRED (expiresAt: {})",
                file.getId(), file.getExpiresAt());
        }

        log.info("Marked {} files as EXPIRED", expiredFiles.size());
    }

    /**
     * Deletes soft-deleted files from S3 after 30-day grace period.
     *
     * <p>Runs daily at 2:00 AM.
     *
     * <p>Files with deleted=true and deletedAt older than 30 days:
     * <ul>
     *   <li>Deleted from S3</li>
     *   <li>Hard deleted from database</li>
     * </ul>
     *
     * <p>30-day grace period allows recovery of accidentally deleted files.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupDeletedFiles() {
        log.info("Starting scheduled job: cleanupDeletedFiles");

        Instant cutoffDate = Instant.now().minus(SOFT_DELETE_GRACE_PERIOD_DAYS, ChronoUnit.DAYS);
        List<UploadedFile> filesToDelete = uploadedFileRepository
            .findByDeletedTrueAndDeletedAtBefore(cutoffDate);

        if (filesToDelete.isEmpty()) {
            log.info("No files to cleanup (30-day grace period)");
            return;
        }

        log.info("Found {} files to cleanup from S3 and database", filesToDelete.size());

        int successCount = 0;
        int failCount = 0;

        for (UploadedFile file : filesToDelete) {
            try {
                // Delete from S3
                deleteFromS3(file.getStoragePath());

                // Hard delete from database
                uploadedFileRepository.delete(file);

                successCount++;
                log.debug("Deleted file {} from S3 and database (deletedAt: {})",
                    file.getId(), file.getDeletedAt());

            } catch (Exception e) {
                failCount++;
                log.error("Failed to cleanup file {}: {}",
                    file.getId(), e.getMessage(), e);
                // Continue with next file
            }
        }

        log.info("Cleanup completed: {} succeeded, {} failed", successCount, failCount);
    }

    /**
     * Deletes object from S3.
     *
     * @param storagePath S3 object key
     */
    private void deleteFromS3(String storagePath) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(storageProperties.getBucketName())
                .key(storagePath)
                .build();

            s3Client.deleteObject(deleteRequest);
            log.debug("Deleted from S3: {}", storagePath);

        } catch (Exception e) {
            log.error("Failed to delete from S3: {}", storagePath, e);
            throw e; // Re-throw to trigger rollback
        }
    }
}
