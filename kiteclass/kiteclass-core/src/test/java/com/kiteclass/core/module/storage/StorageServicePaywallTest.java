package com.kiteclass.core.module.storage;

import com.kiteclass.core.common.exception.PermissionDeniedException;
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
import org.springframework.beans.factory.ObjectProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GAP-1307 — {@code StorageServiceImpl.generatePresignedDownloadUrl} paywall delegation +
 * {@link ObjectProvider} context-resilience.
 *
 * <p>Asserts the download path:
 * <ul>
 *   <li>delegates to {@link LessonMaterialAccessGuard} when the bean is present, and a guard
 *       rejection (paid lesson, non-enrolled student) propagates as 403 BEFORE the S3
 *       presign step (no URL leaked);</li>
 *   <li>still produces a URL when the guard allows;</li>
 *   <li><b>does not fail</b> when the guard bean is absent (sliced context) — the
 *       {@code ObjectProvider.getIfAvailable()} returns {@code null} and the method proceeds.
 *       This is the regression the reverted #2416 hard-required-bean wiring caused
 *       ({@code OpenApiSpecExportTest}-style context-load failure).</li>
 * </ul>
 *
 * @since GAP-1307
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GAP-1307 — Storage download paywall delegation + ObjectProvider resilience")
class StorageServicePaywallTest {

    @Mock private UploadedFileRepository uploadedFileRepository;
    @Mock private StorageQuotaRepository storageQuotaRepository;
    @Mock private StorageMapper storageMapper;
    @Mock private S3Client s3Client;
    @Mock private S3Presigner s3Presigner;
    @Mock private StorageProperties storageProperties;
    @Mock private ObjectProvider<LessonMaterialAccessGuard> guardProvider;
    @Mock private LessonMaterialAccessGuard guard;

    @InjectMocks private StorageServiceImpl storageService;

    private static final Long FILE_ID = 500L;
    private static final Long UPLOADER_ID = 1L;
    private static final Long STUDENT_ID = 7L;
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    /** A CONFIRMED, TENANT-scoped file in the requester's tenant (passes the visibility check). */
    private UploadedFile confirmedTenantFile() {
        UploadedFile file = UploadedFile.builder()
            .uploaderId(UPLOADER_ID)
            .fileType(FileType.DOCUMENT)
            .originalName("paid-material.pdf")
            .storagePath("tenant/uploads/2026/06/uuid.pdf")
            .fileSize(2048L)
            .mimeType("application/pdf")
            .accessLevel(AccessLevel.TENANT)
            .status(StorageStatus.CONFIRMED)
            .build();
        file.setId(FILE_ID);
        file.setInstanceId(TENANT_ID);
        return file;
    }

    private void stubPresigner() throws Exception {
        URL url = URI.create("https://minio.example.test/tenant/uploads/2026/06/uuid.pdf?sig=abc").toURL();
        PresignedGetObjectRequest presigned = org.mockito.Mockito.mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(url);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);
        when(storageProperties.getBucketName()).thenReturn("kiteclass-bucket");
    }

    @Test
    @DisplayName("guard present + rejects → 403 propagates, no S3 presign (no URL leaked)")
    void guardPresent_rejects_deniesBeforePresign() {
        when(uploadedFileRepository.findByIdAndDeletedFalse(FILE_ID))
            .thenReturn(Optional.of(confirmedTenantFile()));
        when(guardProvider.getIfAvailable()).thenReturn(guard);
        doThrow(new PermissionDeniedException("STUDENT_NOT_ENROLLED_IN_COURSE"))
            .when(guard).verifyLessonMaterialDownloadAccess(eq(FILE_ID), eq(UPLOADER_ID), eq(STUDENT_ID), eq(false));

        assertThatThrownBy(() ->
            storageService.generatePresignedDownloadUrl(FILE_ID, STUDENT_ID, TENANT_ID, false))
            .isInstanceOf(PermissionDeniedException.class)
            .hasMessageContaining("STUDENT_NOT_ENROLLED_IN_COURSE");

        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("guard present + allows → presigned URL returned")
    void guardPresent_allows_returnsUrl() throws Exception {
        when(uploadedFileRepository.findByIdAndDeletedFalse(FILE_ID))
            .thenReturn(Optional.of(confirmedTenantFile()));
        when(guardProvider.getIfAvailable()).thenReturn(guard);
        stubPresigner();

        String url = storageService.generatePresignedDownloadUrl(FILE_ID, STUDENT_ID, TENANT_ID, false);

        assertThat(url).startsWith("https://");
        verify(guard).verifyLessonMaterialDownloadAccess(FILE_ID, UPLOADER_ID, STUDENT_ID, false);
    }

    @Test
    @DisplayName("guard bean ABSENT (sliced context) → no failure, URL still produced (context-resilient)")
    void guardAbsent_resilient_returnsUrl() throws Exception {
        when(uploadedFileRepository.findByIdAndDeletedFalse(FILE_ID))
            .thenReturn(Optional.of(confirmedTenantFile()));
        when(guardProvider.getIfAvailable()).thenReturn(null); // LMS bean not in this context
        stubPresigner();

        String url = storageService.generatePresignedDownloadUrl(FILE_ID, STUDENT_ID, TENANT_ID, false);

        assertThat(url).startsWith("https://");
        verify(guard, never()).verifyLessonMaterialDownloadAccess(anyLong(), anyLong(), anyLong(), org.mockito.ArgumentMatchers.anyBoolean());
    }
}
