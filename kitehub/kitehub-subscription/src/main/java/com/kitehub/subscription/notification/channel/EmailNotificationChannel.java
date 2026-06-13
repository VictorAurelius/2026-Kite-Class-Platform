package com.kitehub.subscription.notification.channel;

import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.notification.enums.NotificationChannelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * EMAIL adapter for the {@link NotificationChannel} seam (GAP-1265) — the primary, wired channel.
 *
 * <p>Delegates to {@link EmailServiceClient#sendTemplatedEmail} so it inherits the existing outbox
 * reliability path + per-type admin toggle. Skips (returns {@code false}) when the notification
 * carries no recipient email OR no email template (in-app-only notifications).</p>
 *
 * @author KiteHub Team
 * @since wave-kitehub-biz-100
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationChannel implements NotificationChannel {

    private final EmailServiceClient emailServiceClient;

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.EMAIL;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean deliver(OwnerNotification n) {
        if (n.getRecipientEmail() == null || n.getRecipientEmail().isBlank()
            || n.getEmailTemplate() == null || n.getEmailTemplate().isBlank()) {
            log.debug("Email channel skipped for {} — no recipient/template", n.getNotificationType());
            return false;
        }
        try {
            Map<String, Object> vars = n.getEmailVariables() != null ? n.getEmailVariables() : Map.of();
            emailServiceClient.sendTemplatedEmail(
                n.getInstanceId(), n.getNotificationType(), n.getRecipientEmail(),
                n.getEmailSubject(), n.getEmailTemplate(), vars);
            return true;
        } catch (Exception e) {
            log.warn("Email channel delivery failed for {}: {}", n.getNotificationType(), e.getMessage());
            return false;
        }
    }
}
