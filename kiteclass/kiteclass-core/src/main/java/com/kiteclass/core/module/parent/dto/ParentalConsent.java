package com.kiteclass.core.module.parent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * PDPL Decree 13/2023 Art 16 granular parental-consent payload.
 *
 * <p>Stored as a JSONB blob on
 * {@link com.kiteclass.core.module.parent.entity.ParentStudentLink#parentalConsent}.
 * Shape:
 * <pre>
 * {
 *   "fields": { "fees": true, "conduct": false, ... },
 *   "version": 1,
 *   "updatedAt": "2026-05-05T12:34:56Z"
 * }
 * </pre>
 *
 * <p>Why a typed record instead of {@code Map<String, Object>}? — the
 * fields map is already heterogeneous (per-field booleans), so wrapping
 * version + updatedAt in their own typed slots avoids string-typed
 * downcasts at every read site. {@link Map} for the {@code fields} bag is
 * intentional: new facets ship without a migration and ConsentService
 * treats absence of a key as "consent not granted" (fail-safe per
 * BR-PARENT-PORTAL-011).
 *
 * <p>Bucket C v1 ships shape only. Re-consent flow on policy version bump
 * + admin tooling for bulk version bumps are deferred to the GAP-321c
 * follow-up filed at PR closure.
 *
 * @author KiteClass Team
 * @since 2.19.0 (Wave 19 — GAP-321c Phase 1C v1)
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ParentalConsent(
        Map<String, Boolean> fields,
        int version,
        Instant updatedAt) {

    /** Default value used by V56 migration + new entity rows. */
    public static ParentalConsent defaultValue() {
        return new ParentalConsent(new HashMap<>(), 1, null);
    }

    /**
     * Returns whether the parent has explicitly granted consent for the
     * named field. Missing key = no consent (fail-safe).
     *
     * @param field facet/field name (e.g., "fees", "conduct")
     * @return {@code true} only if {@code fields.get(field) == TRUE}
     */
    public boolean hasFieldConsent(String field) {
        if (fields == null || field == null) {
            return false;
        }
        return Boolean.TRUE.equals(fields.get(field));
    }
}
