package com.kitehub.subscription.notification.service;

import com.kitehub.subscription.notification.enums.NotificationType;

/**
 * Thrown when a caller tries to remove the EMAIL channel from a
 * {@link NotificationType} flagged as mandatory per BR-NOTIF-005 / BR-NOTIF-008.
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
public class MandatoryTypeCannotBeDisabledException extends RuntimeException {

    public static final String ERROR_CODE = "MANDATORY_TYPE_CANNOT_BE_DISABLED";

    private final NotificationType type;

    public MandatoryTypeCannotBeDisabledException(NotificationType type) {
        super("Mandatory notification type cannot be disabled: " + type);
        this.type = type;
    }

    public NotificationType getType() {
        return type;
    }
}
