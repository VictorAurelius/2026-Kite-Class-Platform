/**
 * Bulk import feature for Student module (GAP-051 Wave 1 MVP).
 *
 * <p>Provides xlsx upload → parse → validate → batch-create pipeline for
 * onboarding 100s-1000s of students at once.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST {@code /api/v1/students/bulk-import/preview} — dry-run, no DB writes</li>
 *   <li>POST {@code /api/v1/students/bulk-import/commit} — parse + validate + create</li>
 *   <li>GET {@code /api/v1/students/bulk-import/jobs/&#123;id&#125;/errors} — download error report xlsx</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
package com.kiteclass.core.module.student.bulkimport;
