package com.kiteclass.core.module.k12.dto.response;

import java.util.List;

/**
 * Result of a {@code POST /api/v1/grades/subjects/bulk-publish} call.
 *
 * <p>Per BR-GRADEBOOK-005 the operation is best-effort: invalid transitions
 * (e.g. DRAFT → PUBLISHED skipping REVIEWED) are skipped + reported, valid
 * REVIEWED → PUBLISHED transitions are applied. The status code is always 200
 * unless every id failed (then 207 Multi-Status not modelled here — clients
 * inspect counts).
 *
 * @param publishedCount  number of grades transitioned to PUBLISHED
 * @param skippedCount    number of grades rejected (already published or wrong state)
 * @param errors          list of {@code "<gradeId>: <errorCode>"} for client diagnostics
 *
 * @since 5.x (Wave 24 Bucket B — GAP-360 §360.4)
 */
public record BulkPublishResponse(
        int publishedCount,
        int skippedCount,
        List<String> errors) {
}
