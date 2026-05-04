package com.kitehub.subscription.notification.enums;

/**
 * Notification types per BR-NOTIF-003 in
 * {@code documents/01-business/kitehub/notification/rules.md}.
 *
 * <p>The {@code mandatory} flag (BR-NOTIF-005, BR-NOTIF-008) marks types whose
 * EMAIL channel cannot be disabled by the user — these are transactional
 * messages required for legal/operational reasons (billing receipts, security
 * alerts, trial expiration warnings).</p>
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
public enum NotificationType {

    /** Engagement: parent / student notified of class absence. */
    ABSENCE(false),

    /** Engagement: tuition fee due / overdue reminder. */
    FEE_REMINDER(false),

    /** Engagement: exam / assignment result published. */
    EXAM_RESULT(false),

    /** Mandatory: trial period ending (BR-NOTIF-005). */
    TRIAL_ENDING(true),

    /** Mandatory: invoice / payment receipt (BR-NOTIF-005). */
    BILLING_INVOICE(true),

    /** Mandatory: security event (login from new device, password change, etc.). */
    SECURITY_ALERT(true),

    /** Engagement: general announcement from school administration. */
    GENERAL_ANNOUNCEMENT(false);

    private final boolean mandatory;

    NotificationType(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * @return whether the EMAIL channel is mandatory for this type
     *         (cannot be disabled by user — see BR-NOTIF-008).
     */
    public boolean isMandatory() {
        return mandatory;
    }
}
