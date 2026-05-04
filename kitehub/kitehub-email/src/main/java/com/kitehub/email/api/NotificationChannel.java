package com.kitehub.email.api;

/**
 * Strategy Pattern — swap notification provider via config.
 *
 * <p>Single entry-point for every notification producer in the platform. Direct
 * provider SDK calls (SES, Twilio, Zalo) are BANNED outside the adapter that
 * wraps them per business rule {@code BR-NOTIF-001} in
 * {@code documents/01-business/kitehub/notification/rules.md}.</p>
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@code SESEmailService} (Phase 1 — Wave 18a Bucket B) — EMAIL channel</li>
 *   <li>{@code SmsNotificationChannel} (Phase 2 — GAP-063b deferred)</li>
 *   <li>{@code ZaloNotificationChannel} (Phase 2 — GAP-063b deferred)</li>
 *   <li>{@code PushNotificationChannel} (Phase 2 — GAP-063b deferred)</li>
 * </ul>
 *
 * <p>Selected by: per-user {@code NotificationPreference} (kitehub-subscription)
 * lookup at send time (Phase 2 dispatcher); Phase 1 callers invoke the EMAIL
 * implementation directly via Spring autowiring of {@code SESEmailService}.</p>
 *
 * <p>Per design-patterns.md §1.1 this interface justifies the Strategy pattern
 * because there are ≥2 imminent implementations (Zalo/SMS in GAP-063b) on a
 * clear variation axis (delivery channel).</p>
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
public interface NotificationChannel {

    /**
     * Send a notification through this channel.
     *
     * @param recipient channel-specific recipient address — email for EMAIL,
     *                  E.164 phone for SMS, Zalo user id for ZALO, FCM token
     *                  for PUSH. Must not be {@code null} or blank.
     * @param message   message body — for EMAIL this is HTML when
     *                  {@code ctx.templateName} is null, otherwise serves as
     *                  the fallback body.
     * @param ctx       optional context (subject, template name, variables,
     *                  branding instance id, locale). Implementations MUST
     *                  tolerate a {@code null} {@link NotificationContext}.
     * @return result envelope describing outcome — never {@code null}.
     */
    NotificationSendResult send(String recipient, String message, NotificationContext ctx);

    /**
     * Channel identifier — must match a value of {@code NotificationChannelType}
     * enum (kitehub-subscription). Used by future dispatcher to route by
     * preference set.
     *
     * @return channel name (e.g., "EMAIL", "SMS", "ZALO", "PUSH")
     */
    String channelName();
}
