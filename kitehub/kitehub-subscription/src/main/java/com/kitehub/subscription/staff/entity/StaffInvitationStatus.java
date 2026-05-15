package com.kitehub.subscription.staff.entity;

/**
 * Lifecycle status for a {@link StaffInvitation}.
 *
 * <p>Whitelisted values enforced by V45 migration {@code ck_staff_invitations_status}
 * check constraint. Transitions:</p>
 *
 * <pre>
 *   PENDING --(recipient accepts)--&gt;  ACCEPTED  (terminal)
 *           --(TTL passes)----------&gt;  EXPIRED   (terminal, set by cron)
 *           --(owner cancels)-------&gt;  REVOKED   (terminal)
 * </pre>
 *
 * @since Wave 79 — GAP-561
 */
public enum StaffInvitationStatus {
    /** Invitation issued by owner; awaiting recipient action. */
    PENDING,
    /** Recipient set password + first login completed. Terminal. */
    ACCEPTED,
    /** {@code expires_at &lt; now} and never accepted. Terminal. */
    EXPIRED,
    /** Owner cancelled before recipient accepted. Terminal. */
    REVOKED
}
