package com.kitehub.subscription.notification.entity;

import com.kitehub.platform.domain.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persistent in-app notification (persistent-banner) — the durable fallback channel for
 * {@link com.kitehub.subscription.notification.channel.OwnerNotification} (GAP-1265).
 *
 * <p>Written for every dispatched owner notification regardless of whether email succeeds, so
 * the owner always has a durable record/banner even when email delivery is disabled or fails.
 * Tenant isolation is enforced at the app layer (controller {@code TenantOwnershipGuard} +
 * service filter by {@code instanceId}); RLS at the DB layer is a documented follow-up (the
 * dispatch path runs in admin context where the request-tenant GUC is not the owner's tenant).</p>
 *
 * @author KiteHub Team
 * @since wave-kitehub-biz-100
 */
@Entity
@Table(name = "in_app_notifications", indexes = {
    @Index(name = "idx_in_app_notifications_instance_unread", columnList = "instance_id, is_read")
})
@Getter
@Setter
@NoArgsConstructor
public class InAppNotification extends BaseEntity {

    /** Owning instance (recipient scope). */
    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    /** Stable notification-type key (mirrors {@code OwnerNotification.notificationType}). */
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, length = 1000)
    private String body;

    /** Optional CTA URL (billing page, reactivate page). */
    @Column(name = "action_url", length = 500)
    private String actionUrl;

    /** Read/dismissed flag. {@code is_read} column ({@code read} is a SQL reserved word). */
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    /** Mark this banner read/dismissed. */
    public void markRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }
}
