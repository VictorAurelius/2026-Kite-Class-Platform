package com.kitehub.subscription.notification.channel;

import com.kitehub.subscription.notification.entity.InAppNotification;
import com.kitehub.subscription.notification.enums.NotificationChannelType;
import com.kitehub.subscription.notification.repository.InAppNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * IN_APP adapter for the {@link NotificationChannel} seam (GAP-1265) — durable persistent-banner
 * fallback. Persists an {@link InAppNotification} row so the owner always has a record even when
 * email is disabled or fails.
 *
 * <p>{@code REQUIRES_NEW}: this is a best-effort side-effect (per {@code audit-service-isolation.md}
 * / {@code design-patterns.md} §3.11) — persisting a banner must never roll back the caller's
 * payment-capture transaction. Requires {@code instanceId}; skips system-level notifications.</p>
 *
 * @author KiteHub Team
 * @since wave-kitehub-biz-100
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InAppNotificationChannel implements NotificationChannel {

    private final InAppNotificationRepository repository;

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.IN_APP;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean deliver(OwnerNotification n) {
        if (n.getInstanceId() == null) {
            log.debug("In-app channel skipped for {} — no instanceId", n.getNotificationType());
            return false;
        }
        try {
            InAppNotification banner = new InAppNotification();
            banner.setInstanceId(n.getInstanceId());
            banner.setNotificationType(n.getNotificationType());
            banner.setTitle(truncate(n.getTitle(), 200));
            banner.setBody(truncate(n.getBody(), 1000));
            banner.setActionUrl(truncate(n.getActionUrl(), 500));
            repository.save(banner);
            return true;
        } catch (Exception e) {
            log.warn("In-app channel persistence failed for {}: {}", n.getNotificationType(), e.getMessage());
            return false;
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
