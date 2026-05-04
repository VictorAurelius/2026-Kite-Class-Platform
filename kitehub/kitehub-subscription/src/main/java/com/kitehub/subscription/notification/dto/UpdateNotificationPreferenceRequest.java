package com.kitehub.subscription.notification.dto;

import com.kitehub.subscription.notification.enums.NotificationChannelType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Request body for {@code PATCH /api/v1/notification-preferences/{type}}.
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationPreferenceRequest {

    @NotNull(message = "enabledChannels must be a (possibly empty) set, not null")
    @Builder.Default
    private Set<NotificationChannelType> enabledChannels = new LinkedHashSet<>();
}
