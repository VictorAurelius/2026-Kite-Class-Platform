package com.kitehub.subscription.audit.login;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Fired by {@link LoginAuditService} when a PLATFORM_ADMIN user logs in from a
 * fingerprint not seen within the last 24h.
 *
 * <p>{@link AdminLoginAlertEventListener} consumes the event asynchronously and
 * dispatches the {@code admin-new-login-alert} transactional email via
 * {@link com.kitehub.subscription.client.EmailServiceClient}. The login response
 * NEVER waits on email send — alert is best-effort defense-in-depth.</p>
 *
 * @since 1.0.0 (Wave 72b Bucket C GAP-517)
 */
@Getter
@RequiredArgsConstructor
public class AdminLoginNewFingerprintEvent {

    /** Login-audit row id (DB pk). Listener uses this to flip {@code alert_sent}. */
    private final Long auditLogId;

    /** Admin user id (UUID). */
    private final UUID userId;

    /** Admin email (recipient of the alert). */
    private final String email;

    /** Source IP for the new login (mirrored from audit row). */
    private final String ip;

    /** User-Agent string for the new login. */
    private final String userAgent;
}
