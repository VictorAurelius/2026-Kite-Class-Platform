package com.kiteclass.core.module.branding.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Composite branding package response — single round-trip payload for FE.
 *
 * <p>Per ADR-009: theme vars + resolved asset URLs + metadata. FE caches by ETag
 * (derived from {@code brandingVersion}). Server caches the object via Spring Cache
 * (Redis) and evicts on outbox events for {@code instance.deployed} or
 * {@code instance.regenerating}.
 *
 * <p>Implements {@link Serializable} so Redis serializer handles it cleanly.
 *
 * @since 3.20.0 (Wave 3 Sub-PR 3.4, ADR-009)
 */
public record BrandingPackage(
        Long instanceId,
        String tenantId,
        String slug,
        String frontendUrl,
        Integer brandingVersion,
        Instant deployedAt,
        List<AssetEntry> assets) implements Serializable {

    public record AssetEntry(
            String type,
            String category,
            String url,
            String alt) implements Serializable {
    }
}
