package com.kiteclass.core.module.childprotection.storage;

import com.kiteclass.core.common.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract test for the {@link VettingDocumentStorage} stub
 * ({@link MinIOVettingDocumentStorageImpl}).
 *
 * <p>Phase 1B foundation — verifies that callers (controller, future upload
 * UI) can rely on the contract while concrete MinIO SDK wiring is deferred
 * to Phase 1B follow-up.
 *
 * @since Wave 18b2 Bucket B — GAP-322b Phase 1B foundation
 */
@DisplayName("VettingDocumentStorage stub — contract")
class VettingDocumentStorageStubTest {

    private final VettingDocumentStorage storage = new MinIOVettingDocumentStorageImpl();

    @Test
    @DisplayName("storeDocument returns deterministic key under minio://vetting/{id}/")
    void storeReturnsKey() {
        byte[] bytes = "fake LLTP scan".getBytes(StandardCharsets.UTF_8);
        String key = storage.storeDocument(42L, "lltp.pdf", bytes);

        assertThat(key).isEqualTo("minio://vetting/42/lltp.pdf");
    }

    @Test
    @DisplayName("storeDocument sanitizes path-traversal in filename")
    void storeSanitizesFilename() {
        byte[] bytes = new byte[0];
        String key = storage.storeDocument(42L, "../../etc/passwd", bytes);

        assertThat(key).doesNotContain("..");
        assertThat(key).doesNotContain("/etc/");
    }

    @Test
    @DisplayName("storeDocument null vettingId rejected")
    void storeRejectsNullId() {
        assertThatThrownBy(() -> storage.storeDocument(null, "x.pdf", new byte[0]))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("storeDocument blank filename rejected")
    void storeRejectsBlankFilename() {
        assertThatThrownBy(() -> storage.storeDocument(1L, " ", new byte[0]))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("storeDocument null content rejected")
    void storeRejectsNullContent() {
        assertThatThrownBy(() -> storage.storeDocument(1L, "x.pdf", null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("getDownloadUrl returns URL with TTL annotation")
    void getDownloadUrlAppendsTtl() {
        String url = storage.getDownloadUrl(1L, "minio://vetting/1/x.pdf", Duration.ofMinutes(15));

        assertThat(url).contains("ttl=900");
        assertThat(url).contains("minio://vetting/1/x.pdf");
    }

    @Test
    @DisplayName("getDownloadUrl null docId rejected")
    void getDownloadUrlRejectsNullDoc() {
        assertThatThrownBy(() -> storage.getDownloadUrl(1L, null, Duration.ofMinutes(15)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("getDownloadUrl zero/negative TTL rejected")
    void getDownloadUrlRejectsBadTtl() {
        assertThatThrownBy(() -> storage.getDownloadUrl(1L, "x", Duration.ZERO))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> storage.getDownloadUrl(1L, "x", Duration.ofMinutes(-1)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("deleteDocument is a no-op (Phase 1B foundation stub)")
    void deleteIsNoOp() {
        // Should not throw — concrete impl deferred per BR-VETTING-004.
        storage.deleteDocument(1L, "any-key");
    }
}
