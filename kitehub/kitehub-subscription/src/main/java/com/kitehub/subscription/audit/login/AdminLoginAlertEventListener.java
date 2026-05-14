package com.kitehub.subscription.audit.login;

import com.kitehub.subscription.client.EmailServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Listens for {@link AdminLoginNewFingerprintEvent} fired by
 * {@link LoginAuditService} and dispatches the {@code admin-new-login-alert}
 * transactional email via {@link EmailServiceClient}.
 *
 * <p>Decouples the login critical path from email I/O so the JWT response is
 * never blocked by alert dispatch. Email failures are logged and swallowed —
 * the audit row's {@code alertSent} flag remains true regardless to prevent
 * tight retry loops.</p>
 *
 * @since 1.0.0 (Wave 72b Bucket C GAP-517)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminLoginAlertEventListener {

    private final EmailServiceClient emailServiceClient;

    /**
     * Consume the new-fingerprint event + send the alert.
     *
     * <p>{@code @Async} ensures dispatch runs outside the login transaction +
     * outside the request thread. If the application's async executor is not
     * provisioned (legacy test profile), Spring still invokes the listener
     * synchronously and the email send remains best-effort.</p>
     */
    @Async
    @EventListener
    public void onAdminNewLoginFingerprint(AdminLoginNewFingerprintEvent event) {
        log.info("AdminLoginAlertEventListener: dispatching alert for userId={} auditLogId={}",
            event.getUserId(), event.getAuditLogId());
        try {
            emailServiceClient.sendAdminNewLoginAlert(
                event.getEmail(),
                event.getIp(),
                event.getUserAgent(),
                LocalDateTime.now());
        } catch (Exception ex) {
            // Defense-in-depth: EmailServiceClient swallows its own exceptions
            // already; this catch handles unexpected runtime errors only.
            log.warn("Admin login alert dispatch failed (continuing): userId={} err={}",
                event.getUserId(), ex.getMessage());
        }
    }
}
