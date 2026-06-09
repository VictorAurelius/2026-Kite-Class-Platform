/**
 * DTOs for the enrollment bulk-import feature (GAP-1104).
 *
 * <p>{@code EnrollmentBulkRow} = one parsed xlsx row; {@code EnrollmentBulkResult}
 * = preview/commit summary. Per-row errors reuse
 * {@code com.kiteclass.core.module.student.bulkimport.dto.RowError}.
 */
package com.kiteclass.core.module.enrollment.bulkimport.dto;
