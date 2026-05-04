package com.kiteclass.core.module.childprotection.storage;

import com.kiteclass.core.common.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Phase 1B foundation stub for {@link VettingDocumentStorage}.
 *
 * <p><b>What this class IS:</b>
 * <ul>
 *   <li>A pinned-contract Spring bean satisfying the storage interface so
 *       upstream code (controller, future upload endpoint, future verify-
 *       queue UI) can compile + smoke-test against the abstraction.</li>
 *   <li>An audit-friendly trace — every call logs at INFO with the
 *       (vettingId, filename) tuple so the contract is exercised end-to-end
 *       in IT.</li>
 *   <li>Returns deterministic URLs of the form
 *       {@code minio://vetting/{vettingId}/{filename}} that callers can
 *       persist as evidence pointers.</li>
 * </ul>
 *
 * <p><b>What this class is NOT:</b>
 * <ul>
 *   <li>A real MinIO client — the SDK call, server-side encryption,
 *       signed-URL generation, and 7-year retention policy are deferred to
 *       Phase 1B follow-up.</li>
 *   <li>A security boundary — callers MUST still enforce RBAC (BR-VETTING-003)
 *       at the service layer; this stub does not authenticate.</li>
 * </ul>
 *
 * @since Wave 18b2 Bucket B — GAP-322b Phase 1B foundation; concrete impl
 *        deferred to Phase 1B follow-up
 */
@Slf4j
@Service
public class MinIOVettingDocumentStorageImpl implements VettingDocumentStorage {

    private static final String STORAGE_PREFIX = "minio://vetting";

    @Override
    public String storeDocument(Long vettingId, String filename, byte[] content) {
        validate(vettingId, filename, content);
        String safeName = sanitize(filename);
        String objectKey = STORAGE_PREFIX + "/" + vettingId + "/" + safeName;
        log.info("[stub] Stored vetting document vettingId={} filename={} size={}B → key={}",
                vettingId, safeName, content == null ? 0 : content.length, objectKey);
        return objectKey;
    }

    @Override
    public String getDownloadUrl(Long vettingId, String docId, Duration ttl) {
        if (vettingId == null) {
            throw new ValidationException("VETTING_ID_REQUIRED");
        }
        if (docId == null || docId.isBlank()) {
            throw new ValidationException("VETTING_DOC_ID_REQUIRED");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new ValidationException("VETTING_DOC_TTL_INVALID");
        }
        log.info("[stub] Issued download URL vettingId={} docId={} ttl={}s",
                vettingId, docId, ttl.toSeconds());
        // Stub URL — concrete impl will return a presigned MinIO URL.
        return docId + "?ttl=" + ttl.toSeconds();
    }

    @Override
    public void deleteDocument(Long vettingId, String docId) {
        log.info("[stub] Delete vetting document vettingId={} docId={} (no-op — Phase 1C enforces retention)",
                vettingId, docId);
    }

    private static void validate(Long vettingId, String filename, byte[] content) {
        if (vettingId == null) {
            throw new ValidationException("VETTING_ID_REQUIRED");
        }
        if (filename == null || filename.isBlank()) {
            throw new ValidationException("VETTING_DOC_FILENAME_REQUIRED");
        }
        if (content == null) {
            throw new ValidationException("VETTING_DOC_CONTENT_REQUIRED");
        }
    }

    /**
     * Strip path-traversal characters from filename to keep storage paths
     * predictable. Production impl MUST also enforce extension allow-list.
     */
    private static String sanitize(String filename) {
        return filename
                .replace("..", "_")
                .replace("/", "_")
                .replace("\\", "_");
    }
}
