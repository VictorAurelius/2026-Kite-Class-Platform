package com.kitehub.branding.domain.enums;

/**
 * Organization / user-type axis for the AI Branding wizard (GAP-1133).
 *
 * <p>Orthogonal to the <em>audience</em> axis (which drives theme/colour/tone).
 * This axis describes the tenant's organizational structure and drives the
 * asset strategy + tier hint — notably the number of portraits the wizard
 * collects (per GAP-1134):</p>
 *
 * <ul>
 *   <li>{@link #SOLO_TEACHER} — a single teacher → typically 1 portrait</li>
 *   <li>{@link #SMALL_CENTER} — a small centre → a handful of portraits</li>
 *   <li>{@link #LARGE_CENTER} — a large centre → many portraits</li>
 * </ul>
 *
 * <p>Stored on {@link com.kitehub.branding.domain.entity.BrandingJob} as a
 * nullable {@code VARCHAR(20)} column (optional for backward-compat — pre-GAP-1133
 * jobs carry {@code null}).</p>
 *
 * @since GAP-1133 (wizard user-type axis)
 */
public enum OrgType {

    SOLO_TEACHER,
    SMALL_CENTER,
    LARGE_CENTER;

    /**
     * Tolerant parse used at the controller boundary — the wizard FE sends the
     * enum string, but a stray case / unknown value must not 500 the request.
     *
     * @param raw incoming value (nullable / any case)
     * @return matching enum, or {@code null} when blank/unknown (treated as
     *         "not specified" — backward-compatible)
     */
    public static OrgType fromNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OrgType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Suggested maximum number of portrait uploads for this org-type
     * (GAP-1134 — drives the wizard portrait step cap). Solo teacher = 1,
     * small centre = 5, large centre = 20.
     *
     * @return suggested portrait cap
     */
    public int suggestedPortraitCount() {
        return switch (this) {
            case SOLO_TEACHER -> 1;
            case SMALL_CENTER -> 5;
            case LARGE_CENTER -> 20;
        };
    }
}
