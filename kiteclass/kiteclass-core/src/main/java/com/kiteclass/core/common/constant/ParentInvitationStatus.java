package com.kiteclass.core.common.constant;

/**
 * Status of a {@link com.kiteclass.core.module.parent.entity.ParentInvitation}.
 *
 * <p>State transitions:
 * <ul>
 *   <li>{@link #PENDING} — created; parent has not yet redeemed the token.</li>
 *   <li>{@link #REDEEMED} — parent accepted; a Parent + link have been created.</li>
 *   <li>{@link #EXPIRED} — scheduled job marked it expired after TTL elapsed.</li>
 *   <li>{@link #REVOKED} — inviter cancelled the invitation before redemption.</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.14.0 (Wave 2 — GAP-052a)
 */
public enum ParentInvitationStatus {
    PENDING,
    REDEEMED,
    EXPIRED,
    REVOKED
}
