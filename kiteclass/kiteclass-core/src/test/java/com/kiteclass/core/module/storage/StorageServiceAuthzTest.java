package com.kiteclass.core.module.storage;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.config.StorageProperties;
import com.kiteclass.core.module.storage.constant.AccessLevel;
import com.kiteclass.core.module.storage.constant.FileType;
import com.kiteclass.core.module.storage.constant.StorageStatus;
import com.kiteclass.core.module.storage.entity.UploadedFile;
import com.kiteclass.core.module.storage.mapper.StorageMapper;
import com.kiteclass.core.module.storage.repository.StorageQuotaRepository;
import com.kiteclass.core.module.storage.repository.UploadedFileRepository;
import com.kiteclass.core.module.storage.service.LessonMaterialAccessGuard;
import com.kiteclass.core.module.storage.service.impl.StorageServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GAP-1309 — per-resource ownership authz on {@code StorageServiceImpl.confirmUpload} +
 * {@code deleteFile} (intra-tenant IDOR closure).
 *
 * <p>Before the fix both mutate methods took only {@code fileId} and never compared the
 * caller against {@code uploaded_files.uploader_id}; cross-tenant access was blocked by the
 * Hibernate {@code tenantFilter} but ANY same-tenant user could confirm/delete another user's
 * file by enumerating {@code fileId}. These unit tests assert the three required outcomes:
 * non-owner deny (403), owner allow, privileged (admin/owner role) allow.
 *
 * @since GAP-1309
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GAP-1309 — Storage confirm/delete ownership authz (intra-tenant IDOR)")
class StorageServiceAuthzTest {

    @Mock private UploadedFileRepository uploadedFileRepository;
    @Mock private StorageQuotaRepository storageQuotaRepository;
    @Mock private StorageMapper storageMapper;
    @Mock private S3Client s3Client;
    @Mock private S3Presigner s3Presigner;
    @Mock private StorageProperties storageProperties;
    @Mock private LessonMaterialAccessGuard lessonMaterialAccessGuard;

    @InjectMocks private StorageServiceImpl storageService;

    private static final Long FILE_ID = 42L;
    private static final Long UPLOADER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private UploadedFile pendingFileOwnedByUploader() {
        UploadedFile file = UploadedFile.builder()
            .uploaderId(UPLOADER_ID)
            .fileType(FileType.DOCUMENT)
            .originalName("notes.pdf")
            .storagePath("tenant/uploads/2026/06/uuid.pdf")
            .fileSize(1024L)
            .mimeType("application/pdf")
            .accessLevel(AccessLevel.PRIVATE)
            .status(StorageStatus.PENDING)
            .build();
        file.setId(FILE_ID);
        return file;
    }

    // ── deleteFile ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteFile: non-owner (same tenant) is denied with 403 and nothing is mutated")
    void deleteFile_nonOwner_denied() {
        when(uploadedFileRepository.findByIdAndDeletedFalse(FILE_ID))
            .thenReturn(Optional.of(pendingFileOwnedByUploader()));

        assertThatThrownBy(() -> storageService.deleteFile(FILE_ID, OTHER_USER_ID, false))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException be = (BusinessException) ex;
                assertThat(be.getCode()).isEqualTo("FILE_ACCESS_DENIED");
                assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            });

        verify(uploadedFileRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteFile: uploader is allowed (soft delete persisted)")
    void deleteFile_owner_allowed() {
        UploadedFile file = pendingFileOwnedByUploader();
        when(uploadedFileRepository.findByIdAndDeletedFalse(FILE_ID)).thenReturn(Optional.of(file));

        storageService.deleteFile(FILE_ID, UPLOADER_ID, false);

        assertThat(file.getStatus()).isEqualTo(StorageStatus.DELETED);
        assertThat(file.isDeleted()).isTrue();
        verify(uploadedFileRepository, times(1)).save(file);
    }

    @Test
    @DisplayName("deleteFile: privileged role (admin/owner) deletes another user's file")
    void deleteFile_privileged_allowed() {
        UploadedFile file = pendingFileOwnedByUploader();
        when(uploadedFileRepository.findByIdAndDeletedFalse(FILE_ID)).thenReturn(Optional.of(file));

        // OTHER_USER_ID is NOT the uploader, but privileged=true (e.g. tenant OWNER/ADMIN).
        storageService.deleteFile(FILE_ID, OTHER_USER_ID, true);

        assertThat(file.getStatus()).isEqualTo(StorageStatus.DELETED);
        verify(uploadedFileRepository, times(1)).save(file);
    }

    // ── confirmUpload ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmUpload: non-owner is denied 403 before any S3 / status work")
    void confirmUpload_nonOwner_denied_beforeS3() {
        when(uploadedFileRepository.findByIdAndDeletedFalse(FILE_ID))
            .thenReturn(Optional.of(pendingFileOwnedByUploader()));

        assertThatThrownBy(() -> storageService.confirmUpload(FILE_ID, OTHER_USER_ID, false))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        // Ownership check fires BEFORE the S3 HeadObject + status mutation — no side effects.
        verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
        verify(uploadedFileRepository, never()).save(any());
    }
}
