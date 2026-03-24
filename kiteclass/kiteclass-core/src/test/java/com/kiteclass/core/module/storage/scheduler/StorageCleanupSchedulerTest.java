package com.kiteclass.core.module.storage.scheduler;

import com.kiteclass.core.config.StorageProperties;
import com.kiteclass.core.module.storage.constant.StorageStatus;
import com.kiteclass.core.module.storage.entity.UploadedFile;
import com.kiteclass.core.module.storage.repository.UploadedFileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StorageCleanupScheduler}.
 *
 * @author KiteClass Team
 * @since 2026-03-24
 */
@ExtendWith(MockitoExtension.class)
class StorageCleanupSchedulerTest {

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @Mock
    private S3Client s3Client;

    @Mock
    private StorageProperties storageProperties;

    @InjectMocks
    private StorageCleanupScheduler scheduler;

    // ========================================================================
    // markExpiredPendingUploads
    // ========================================================================

    @Test
    @DisplayName("Should mark expired PENDING files as EXPIRED and save each")
    void markExpiredPendingUploads_marksExpiredFiles() {
        // Given
        UploadedFile file1 = mock(UploadedFile.class);
        UploadedFile file2 = mock(UploadedFile.class);
        when(file1.getId()).thenReturn(1L);
        when(file2.getId()).thenReturn(2L);
        when(file1.getExpiresAt()).thenReturn(Instant.now().minusSeconds(600));
        when(file2.getExpiresAt()).thenReturn(Instant.now().minusSeconds(300));

        when(uploadedFileRepository.findByStatusAndExpiresAtBeforeAndDeletedFalse(
            eq(StorageStatus.PENDING), any(Instant.class)))
            .thenReturn(List.of(file1, file2));

        // When
        scheduler.markExpiredPendingUploads();

        // Then
        verify(file1).markAsExpired();
        verify(file2).markAsExpired();
        verify(uploadedFileRepository, times(1)).save(file1);
        verify(uploadedFileRepository, times(1)).save(file2);
    }

    @Test
    @DisplayName("Should do nothing when no expired PENDING files found")
    void markExpiredPendingUploads_noExpiredFiles() {
        // Given
        when(uploadedFileRepository.findByStatusAndExpiresAtBeforeAndDeletedFalse(
            eq(StorageStatus.PENDING), any(Instant.class)))
            .thenReturn(Collections.emptyList());

        // When
        scheduler.markExpiredPendingUploads();

        // Then
        verify(uploadedFileRepository, never()).save(any(UploadedFile.class));
    }

    // ========================================================================
    // cleanupDeletedFiles
    // ========================================================================

    @Test
    @DisplayName("Should delete file from S3 and hard delete from database")
    void cleanupDeletedFiles_deletesFromS3AndDB() {
        // Given
        String storagePath = "tenant-123/uploads/2026/03/file-abc.pdf";
        UploadedFile file = mock(UploadedFile.class);
        when(file.getId()).thenReturn(1L);
        when(file.getStoragePath()).thenReturn(storagePath);
        when(file.getDeletedAt()).thenReturn(Instant.now().minusSeconds(86400 * 31));

        when(uploadedFileRepository.findByDeletedTrueAndDeletedAtBefore(any(Instant.class)))
            .thenReturn(List.of(file));
        when(storageProperties.getBucketName()).thenReturn("test-bucket");

        // When
        scheduler.cleanupDeletedFiles();

        // Then — verify S3 delete request
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());

        DeleteObjectRequest request = captor.getValue();
        assertThat(request.bucket()).isEqualTo("test-bucket");
        assertThat(request.key()).isEqualTo(storagePath);

        // Then — verify hard delete from DB
        verify(uploadedFileRepository).delete(file);
    }

    @Test
    @DisplayName("Should do nothing when no files to cleanup")
    void cleanupDeletedFiles_noFilesToCleanup() {
        // Given
        when(uploadedFileRepository.findByDeletedTrueAndDeletedAtBefore(any(Instant.class)))
            .thenReturn(Collections.emptyList());

        // When
        scheduler.cleanupDeletedFiles();

        // Then
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        verify(uploadedFileRepository, never()).delete(any(UploadedFile.class));
    }

    @Test
    @DisplayName("Should continue processing remaining files when S3 delete fails for one")
    void cleanupDeletedFiles_continuesOnError() {
        // Given
        UploadedFile file1 = mock(UploadedFile.class);
        when(file1.getId()).thenReturn(1L);
        when(file1.getStoragePath()).thenReturn("path/file1.pdf");

        UploadedFile file2 = mock(UploadedFile.class);
        when(file2.getId()).thenReturn(2L);
        when(file2.getStoragePath()).thenReturn("path/file2.pdf");
        when(file2.getDeletedAt()).thenReturn(Instant.now().minusSeconds(86400 * 31));

        when(uploadedFileRepository.findByDeletedTrueAndDeletedAtBefore(any(Instant.class)))
            .thenReturn(List.of(file1, file2));
        when(storageProperties.getBucketName()).thenReturn("test-bucket");

        // First S3 delete throws exception
        DeleteObjectRequest request1 = DeleteObjectRequest.builder()
            .bucket("test-bucket")
            .key("path/file1.pdf")
            .build();
        doThrow(new RuntimeException("S3 connection failed"))
            .when(s3Client).deleteObject(request1);

        // When
        scheduler.cleanupDeletedFiles();

        // Then — file1 failed, so it should NOT be deleted from DB
        verify(uploadedFileRepository, never()).delete(file1);

        // Then — file2 should still be processed successfully
        verify(s3Client, times(2)).deleteObject(any(DeleteObjectRequest.class));
        verify(uploadedFileRepository).delete(file2);
    }
}
