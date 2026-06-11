package com.kiteclass.core.module.settings.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Regenerates a fresh presigned GET URL for a stored branding-asset URL on read.
 *
 * <p><b>GAP-1072 / GAP-1204:</b> the branding pipeline persists a presigned MinIO
 * URL (logo / favicon / hero) whose signature expires after the storage TTL
 * (7 days — the S3 SigV4 maximum). Once expired, the asset renders broken
 * (HTTP 403). Surfaces that serve these stored URLs (settings Branding,
 * marketing LandingPage) must re-derive a live URL from the stable object key on
 * every read instead of returning the persisted (possibly expired) value.
 *
 * <p>Extracted from {@code BrandingServiceImpl} so the marketing LandingPage
 * surface reuses the exact same regenerate-on-read logic (per
 * {@code cross-flow-bug-class-sweep.md} — one fix, every sister site). Stateless
 * + side-effect free: never writes the regenerated URL back to the DB.
 */
@Component
@Slf4j
public class BrandingAssetUrlResolver {

    private final BrandingAssetStorage brandingAssetStorage;

    public BrandingAssetUrlResolver(
            @Autowired(required = false) BrandingAssetStorage brandingAssetStorage) {
        this.brandingAssetStorage = brandingAssetStorage;
    }

    /**
     * Regenerate a fresh presigned GET URL for a stored branding-asset URL.
     *
     * <p>Graceful fallback — returns the stored URL unchanged (never throws) when:
     * storage is unavailable, the value is blank, it is not one of our presigned
     * URLs (external / non-presigned / static asset path), or regeneration fails.
     *
     * @param storedUrl the persisted (possibly expired) asset URL
     * @return a freshly presigned URL, or {@code storedUrl} unchanged
     */
    public String regenerate(String storedUrl) {
        if (storedUrl == null || storedUrl.isBlank() || brandingAssetStorage == null) {
            return storedUrl;
        }
        String objectKey = extractObjectKey(storedUrl);
        if (objectKey == null) {
            return storedUrl; // non-presigned / external / unparseable → keep as-is
        }
        try {
            return brandingAssetStorage.renderableUrl(objectKey);
        } catch (Exception ex) {
            log.warn("Failed to regenerate presigned branding asset URL; keeping stored URL. reason={}",
                    ex.getMessage());
            return storedUrl;
        }
    }

    /**
     * Extract the MinIO object key from a stored presigned branding-asset URL.
     *
     * <p>Stored shape:
     * {@code http://host:9100/<bucket>/static/<tenantId>/<type>/<file>?X-Amz-...}.
     * The key always begins with the {@code static/} prefix (per
     * {@link com.kiteclass.core.module.branding.storage.BrandingStoragePaths}), so
     * we anchor on {@code /static/} — robust to bucket name, host, port and
     * path-style access. {@link URI#getPath()} returns the percent-decoded path
     * (query stripped), which equals the original object key.
     *
     * @return the object key, or {@code null} when the URL is not one of our
     *         presigned assets ({@code X-Amz} absent) or cannot be parsed
     */
    private String extractObjectKey(String storedUrl) {
        // Defensive: only regenerate URLs we issued (presigned). Non-presigned column
        // values, static paths (/demo-banners/...) or external URLs are left untouched.
        if (!storedUrl.contains("X-Amz-")) {
            return null;
        }
        try {
            String path = URI.create(storedUrl).getPath();
            if (path == null) {
                return null;
            }
            int idx = path.indexOf("/static/");
            if (idx < 0) {
                return null;
            }
            return path.substring(idx + 1); // drop leading '/', keep "static/..."
        } catch (IllegalArgumentException ex) {
            log.warn("Stored branding asset URL not parseable as URI; keeping stored. reason={}",
                    ex.getMessage());
            return null;
        }
    }
}
