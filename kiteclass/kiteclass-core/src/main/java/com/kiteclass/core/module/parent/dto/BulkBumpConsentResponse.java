package com.kiteclass.core.module.parent.dto;

import java.time.Instant;

/**
 * Admin response body for {@code POST /api/v1/admin/parent/consent/bulk-bump}.
 *
 * <p>Wave 24 — GAP-361 Phase 1C v1.5 — re-consent flow (BR-PARENT-PORTAL-016).
 *
 * @param bumpedCount number of {@code parent_student_links} rows bumped
 *                    (records already at or above {@code newVersion} are
 *                    NOT touched and NOT included in this count)
 * @param newVersion  target version applied
 * @param effectiveAt timestamp the bump was applied at the server
 *
 * @author KiteClass Team
 * @since 2.24.0 (Wave 24 — GAP-361 Phase 1C v1.5)
 */
public record BulkBumpConsentResponse(
        int bumpedCount,
        int newVersion,
        Instant effectiveAt) {
}
