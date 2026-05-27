package com.kiteclass.core.common.constant;

/**
 * Status of a {@link com.kiteclass.core.module.staff.entity.StaffInvitation}.
 *
 * <p>State transitions:
 * <ul>
 *   <li>{@link #PENDING} — created by an Owner; staff has not yet accepted.</li>
 *   <li>{@link #ACCEPTED} — staff completed redemption; gateway issued a STAFF
 *       role on their User row.</li>
 *   <li>{@link #EXPIRED} — scheduled job marked it expired after TTL elapsed
 *       (7-day default per {@code kiteclass.staff-invite.invitation-ttl-hours}).</li>
 *   <li>{@link #REVOKED} — Owner cancelled the invitation before redemption.</li>
 * </ul>
 *
 * @since 2026-05-27 (Wave meta-6 Bucket A — GAP-772)
 */
public enum StaffInvitationStatus {
    PENDING,
    ACCEPTED,
    EXPIRED,
    REVOKED
}
