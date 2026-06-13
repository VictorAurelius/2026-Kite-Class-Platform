package com.kitehub.subscription.notification.enums;

/**
 * Notification delivery channels per BR-NOTIF-002 in
 * {@code documents/01-business/kitehub/notification/rules.md}.
 *
 * <p>Phase 1 (Wave 18a Bucket B) only the {@link #EMAIL} channel has a wired
 * adapter ({@code SESEmailService}). The other values are stored forward-
 * compatibly: producers may persist preferences referencing them; the dispatcher
 * (Phase 2 GAP-063b) logs {@code channel.disabled.in.phase1} and skips per
 * BR-NOTIF-010.</p>
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
public enum NotificationChannelType {

    /** Phase 1 wired (SESEmailService). */
    EMAIL,

    /**
     * Phase 1 wired (GAP-1265) — persistent in-app banner fallback
     * ({@code InAppNotificationChannel} + {@code in_app_notifications} table).
     */
    IN_APP,

    /** Phase 2 deferred — Twilio / VNStack / FPT SMS adapter (GAP-063b). */
    SMS,

    /** Phase 2 deferred — Zalo Notification Service adapter (GAP-063b). */
    ZALO,

    /** Phase 2 deferred — Firebase Cloud Messaging push (GAP-063b). */
    PUSH
}
