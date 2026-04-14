package com.kiteclass.core.module.branding.entity;

/**
 * Branding resource types (GAP-007, ADR-005).
 *
 * <p>Each type has a preferred default category (typically TEMPLATE for graphics,
 * STATIC for simple logos) — {@link ResourceRoutingService} chains classifiers to pick.
 *
 * @since 3.16.0
 */
public enum ResourceType {
    LOGO,
    FAVICON,
    BANNER,
    HERO,
    COURSE_THUMBNAIL,
    SOCIAL_COVER,
    EMAIL_HEADER
}
