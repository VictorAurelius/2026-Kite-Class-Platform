/**
 * Enrollment bulk-import feature (GAP-1104) — bulk-enroll students into classes
 * via xlsx upload.
 *
 * <p>Mirrors the student bulk-import module structure (parser / template generator
 * / service / controller / dto) but delegates each row to the existing
 * single-enroll transaction ({@code EnrollmentService.enrollStudent}).
 */
package com.kiteclass.core.module.enrollment.bulkimport;
