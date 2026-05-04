package com.kiteclass.core.module.childprotection.storage;

import com.kiteclass.core.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for the real {@link MinIOVettingDocumentStorageImpl} backed by AWS
 * SDK v2 {@code S3Client} + {@code S3Presigner}.
 *
 * <p>Mocks the SDK clients — verifies the impl wires correct bucket / key /
 * content-length, sanitizes path-traversal, and propagates SDK errors as
 * {@code AwsServiceException}.
 *
 * @since Wave 18b3 Bucket B — GAP-322b Phase 1B remainder
 */
@DisplayName("MinIOVettingDocumentStorageImpl — AWS SDK v2 wiring")
class MinIOVettingDocumentStorageImplTest {

    private static final String BUCKET = "kiteclass-vetting";

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private MinIOVettingDocumentStorageImpl storage;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        s3Presigner = mock(S3Presigner.class);
        storage = new MinIOVettingDocumentStorageImpl(s3Client, s3Presigner, BUCKET);
    }

    @Test
    @DisplayName("storeDocument calls S3Client.putObject with bucket + namespaced key + content")
    void storeCallsPutObject() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("etag-1").build());

        byte[] bytes = "fake LLTP scan".getBytes(StandardCharsets.UTF_8);
        String key = storage.storeDocument(42L, "lltp.pdf", bytes);

        // Object key must be deterministic + scoped to vetting/{id}/
        assertThat(key).isEqualTo("vetting/42/lltp.pdf");

        ArgumentCaptor<PutObjectRequest> reqCap = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(reqCap.capture(), any(RequestBody.class));
        PutObjectRequest req = reqCap.getValue();
        assertThat(req.bucket()).isEqualTo(BUCKET);
        assertThat(req.key()).isEqualTo("vetting/42/lltp.pdf");
        assertThat(req.contentLength()).isEqualTo((long) bytes.length);
    }

    @Test
    @DisplayName("storeDocument sanitizes path-traversal in filename")
    void storeSanitizesPathTraversal() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("etag").build());

        String key = storage.storeDocument(42L, "../../etc/passwd", new byte[0]);

        assertThat(key).doesNotContain("..");
        assertThat(key).doesNotContain("/etc/");
    }

    @Test
    @DisplayName("storeDocument propagates AWS error (e.g. NoSuchBucket)")
    void storePropagatesAwsError() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(NoSuchBucketException.builder().message("bucket missing").build());

        assertThatThrownBy(() -> storage.storeDocument(1L, "x.pdf", new byte[1]))
                .isInstanceOf(AwsServiceException.class);
    }

    @Test
    @DisplayName("storeDocument null vettingId — rejected before S3 call")
    void storeRejectsNullId() {
        assertThatThrownBy(() -> storage.storeDocument(null, "x.pdf", new byte[0]))
                .isInstanceOf(ValidationException.class);
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("storeDocument blank filename — rejected")
    void storeRejectsBlankFilename() {
        assertThatThrownBy(() -> storage.storeDocument(1L, " ", new byte[0]))
                .isInstanceOf(ValidationException.class);
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("storeDocument null content — rejected")
    void storeRejectsNullContent() {
        assertThatThrownBy(() -> storage.storeDocument(1L, "x.pdf", null))
                .isInstanceOf(ValidationException.class);
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("getDownloadUrl returns presigned URL via S3Presigner")
    void getDownloadUrlPresigns() throws Exception {
        URL url = URI.create("https://minio.example.test/kiteclass-vetting/vetting/1/x.pdf?sig=abc").toURL();
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(url);
        when(s3Presigner.presignGetObject(any(software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.class)))
                .thenReturn(presigned);

        String result = storage.getDownloadUrl(1L, "vetting/1/x.pdf", Duration.ofMinutes(15));

        assertThat(result).isEqualTo(url.toString());
    }

    @Test
    @DisplayName("getDownloadUrl caps TTL at 15 minutes max")
    void getDownloadUrlCapsTtl() throws Exception {
        URL url = URI.create("https://minio.example.test/kiteclass-vetting/x?sig=abc").toURL();
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(url);

        ArgumentCaptor<software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest> cap =
                ArgumentCaptor.forClass(software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.class);
        when(s3Presigner.presignGetObject(cap.capture())).thenReturn(presigned);

        // Caller asks for 1h — impl must cap at 15min per BR-VETTING-004
        storage.getDownloadUrl(1L, "vetting/1/x.pdf", Duration.ofHours(1));

        assertThat(cap.getValue().signatureDuration()).isLessThanOrEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("getDownloadUrl null docId rejected")
    void getDownloadUrlRejectsNullDoc() {
        assertThatThrownBy(() -> storage.getDownloadUrl(1L, null, Duration.ofMinutes(15)))
                .isInstanceOf(ValidationException.class);
        verify(s3Presigner, never()).presignGetObject(
                any(software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("getDownloadUrl zero / negative TTL rejected")
    void getDownloadUrlRejectsBadTtl() {
        assertThatThrownBy(() -> storage.getDownloadUrl(1L, "x", Duration.ZERO))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> storage.getDownloadUrl(1L, "x", Duration.ofMinutes(-1)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("deleteDocument calls S3Client.deleteObject on the docId")
    void deleteDelegatesToS3() {
        storage.deleteDocument(1L, "vetting/1/x.pdf");

        ArgumentCaptor<DeleteObjectRequest> cap = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(cap.capture());
        assertThat(cap.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(cap.getValue().key()).isEqualTo("vetting/1/x.pdf");
    }
}
