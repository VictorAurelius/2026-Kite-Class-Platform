package com.kitehub.subscription.notification.dto;

import com.kitehub.subscription.notification.enums.NotificationChannelType;
import com.kitehub.subscription.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Notification preference response DTO (per api-contract.md).
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceDto {

    private NotificationType notificationType;

    @Builder.Default
    private Set<NotificationChannelType> enabledChannels = new LinkedHashSet<>();

    /** True if the EMAIL channel cannot be disabled per BR-NOTIF-008. */
    private boolean mandatory;
}
