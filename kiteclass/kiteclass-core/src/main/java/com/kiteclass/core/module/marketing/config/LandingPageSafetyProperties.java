package com.kiteclass.core.module.marketing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for landing page input safety (GAP-827).
 *
 * <p>Binds {@code landing.safety.*} keys. The image-host allowlist gates tenant-supplied
 * image URLs (heroImageUrl / logoUrl) so off-origin assets (script-bearing {@code .svg},
 * {@code .html} disguised as {@code .png}) cannot be persisted.
 *
 * <p>Default allowlist covers dev MinIO + local; production overrides via
 * {@code LANDING_SAFETY_ALLOWED_IMAGE_HOSTS} env (comma-separated) per
 * {@code production-env-config-registry.md}.
 *
 * @since wave-thesis-5 (GAP-827)
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "landing.safety")
public class LandingPageSafetyProperties {

    /**
     * Allowed hosts for tenant-supplied image/asset URLs. A URL passes validation only when
     * its scheme is {@code https} (or {@code http} for an explicitly-listed dev host) AND its
     * host exactly matches (or is a sub-domain of) an entry here.
     */
    private List<String> allowedImageHosts = new ArrayList<>(List.of(
            "localhost",
            "minio",
            "kite-minio",
            "cdn.kitehub.me",
            "assets.kitehub.me"));
}
