package com.kiteclass.core.module.childprotection.storage;

import com.kiteclass.core.config.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full integration test for {@link MinIOVettingDocumentStorageImpl} against a
 * real MinIO container.
 *
 * <p>Uploads a fake LLTP document, verifies the bytes round-trip via the
 * underlying {@link S3Client}, and checks the presigned download URL is
 * issued. Sister of {@code StorageIntegrationTest} (general user uploads) —
 * scoped to vetting bucket per BR-VETTING-004.
 *
 * <p>Reuses {@link TestContainersConfiguration} which already boots MinIO,
 * Postgres, and Redis. The vetting bucket is created on each test method's
 * setup (idempotent).
 *
 * @since Wave 18b3 Bucket B — GAP-322b Phase 1B remainder
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfiguration.class)
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@DisplayName("MinIOVettingDocumentStorage — testcontainer IT")
class MinIOVettingDocumentStorageIT {

    @Autowired private MinIOVettingDocumentStorageImpl storage;
    @Autowired private S3Client s3Client;

    @Value("${childprotection.minio.bucket:kiteclass-vetting}")
    private String vettingBucket;

    @BeforeEach
    void ensureBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(vettingBucket).build());
        } catch (S3Exception ex) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(vettingBucket).build());
        }
    }

    @Test
    @DisplayName("storeDocument → S3 putObject; bytes round-trip identical")
    void storeRoundTrip() {
        byte[] payload = "fake LLTP scan payload — Wave 18b3 Bucket B"
                .getBytes(StandardCharsets.UTF_8);

        String key = storage.storeDocument(101L, "lltp-scan.pdf", payload);

        assertThat(key).isEqualTo("vetting/101/lltp-scan.pdf");

        // Fetch directly via S3Client and assert bytes
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(vettingBucket)
                .key(key)
                .build();
        try (ResponseInputStream<GetObjectResponse> in = s3Client.getObject(get,
                ResponseTransformer.toInputStream())) {
            byte[] fetched = in.readAllBytes();
            assertThat(fetched).isEqualTo(payload);
        } catch (Exception ex) {
            throw new AssertionError("Failed to fetch uploaded vetting document", ex);
        }
    }

    @Test
    @DisplayName("getDownloadUrl returns a usable presigned URL string")
    void presignedUrlIsIssued() {
        byte[] payload = "another payload".getBytes(StandardCharsets.UTF_8);
        String key = storage.storeDocument(102L, "doc.pdf", payload);

        String url = storage.getDownloadUrl(102L, key, Duration.ofMinutes(5));

        assertThat(url).isNotBlank();
        assertThat(url).contains(vettingBucket);
        // Presigned URL contains AWS signature query params
        assertThat(url).containsAnyOf("X-Amz-Signature", "Signature=");
    }

    @Test
    @DisplayName("Path-traversal sanitization holds end-to-end")
    void pathTraversalSanitized() {
        byte[] payload = "evil".getBytes(StandardCharsets.UTF_8);
        String key = storage.storeDocument(103L, "../../etc/passwd", payload);

        assertThat(key).doesNotContain("..");
        assertThat(key).doesNotContain("/etc/");
        assertThat(key).startsWith("vetting/103/");

        // Verify round-trip succeeded under sanitized key
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(vettingBucket)
                .key(key)
                .build();
        try (ResponseInputStream<GetObjectResponse> in = s3Client.getObject(get,
                ResponseTransformer.toInputStream())) {
            assertThat(in.readAllBytes()).isEqualTo(payload);
        } catch (Exception ex) {
            throw new AssertionError("Failed to fetch sanitized-key document", ex);
        }
    }
}
