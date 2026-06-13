package com.kitehub.subscription.notification.channel;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

/**
 * Channel-agnostic owner notification payload (GAP-1265).
 *
 * <p>Carries everything any {@link NotificationChannel} adapter needs: the durable
 * in-app banner content ({@code title}/{@code body}/{@code actionUrl}) AND the email
 * rendering inputs ({@code emailSubject}/{@code emailTemplate}/{@code emailVariables}).
 * The {@code notificationType} doubles as the per-type idempotency / admin-toggle key.</p>
 *
 * @author KiteHub Team
 * @since wave-kitehub-biz-100
 */
@Data
@Builder
public class OwnerNotification {

    /** Owning instance (idempotency tracking + in-app scoping). Nullable for system-level. */
    private UUID instanceId;

    /** Stable notification-type key (e.g. {@code payment-confirmed}, {@code winback-reactivate}). */
    private String notificationType;

    /** Owner email recipient. When null, the email channel is skipped (in-app only). */
    private String recipientEmail;

    private String organizationName;

    // ---- in-app banner content (durable fallback) ----
    private String title;
    private String body;
    /** Optional CTA URL the banner links to (e.g. billing page). */
    private String actionUrl;

    // ---- email rendering inputs ----
    private String emailSubject;
    private String emailTemplate;
    private Map<String, Object> emailVariables;
}
