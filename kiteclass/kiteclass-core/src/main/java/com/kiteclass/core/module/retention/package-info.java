/**
 * Data retention + GDPR deletion module (Wave 4 Sub-PR 4.4, ADR-013, GAP-073).
 *
 * <p>Key types:
 * <ul>
 *   <li>{@link com.kiteclass.core.module.retention.DeletionRequest} — entity, 7-day grace</li>
 *   <li>{@link com.kiteclass.core.module.retention.DeletionStatus} — state machine</li>
 *   <li>{@link com.kiteclass.core.module.retention.DeletionService} — lifecycle + scheduler</li>
 *   <li>{@link com.kiteclass.core.module.retention.DataExportService} — GDPR Art. 20 ZIP</li>
 *   <li>{@link com.kiteclass.core.module.retention.RetentionClassifier} — reflection-based
 *       lookup of {@link com.kiteclass.core.module.retention.Retention @Retention} on entities</li>
 *   <li>{@link com.kiteclass.core.module.retention.RetentionBucket} — 4 retention buckets</li>
 * </ul>
 */
package com.kiteclass.core.module.retention;
