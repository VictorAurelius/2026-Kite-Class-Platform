package com.kitehub.subscription.auth.role;

import java.util.Set;

/**
 * Canonical Wave 79 platform role enum + backward-compat alias resolver.
 *
 * <p>Wave 79 GAP-562 introduces the OWNER / STAFF role separation. The
 * legacy values {@code PLATFORM_ADMIN} and {@code ADMIN} remain accepted in
 * the database during a 30-day backward-compat window (cutoff 2026-06-14)
 * and are aliased to {@link #OWNER} at authority-resolution time.</p>
 *
 * <p>Wave 81 (post-cutoff) will run a data-migration replacing legacy values
 * with the canonical {@code OWNER} string and drop the aliases here.</p>
 *
 * <p>Schema source-of-truth: {@code documents/01-business/roles/rules.md}.</p>
 *
 * @since Wave 79 — GAP-562
 */
public enum PlatformRole {

    /** Tenant owner — full access (billing, branding, staff management, AI). */
    OWNER,

    /** Invited staff — scoped operational access; cannot touch billing/branding/AI. */
    STAFF;

    /** String value cutoff date when legacy aliases are removed (Wave 81). */
    public static final String ALIAS_CUTOFF_DATE = "2026-06-14";

    private static final Set<String> OWNER_ALIASES = Set.of(
            "OWNER",
            "PLATFORM_ADMIN", // legacy — Wave 81 cleanup
            "ADMIN"            // legacy — Wave 81 cleanup
    );

    private static final Set<String> STAFF_ALIASES = Set.of(
            "STAFF"
    );

    /**
     * Resolve a stored role value (may be legacy or canonical) to the
     * canonical {@link PlatformRole} enum.
     *
     * @param storedValue value from {@code users.role} column
     * @return canonical role, or {@code null} if unknown
     */
    public static PlatformRole fromStoredValue(String storedValue) {
        if (storedValue == null) {
            return null;
        }
        String upper = storedValue.trim().toUpperCase();
        if (OWNER_ALIASES.contains(upper)) {
            return OWNER;
        }
        if (STAFF_ALIASES.contains(upper)) {
            return STAFF;
        }
        return null;
    }

    /** Spring Security authority string for this role. */
    public String authority() {
        return "ROLE_" + this.name();
    }

    public boolean isOwner() {
        return this == OWNER;
    }

    public boolean isStaff() {
        return this == STAFF;
    }
}
