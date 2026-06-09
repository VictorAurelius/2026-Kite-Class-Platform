/**
 * Service layer for the enrollment bulk-import feature (GAP-1104).
 *
 * <p>{@code EnrollmentXlsxParser} parses the upload; {@code EnrollmentTemplateGenerator}
 * builds the downloadable template; {@code EnrollmentBulkImportService} orchestrates
 * resolve → validate → delegate to {@code EnrollmentService.enrollStudent} per row.
 */
package com.kiteclass.core.module.enrollment.bulkimport.service;
