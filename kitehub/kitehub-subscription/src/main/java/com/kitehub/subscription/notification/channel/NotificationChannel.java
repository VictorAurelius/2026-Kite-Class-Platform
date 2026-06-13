package com.kitehub.subscription.notification.channel;

import com.kitehub.subscription.notification.enums.NotificationChannelType;

/**
 * Port for delivering an {@link OwnerNotification} over one channel (GAP-1265).
 *
 * <p>Strategy seam (per {@code design-patterns.md} §2 "Multiple implementations") so the
 * dispatcher can fan a notification across channels without hard-coding email. Phase 1 wires
 * {@link com.kitehub.subscription.notification.channel.EmailNotificationChannel} (primary) +
 * {@link com.kitehub.subscription.notification.channel.InAppNotificationChannel} (persistent
 * banner fallback). SMS / ZALO / PUSH adapters are documented stubs deferred to GAP-063b — see
 * {@link NotificationChannelType}; a future bean simply implements this port and registers.</p>
 *
 * @author KiteHub Team
 * @since wave-kitehub-biz-100
 */
public interface NotificationChannel {

    /** Which channel this adapter delivers over. */
    NotificationChannelType type();

    /**
     * Deliver the notification over this channel. Implementations are best-effort —
     * they MUST NOT throw; a delivery failure returns {@code false} (the dispatcher logs it).
     *
     * @param notification payload to deliver
     * @return {@code true} when delivery was dispatched/persisted, {@code false} on failure or skip
     */
    boolean deliver(OwnerNotification notification);
}
