package com.kiteclass.core.module.settings.dto.response;

/**
 * Response for a single banner image upload (GAP-1211).
 *
 * <p>Unlike logo/favicon (which overwrite a single slot on the {@code Branding}
 * entity), a banner upload only stores the image and returns its renderable URL.
 * The FE appends this URL to the landing {@code heroImages} list and persists it
 * via the landing admin PUT — so the banner endpoint itself does not touch the
 * {@code Branding} row.
 *
 * <p>The returned {@code url} is a presigned MinIO GET URL whose stable object key
 * (prefixed {@code static/{tenantId}/banner/}) lets
 * {@link com.kiteclass.core.module.settings.storage.BrandingAssetUrlResolver}
 * regenerate a fresh signature on every landing read (GAP-1072 / GAP-1204) — the
 * presigned signature itself is never relied upon for persistence.
 *
 * @param url renderable (presigned) URL of the stored banner image
 * @since GAP-1211
 */
public record BannerUploadResponse(String url) {
}
