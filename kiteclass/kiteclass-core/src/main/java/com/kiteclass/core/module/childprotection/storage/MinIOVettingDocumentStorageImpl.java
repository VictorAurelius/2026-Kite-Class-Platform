package com.kiteclass.core.module.childprotection.storage;

import com.kiteclass.core.common.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Duration;

/**
 * Real MinIO / S3-compatible storage for vetting evidence documents.
 *
 * <p><b>Wave 18b3 Bucket B (GAP-322b Phase 1B remainder):</b> replaces the
 * Wave 18b2 stub with concrete AWS SDK v2 wiring against the {@link S3Client}
 * + {@link S3Presigner} beans configured by {@code S3Config}. MinIO-compatible
 * (path-style access, custom endpoint).
 *
 * <p><b>Bucket scoping:</b> uses a dedicated vetting bucket
 * ({@code childprotection.minio.bucket}, default {@code kiteclass-vetting})
 * separate from the general user-uploads bucket. This keeps RBAC + lifecycle
 * policies (7-year retention enforced by the bucket admin in Phase 1C)
 * scoped narrowly to the special-protection class of data.
 *
 * <p><b>Key layout:</b> {@code vetting/{vettingId}/{sanitized-filename}}.
 * Path-traversal characters in the filename are stripped at the
 * {@link #sanitize} boundary before any S3 call.
 *
 * <p><b>Compliance</b> (BR-VETTING-004 + PDPL Decree 13/2023 Art 16):
 * <ul>
 *   <li>Server-side encryption — relies on bucket-default
 *       {@code SSE-S3} configured at the MinIO admin level (does not pass
 *       per-object encryption headers; bucket policy is the authoritative
 *       control).</li>
 *   <li>Presigned download URLs — capped at <b>15 minutes</b> regardless of
 *       caller-requested TTL; protects against long-lived URL leakage.</li>
 *   <li>7-year retention — enforced by bucket lifecycle policy (Phase 1C
 *       deliverable, GAP-322c). This impl does NOT block delete; the bucket
 *       policy will reject delete requests for the retention window.</li>
 * </ul>
 *
 * <p><b>Strategy pattern</b> per {@code design-patterns.md} §1.1 — {@link
 * VettingDocumentStorage} interface allows future swap (AWS S3 native /
 * local-filesystem-for-dev / mock-for-tests) without churning callers.
 *
 * @since Wave 18b3 Bucket B — GAP-322b Phase 1B remainder (replaces Wave 18b2 stub)
 */
@Slf4j
@Service
public class MinIOVettingDocumentStorageImpl implements VettingDocumentStorage {

    /** Object-key prefix scoping all vetting evidence under {@code vetting/}. */
    static final String KEY_PREFIX = "vetting";

    /**
     * Maximum TTL for presigned download URLs. Caller-requested TTLs above
     * this cap are clamped to {@code MAX_DOWNLOAD_TTL}. Per BR-VETTING-004 —
     * short-lived access reduces credential-leak blast radius.
     */
    static final Duration MAX_DOWNLOAD_TTL = Duration.ofMinutes(15);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public MinIOVettingDocumentStorageImpl(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${childprotection.minio.bucket:kiteclass-vetting}") String bucket
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    @Override
    public String storeDocument(Long vettingId, String filename, byte[] content) {
        validate(vettingId, filename, content);
        String safeName = sanitize(filename);
        String objectKey = KEY_PREFIX + "/" + vettingId + "/" + safeName;

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentLength((long) content.length)
                .build();

        s3Client.putObject(req, RequestBody.fromBytes(content));

        log.info("Stored vetting document vettingId={} key={} size={}B",
                vettingId, objectKey, content.length);
        return objectKey;
    }

    @Override
    public String getDownloadUrl(Long vettingId, String docId, Duration ttl) {
        if (vettingId == null) {
            throw new ValidationException("VETTING_ID_REQUIRED", new Object[0]);
        }
        if (docId == null || docId.isBlank()) {
            throw new ValidationException("VETTING_DOC_ID_REQUIRED", new Object[0]);
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new ValidationException("VETTING_DOC_TTL_INVALID", new Object[0]);
        }
        Duration capped = ttl.compareTo(MAX_DOWNLOAD_TTL) > 0 ? MAX_DOWNLOAD_TTL : ttl;

        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(docId)
                .build();
        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(capped)
                .getObjectRequest(get)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignReq);
        log.info("Issued presigned download URL vettingId={} key={} ttl={}s",
                vettingId, docId, capped.toSeconds());
        return presigned.url().toString();
    }

    @Override
    public void deleteDocument(Long vettingId, String docId) {
        if (vettingId == null) {
            throw new ValidationException("VETTING_ID_REQUIRED", new Object[0]);
        }
        if (docId == null || docId.isBlank()) {
            throw new ValidationException("VETTING_DOC_ID_REQUIRED", new Object[0]);
        }
        DeleteObjectRequest req = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(docId)
                .build();
        s3Client.deleteObject(req);
        log.info("Delete vetting document vettingId={} key={} (bucket retention policy may reject)",
                vettingId, docId);
    }

    private static void validate(Long vettingId, String filename, byte[] content) {
        if (vettingId == null) {
            throw new ValidationException("VETTING_ID_REQUIRED", new Object[0]);
        }
        if (filename == null || filename.isBlank()) {
            throw new ValidationException("VETTING_DOC_FILENAME_REQUIRED", new Object[0]);
        }
        if (content == null) {
            throw new ValidationException("VETTING_DOC_CONTENT_REQUIRED", new Object[0]);
        }
    }

    /**
     * Strip path-traversal characters from filename to keep storage paths
     * predictable. Production setup should also enforce extension allow-list
     * + size cap at the controller layer (done in {@code VettingController}
     * via {@code MultipartFile} validation).
     */
    private static String sanitize(String filename) {
        return filename
                .replace("..", "_")
                .replace("/", "_")
                .replace("\\", "_");
    }
}
