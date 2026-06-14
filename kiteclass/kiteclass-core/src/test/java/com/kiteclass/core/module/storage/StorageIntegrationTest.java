package com.kiteclass.core.module.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.storage.constant.AccessLevel;
import com.kiteclass.core.module.storage.constant.FileType;
import com.kiteclass.core.module.storage.dto.PresignedUploadRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MinIOContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

import java.net.URI;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full integration tests for Storage API endpoints.
 *
 * <p>Tests the complete file upload/download workflow:
 * <ol>
 *   <li>Generate presigned upload URL</li>
 *   <li>Confirm file upload</li>
 *   <li>Generate presigned download URL</li>
 *   <li>Delete file</li>
 *   <li>Check quota usage</li>
 * </ol>
 *
 * <p>Uses @SpringBootTest with MinIO Testcontainer for S3 operations.
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
class StorageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MinIOContainer minioContainer;

    private UUID tenantId;
    private Long userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = 1L;

        // Create bucket in MinIO for tests
        ensureBucketExists();
    }

    @Test
    @DisplayName("POST /api/v1/storage/upload-url - Should generate presigned upload URL successfully")
    void shouldGeneratePresignedUploadUrl() throws Exception {
        // Given
        PresignedUploadRequest request = new PresignedUploadRequest(
            "test-image.jpg",
            1024L * 100, // 100 KB
            "image/jpeg",
            FileType.IMAGE,
            AccessLevel.PRIVATE
        );

        // When & Then
        mockMvc.perform(post("/api/v1/storage/upload-url")
                .header("X-User-Id", userId)
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.fileId", notNullValue()))
            .andExpect(jsonPath("$.data.uploadUrl", notNullValue()))
            .andExpect(jsonPath("$.data.uploadUrl", containsString(".jpg")))
            .andExpect(jsonPath("$.data.uploadUrl", containsString("X-Amz-Algorithm")))
            .andExpect(jsonPath("$.data.expiresAt", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/storage/upload-url - Should reject file type not in whitelist")
    void shouldRejectInvalidFileType() throws Exception {
        // Given
        PresignedUploadRequest request = new PresignedUploadRequest(
            "malicious.exe",
            1024L,
            "application/x-msdownload", // Executable - not in whitelist
            FileType.OTHER,
            AccessLevel.PRIVATE
        );

        // When & Then
        mockMvc.perform(post("/api/v1/storage/upload-url")
                .header("X-User-Id", userId)
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("FILE_TYPE_NOT_ALLOWED")));
    }

    @Test
    @DisplayName("POST /api/v1/storage/upload-url - Should reject file size exceeding maximum")
    void shouldRejectFileExceedingMaximumSize() throws Exception {
        // Given
        PresignedUploadRequest request = new PresignedUploadRequest(
            "huge-file.mp4",
            200L * 1024 * 1024, // 200 MB (max is 100 MB)
            "video/mp4",
            FileType.VIDEO,
            AccessLevel.PRIVATE
        );

        // When & Then
        mockMvc.perform(post("/api/v1/storage/upload-url")
                .header("X-User-Id", userId)
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("FILE_SIZE_EXCEEDS_MAXIMUM")));
    }

    @Test
    @DisplayName("GET /api/v1/storage/quota - Should return quota usage for tenant")
    void shouldGetQuotaUsage() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/storage/quota")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.tier", is("FREE")))
            .andExpect(jsonPath("$.data.usedBytes", is(0)))
            .andExpect(jsonPath("$.data.quotaBytes", greaterThan(0)))
            .andExpect(jsonPath("$.data.remainingBytes", greaterThan(0)))
            .andExpect(jsonPath("$.data.usagePercentage", is(0.0)));
    }

    @Test
    @DisplayName("POST /api/v1/storage/upload-url - Should enforce quota limit")
    void shouldEnforceQuotaLimit() throws Exception {
        // Given - Upload files until quota is almost exhausted
        // FREE tier = 1 GB (1024 MB), upload 10 files of 95 MB each (950 MB total)
        long fileSize = 95L * 1024 * 1024; // 95 MB

        for (int i = 0; i < 10; i++) {
            PresignedUploadRequest request = new PresignedUploadRequest(
                "file-" + i + ".mp4",
                fileSize,
                "video/mp4",
                FileType.VIDEO,
                AccessLevel.PRIVATE
            );

            mockMvc.perform(post("/api/v1/storage/upload-url")
                    .header("X-User-Id", userId)
                    .header("X-Tenant-Id", tenantId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        }

        // Now upload 11th file (950 + 95 = 1045 MB > 1024 MB quota)
        PresignedUploadRequest finalRequest = new PresignedUploadRequest(
            "final-file.mp4",
            fileSize,
            "video/mp4",
            FileType.VIDEO,
            AccessLevel.PRIVATE
        );

        // When & Then - Should reject due to quota exceeded
        mockMvc.perform(post("/api/v1/storage/upload-url")
                .header("X-User-Id", userId)
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(finalRequest)))
            .andExpect(status().isInsufficientStorage())
            .andExpect(jsonPath("$.code", is("STORAGE_QUOTA_EXCEEDED")));
    }

    @Test
    @DisplayName("DELETE /api/v1/storage/{fileId} - Should delete file successfully")
    void shouldDeleteFile() throws Exception {
        // Given - Create a file first
        PresignedUploadRequest uploadRequest = new PresignedUploadRequest(
            "test.pdf",
            1024L,
            "application/pdf",
            FileType.DOCUMENT,
            AccessLevel.PRIVATE
        );

        String createResponse = mockMvc.perform(post("/api/v1/storage/upload-url")
                .header("X-User-Id", userId)
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(uploadRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long fileId = objectMapper.readTree(createResponse).get("data").get("fileId").asLong();

        // When & Then - Delete the file (GAP-1309: must be the uploader; X-User-Id now required)
        mockMvc.perform(delete("/api/v1/storage/" + fileId)
                .header("X-User-Id", userId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isNoContent());
    }

    // === Helper Methods ===

    /**
     * Ensures test bucket exists in MinIO.
     * Creates bucket if it doesn't exist.
     */
    private void ensureBucketExists() {
        try (S3Client s3Client = createS3Client()) {
            String bucketName = "test-bucket";

            // Check if bucket exists
            try {
                s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            } catch (Exception e) {
                // Bucket doesn't exist, create it
                s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            }
        }
    }

    /**
     * Creates S3Client for MinIO Testcontainer.
     *
     * @return configured S3Client
     */
    private S3Client createS3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            minioContainer.getUserName(),
            minioContainer.getPassword()
        );

        return S3Client.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create(minioContainer.getS3URL()))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .forcePathStyle(true)
            .build();
    }
}
