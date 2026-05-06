package com.kiteclass.core.module.parent.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Admin request body for {@code POST /api/v1/admin/parent/consent/bulk-bump}.
 *
 * <p>Wave 24 — GAP-361 Phase 1C v1.5 — re-consent flow (BR-PARENT-PORTAL-016).
 *
 * @param newVersion  target version to bump records up to (must be ≥ 1)
 * @param reason      free-text rationale persisted in the audit log line
 *                    (e.g. "Privacy policy v2 — added homework facet")
 * @param effectiveAt timestamp the new policy took effect (informational —
 *                    actual bump happens at request handling time)
 *
 * @author KiteClass Team
 * @since 2.24.0 (Wave 24 — GAP-361 Phase 1C v1.5)
 */
public record BulkBumpConsentRequest(
        @NotNull @Min(1) Integer newVersion,
        @NotBlank @Size(max = 500) String reason,
        Instant effectiveAt) {
}
