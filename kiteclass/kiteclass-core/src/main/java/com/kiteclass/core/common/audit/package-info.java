/**
 * Append-only audit trail — shared foundation for Wave 4 security-sensitive modules.
 *
 * <p>Every mutation touching moderation decisions, DMCA takedowns, deletion requests,
 * or admin-override actions writes a row here. Rows are NEVER updated or deleted in
 * normal operation (semantic append-only; the {@code deleted} column exists only to
 * satisfy the {@code BaseEntity} + tenant-filter contract and is never flipped).
 *
 * <p>Consumed by:
 * <ul>
 *   <li>Sub-PR 4.1 Content Moderation</li>
 *   <li>Sub-PR 4.3 Legal / IP Protection</li>
 *   <li>Sub-PR 4.4 GDPR Deletion</li>
 * </ul>
 *
 * @since 3.23.0 (Wave 4 Sub-PR 4.0)
 */
package com.kiteclass.core.common.audit;
