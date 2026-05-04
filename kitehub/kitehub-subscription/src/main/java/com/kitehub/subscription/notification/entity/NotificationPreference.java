package com.kitehub.subscription.notification.entity;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.notification.enums.NotificationChannelType;
import com.kitehub.subscription.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Per-user × per-NotificationType × Set&lt;Channel&gt; preference row.
 *
 * <p>Backed by table {@code notification_preferences} (V23 migration). Unique
 * key is {@code (user_id, notification_type)} per BR-NOTIF-004.</p>
 *
 * <p>Existing state — V18 (GAP-098, 2026-04-XX) added two boolean columns on
 * {@code instances} table for instance-level coarse preferences. This entity
 * supersedes that for user-level granularity; the V18 columns remain as legacy
 * fallback per the rules.md "Existing state" section.</p>
 *
 * <p>{@code enabledChannels} stored as a comma-separated string; converted to/from
 * {@link EnumSet} via {@link ChannelSetConverter}. Avoids a separate join table
 * for Phase 1 simplicity (4 enum values fit comfortably in a VARCHAR(64)).</p>
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
@Entity
@Table(
        name = "notification_preferences",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_notification_preferences_user_type",
                columnNames = {"user_id", "notification_type"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notification_preferences_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 32)
    private NotificationType notificationType;

    @Convert(converter = ChannelSetConverter.class)
    @Column(name = "enabled_channels", nullable = false, length = 64)
    @Builder.Default
    private Set<NotificationChannelType> enabledChannels = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
