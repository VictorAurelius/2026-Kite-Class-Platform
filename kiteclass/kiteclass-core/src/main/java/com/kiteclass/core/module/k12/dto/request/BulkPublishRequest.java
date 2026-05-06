package com.kiteclass.core.module.k12.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Hiệu trưởng "publish all REVIEWED in one click" payload.
 *
 * <p>Cap at 500 ids per request to bound transaction time + memory footprint;
 * larger batches should be split client-side or via job queue (out of scope
 * for §360.4).
 *
 * @param gradeIds  SubjectGrade primary keys to attempt publishing
 *
 * @since 5.x (Wave 24 Bucket B — GAP-360 §360.4)
 */
public record BulkPublishRequest(
        @NotEmpty
        @Size(max = 500, message = "Bulk publish capped at 500 grades per request")
        List<Long> gradeIds) {
}
