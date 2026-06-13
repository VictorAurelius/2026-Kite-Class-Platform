package com.kitehub.subscription.notification.service;

import com.kitehub.subscription.notification.dto.InAppNotificationResponse;
import com.kitehub.subscription.notification.entity.InAppNotification;
import com.kitehub.subscription.notification.repository.InAppNotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Read + dismiss operations for persistent in-app notifications (GAP-1265 banner store).
 *
 * <p>All methods are instance-scoped — the controller binds {@code instanceId} to the caller's
 * tenant via {@code TenantOwnershipGuard} first, and {@link #markRead} re-checks ownership via the
 * tenant-scoped lookup so a guessed id from another tenant cannot be dismissed.</p>
 *
 * @author KiteHub Team
 * @since wave-kitehub-biz-100
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InAppNotificationService {

    private final InAppNotificationRepository repository;

    /** Banners for an instance (newest first). {@code unreadOnly=true} → only undismissed. */
    @Transactional(readOnly = true)
    public List<InAppNotificationResponse> list(UUID instanceId, boolean unreadOnly) {
        List<InAppNotification> rows = unreadOnly
            ? repository.findByInstanceIdAndReadFalseAndDeletedFalseOrderByCreatedAtDesc(instanceId)
            : repository.findByInstanceIdAndDeletedFalseOrderByCreatedAtDesc(instanceId);
        return rows.stream().map(InAppNotificationResponse::fromEntity).toList();
    }

    /** Mark one banner read/dismissed (tenant-scoped). */
    @Transactional
    public InAppNotificationResponse markRead(UUID instanceId, UUID notificationId) {
        InAppNotification n = repository
            .findByIdAndInstanceIdAndDeletedFalse(notificationId, instanceId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Notification not found: " + notificationId));
        if (!n.isRead()) {
            n.markRead();
            repository.save(n);
        }
        return InAppNotificationResponse.fromEntity(n);
    }
}
