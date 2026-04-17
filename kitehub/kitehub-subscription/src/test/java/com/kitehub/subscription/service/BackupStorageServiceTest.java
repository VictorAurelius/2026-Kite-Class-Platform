package com.kitehub.subscription.service;

import com.kitehub.subscription.config.S3Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BackupStorageService.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BackupStorageService Unit Tests")
class BackupStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Nested
    @DisplayName("Mock Mode (s3Client is null)")
    class MockMode {

        private BackupStorageService createMockModeService() {
            S3Config config = new S3Config();
            config.setMockMode(true);
            config.setBucket("test-bucket");
            return new BackupStorageService(config, null, null);
        }

        @Test
        @DisplayName("uploadBackup should log but not throw in mock mode")
        void uploadShouldNotThrowInMockMode() {
            BackupStorageService service = createMockModeService();
            InputStream data = new ByteArrayInputStream("test".getBytes());

            // Should not throw
            service.uploadBackup("backups/test.dump", data, 4L);
        }

        @Test
        @DisplayName("deleteBackup should log but not throw in mock mode")
        void deleteShouldNotThrowInMockMode() {
            BackupStorageService service = createMockModeService();

            // Should not throw
            service.deleteBackup("backups/test.dump");
        }

        @Test
        @DisplayName("downloadBackup should throw UnsupportedOperationException in mock mode")
        void downloadShouldThrowInMockMode() {
            BackupStorageService service = createMockModeService();

            assertThatThrownBy(() -> service.downloadBackup("backups/test.dump"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("mock mode");
        }

        @Test
        @DisplayName("getBackupUrl should return mock URL in mock mode")
        void getUrlShouldReturnMockUrl() {
            BackupStorageService service = createMockModeService();

            String url = service.getBackupUrl("backups/test.dump");

            assertThat(url).startsWith("mock://");
            assertThat(url).contains("test-bucket");
            assertThat(url).contains("backups/test.dump");
        }
    }

    @Nested
    @DisplayName("Non-Mock Mode (real S3 client)")
    class NonMockMode {

        private BackupStorageService createRealService() {
            S3Config config = new S3Config();
            config.setMockMode(false);
            config.setBucket("kite-backups");
            return new BackupStorageService(config, s3Client, s3Presigner);
        }

        @Test
        @DisplayName("uploadBackup should call S3 putObject with correct params")
        void uploadShouldCallS3PutObject() {
            BackupStorageService service = createRealService();
            InputStream data = new ByteArrayInputStream("backup data".getBytes());

            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

            service.uploadBackup("backups/instance-123/test.dump", data, 11L);

            ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

            PutObjectRequest request = captor.getValue();
            assertThat(request.bucket()).isEqualTo("kite-backups");
            assertThat(request.key()).isEqualTo("backups/instance-123/test.dump");
            assertThat(request.contentLength()).isEqualTo(11L);
            assertThat(request.contentType()).isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("deleteBackup should call S3 deleteObject with correct params")
        void deleteShouldCallS3DeleteObject() {
            BackupStorageService service = createRealService();

            when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

            service.deleteBackup("backups/instance-123/test.dump");

            ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
            verify(s3Client).deleteObject(captor.capture());

            DeleteObjectRequest request = captor.getValue();
            assertThat(request.bucket()).isEqualTo("kite-backups");
            assertThat(request.key()).isEqualTo("backups/instance-123/test.dump");
        }

        @Test
        @DisplayName("uploadBackup should throw when S3 client is null in non-mock mode")
        void uploadShouldThrowWhenS3ClientNull() {
            S3Config config = new S3Config();
            config.setMockMode(false);
            config.setBucket("kite-backups");
            BackupStorageService service = new BackupStorageService(config, null, null);

            InputStream data = new ByteArrayInputStream("test".getBytes());

            assertThatThrownBy(() -> service.uploadBackup("key", data, 4L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("S3Client is not configured");
        }

        @Test
        @DisplayName("downloadBackup should throw when S3 client is null in non-mock mode")
        void downloadShouldThrowWhenS3ClientNull() {
            S3Config config = new S3Config();
            config.setMockMode(false);
            config.setBucket("kite-backups");
            BackupStorageService service = new BackupStorageService(config, null, null);

            assertThatThrownBy(() -> service.downloadBackup("key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("S3Client is not configured");
        }

        @Test
        @DisplayName("deleteBackup should throw when S3 client is null in non-mock mode")
        void deleteShouldThrowWhenS3ClientNull() {
            S3Config config = new S3Config();
            config.setMockMode(false);
            config.setBucket("kite-backups");
            BackupStorageService service = new BackupStorageService(config, null, null);

            assertThatThrownBy(() -> service.deleteBackup("key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("S3Client is not configured");
        }

        @Test
        @DisplayName("getBackupUrl should throw when S3 presigner is null in non-mock mode")
        void getUrlShouldThrowWhenPresignerNull() {
            S3Config config = new S3Config();
            config.setMockMode(false);
            config.setBucket("kite-backups");
            BackupStorageService service = new BackupStorageService(config, s3Client, null);

            assertThatThrownBy(() -> service.getBackupUrl("key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("S3Presigner is not configured");
        }
    }
}
