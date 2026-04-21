package com.kitehub.subscription.webhook;

/**
 * Event types accepted by the migration webhook endpoint (GAP-192 Phase 4b-i).
 *
 * <p>See {@code documents/01-business/kitehub/trial-to-paid-migration/api-contract.md}
 * (POST /webhooks/payment) — gateway vendors post capture / reversal events using these
 * literals.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192 Phase 4b-i)
 */
public enum MigrationWebhookEventType {

    /** Payment authorised + captured — advances PAYMENT_PENDING → PAYMENT_CAPTURED. */
    PAYMENT_CAPTURED("payment.captured"),

    /** Payment reversed post-capture (chargeback / gateway rollback) — triggers rollback flow. */
    PAYMENT_REVERSED("payment.reversed");

    private final String wireName;

    MigrationWebhookEventType(String wireName) {
        this.wireName = wireName;
    }

    public String getWireName() {
        return wireName;
    }

    /**
     * Parse the dotted wire name (e.g. {@code "payment.captured"}) into the enum.
     * Returns {@code null} on unknown values — callers map that to HTTP 400.
     */
    public static MigrationWebhookEventType fromWireName(String wireName) {
        if (wireName == null) {
            return null;
        }
        for (MigrationWebhookEventType type : values()) {
            if (type.wireName.equalsIgnoreCase(wireName)) {
                return type;
            }
        }
        return null;
    }
}
