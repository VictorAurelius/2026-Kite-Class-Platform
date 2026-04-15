package com.kiteclass.core.module.branding.storage;

import com.kiteclass.core.module.branding.entity.ResourceType;

import java.util.UUID;

/**
 * Centralised storage-path conventions for branding assets in MinIO.
 *
 * <p>Layout per ADR-005 / AI branding v2 redesign §2.4:
 * <pre>
 * kite-branding-assets/
 *   ├── static/{tenantId}/{type}/{filename}      (user upload; cache 30d+)
 *   ├── templates/{tenantId}/{type}/{hash}.png   (template composed; cache 7d)
 *   └── ai-generated/{tenantId}/{jobId}.png      (AI output; cache 1d, archive later)
 * </pre>
 *
 * <p>Kept as a pure-function helper so handlers stay handler-focused and path
 * conventions live in one place (easy to change when ops tunes cache tiers).
 *
 * @since 3.19.0 (Wave 3 Sub-PR 3.3, ADR-005)
 */
public final class BrandingStoragePaths {

    public static final String BUCKET = "kite-branding-assets";

    private BrandingStoragePaths() {
    }

    public static String staticPath(UUID tenantId, ResourceType type, String filename) {
        return String.format("static/%s/%s/%s",
                tenantId, type.name().toLowerCase(), filename);
    }

    public static String templatePath(UUID tenantId, ResourceType type, String hash) {
        return String.format("templates/%s/%s/%s.png",
                tenantId, type.name().toLowerCase(), hash);
    }

    public static String aiGeneratedPath(UUID tenantId, UUID aiJobId) {
        return String.format("ai-generated/%s/%s.png", tenantId, aiJobId);
    }
}
