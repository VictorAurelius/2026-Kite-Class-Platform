package com.kitehub.subscription.notification.dto;

import com.kitehub.subscription.notification.entity.InAppNotification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Owner-facing in-app notification (persistent banner) projection (GAP-1265).
 *
 * @author KiteHub Team
 * @since wave-kitehub-biz-100
 */
@Data
@Builder
public class InAppNotificationResponse {

    private UUID id;
    private String notificationType;
    private String title;
    private String body;
    private String actionUrl;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public static InAppNotificationResponse fromEntity(InAppNotification n) {
        return InAppNotificationResponse.builder()
            .id(n.getId())
            .notificationType(n.getNotificationType())
            .title(n.getTitle())
            .body(n.getBody())
            .actionUrl(n.getActionUrl())
            .read(n.isRead())
            .createdAt(n.getCreatedAt())
            .readAt(n.getReadAt())
            .build();
    }
}
