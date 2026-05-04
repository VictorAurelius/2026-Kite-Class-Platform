package com.kiteclass.core.module.childprotection.storage;

import java.time.Duration;

/**
 * Storage contract for vetting evidence documents (LLTP scans, CCCD images,
 * bằng tốt nghiệp, ảnh 3×4). Pinned interface allows swapping concrete impl
 * without churning callers.
 *
 * <p><b>Phase 1B foundation:</b> only the contract + a stub impl
 * ({@link MinIOVettingDocumentStorageImpl}) that returns deterministic URLs
 * + logs at INFO. Concrete MinIO SDK wiring (server-side encryption, signed
 * URLs, lifecycle policies for 7-year retention) is deferred to Phase 1B
 * follow-up — this interface pins the contract early so future UI work can
 * proceed against the abstraction.
 *
 * <p><b>Strategy pattern</b> per project rule
 * {@code design-patterns.md} §1.1 — multiple implementations envisaged
 * (MinIO production / S3 / local file system for dev / mock for tests).
 *
 * <p><b>Compliance</b> (BR-VETTING-004):
 * <ul>
 *   <li>Bucket-level AES-256 server-side encryption (Phase 1B follow-up)</li>
 *   <li>Signed URLs with TTL (Phase 1B follow-up — caller will request
 *       short-lived access via {@link #getDownloadUrl})</li>
 *   <li>7-year retention enforcement on bucket lifecycle policy
 *       (GAP-322c Phase 1C)</li>
 * </ul>
 *
 * @since Wave 18b2 Bucket B — GAP-322b Phase 1B foundation
 */
public interface VettingDocumentStorage {

    /**
     * Persist a document for the given vetting record. The returned object
     * key locates the document inside the storage backend.
     *
     * @param vettingId the vetting record id (used to scope storage path)
     * @param filename  caller-provided filename (used in storage path; impl
     *                  must sanitize against path-traversal)
     * @param content   raw file bytes (encrypted at rest by storage backend)
     * @return opaque object key — caller persists alongside the vetting row
     *         (Phase 1B follow-up adds a {@code VettingDocument} child entity)
     */
    String storeDocument(Long vettingId, String filename, byte[] content);

    /**
     * Generate a short-lived URL that allows the safeguarding officer to
     * download the document. URL TTL bounded by caller — production impl
     * SHOULD enforce a maximum (e.g. 15 minutes) regardless of caller
     * request.
     *
     * @param vettingId the vetting record id
     * @param docId     opaque object key returned by {@link #storeDocument}
     * @param ttl       requested TTL (impl may cap)
     * @return temporary download URL
     */
    String getDownloadUrl(Long vettingId, String docId, Duration ttl);

    /**
     * Delete a document. Phase 1B foundation impl is a no-op; Phase 1C will
     * enforce 7-year retention preventing real delete on APPROVED/REJECTED
     * records.
     *
     * @param vettingId the vetting record id
     * @param docId     opaque object key returned by {@link #storeDocument}
     */
    void deleteDocument(Long vettingId, String docId);
}
