package com.kitehub.subscription.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response envelope for {@code GET /api/v1/notification-preferences}.
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceListResponse {

    @Builder.Default
    private List<NotificationPreferenceDto> preferences = new ArrayList<>();
}
