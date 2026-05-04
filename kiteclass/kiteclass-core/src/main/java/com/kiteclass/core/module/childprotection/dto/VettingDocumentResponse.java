package com.kiteclass.core.module.childprotection.dto;

/**
 * Response DTO for a successful vetting document upload (LLTP scan, CCCD,
 * etc.). Returned from {@code POST /api/v1/vettings/{vettingId}/documents}.
 *
 * <p>Phase 1B remainder (Wave 18b3) — single-file upload returns the
 * deterministic storage key the caller can persist alongside the vetting
 * record. A dedicated {@code vetting_document} child entity is deferred to
 * Phase 1C (GAP-322c) when audit-log + retention-enforcement rules ship.
 *
 * @param vettingId  the vetting record id this document belongs to
 * @param storageKey opaque object key in MinIO (format
 *                   {@code vetting/{vettingId}/{sanitized-filename}})
 * @param sizeBytes  number of bytes stored
 * @param contentType MIME type as declared by the client (Tika sniffing
 *                    deferred to Phase 1C)
 *
 * @since Wave 18b3 Bucket B — GAP-322b Phase 1B remainder
 */
public record VettingDocumentResponse(
        Long vettingId,
        String storageKey,
        long sizeBytes,
        String contentType
) {
}
