package com.kiteclass.core.module.settings.storage;

import com.kiteclass.core.module.branding.entity.ResourceType;

import java.util.UUID;

/**
 * Storage contract for tenant branding assets (logo, favicon).
 *
 * <p>Strategy pattern per {@code design-patterns.md} §1.1 — the interface
 * pins the contract so the concrete backend (MinIO/S3 production, local FS
 * for dev, mock for tests) can be swapped without churning callers.
 *
 * <p><b>GAP-804:</b> introduced to support direct multipart logo/favicon
 * upload from the FE branding settings page. The FE posts the raw file as
 * {@code multipart/form-data}; the controller hands the bytes here; this
 * contract stores them in MinIO and returns a URL the FE can render.
 *
 * <p>Distinct from the generic {@code StorageController} presigned-URL flow
 * (which requires {@code X-User-Reference-Id} and is fail-closed for not-yet
 * wired personas — GAP-798b). Branding upload is an OWNER-scoped admin path
 * that resolves the tenant from {@code TenantContext}, so it does not depend
 * on the storage presigned flow.
 *
 * @since GAP-804
 */
public interface BrandingAssetStorage {

    /**
     * Persist a branding asset for the given tenant and return a URL the FE
     * can render directly.
     *
     * @param tenantId    tenant instance id (resolved from {@code TenantContext})
     * @param type        asset type (LOGO / FAVICON)
     * @param filename    caller-provided filename (impl sanitizes against
     *                    path-traversal; VN diacritics preserved)
     * @param contentType MIME type reported by the client (used to set the
     *                    stored object {@code Content-Type})
     * @param content     raw file bytes
     * @return renderable URL pointing at the stored asset
     */
    String store(UUID tenantId, ResourceType type, String filename, String contentType, byte[] content);

    /**
     * Regenerate a fresh renderable (presigned GET) URL for an already-stored
     * asset, identified by its object key.
     *
     * <p><b>GAP-1072:</b> {@link #store} returns a presigned URL whose signature
     * expires after a fixed TTL (7 days — the S3 SigV4 maximum). Callers persist
     * that URL; once it expires the asset renders broken (HTTP 403). This method
     * re-derives a fresh presigned URL from the stable object key on every READ,
     * so the FE always receives a live URL without re-uploading.
     *
     * <p>Mirrors exactly the presign half of {@link #store} (same bucket, same
     * TTL, same presigner) — only the upload step is skipped.
     *
     * @param objectKey the MinIO object key (e.g. {@code static/{tenantId}/{type}/{file}})
     * @return a freshly presigned GET URL pointing at the stored asset
     */
    String renderableUrl(String objectKey);
}
