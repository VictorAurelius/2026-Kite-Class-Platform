package com.kitehub.branding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Branding asset information.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandingAsset {

    /**
     * Asset type (profile, hero, logos, banners, og).
     */
    private String type;

    /**
     * Asset variant (e.g., cutout, circle, square for profile).
     */
    private String variant;

    /**
     * Asset URL (CDN or S3).
     */
    private String url;

    /**
     * File size in bytes.
     */
    private Long sizeBytes;

    /**
     * MIME type.
     */
    private String contentType;

    /**
     * Upload timestamp.
     */
    private Long uploadedAt;
}
