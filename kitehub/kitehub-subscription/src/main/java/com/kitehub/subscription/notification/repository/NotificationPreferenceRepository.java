package com.kitehub.subscription.notification.repository;

import com.kitehub.subscription.notification.entity.NotificationPreference;
import com.kitehub.subscription.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link NotificationPreference}. Queries always filter by
 * {@code userId} per BR-NOTIF-009 (tenant isolation).
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findByUserId(UUID userId);

    Optional<NotificationPreference> findByUserIdAndNotificationType(UUID userId, NotificationType type);
}
