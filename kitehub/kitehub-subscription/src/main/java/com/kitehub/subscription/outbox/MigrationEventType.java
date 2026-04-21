package com.kitehub.subscription.outbox;

/**
 * Canonical event names published via {@link MigrationOutboxEvent}.
 *
 * <p>Strings are stable contract — consumers (BillingService, KiteClass core cache,
 * BrandingService, EmailService, Alertmanager DLQ) match on these literals.</p>
 *
 * <p>See {@code documents/01-business/kitehub/trial-to-paid-migration/rules.md §5}
 * for the full payload schema + consumer mapping of each event.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192)
 */
public final class MigrationEventType {

    public static final String TRIAL_UPGRADE_INITIATED = "trial.upgrade.initiated";
    public static final String PAYMENT_CAPTURED = "payment.captured";
    public static final String INSTANCE_MIGRATED = "instance.migrated";
    public static final String BRANDING_REFRESH_REQUIRED = "branding.refresh.required";
    public static final String PAYMENT_REVERSED = "payment.reversed";
    public static final String MIGRATION_ROLLED_BACK = "migration.rolled_back";
    public static final String MIGRATION_FAILED = "migration.failed";

    /** Main topic for migration lifecycle events. */
    public static final String TOPIC_MIGRATION = "kitehub.migration";

    /** Sub-topic for branding-refresh events (separate queue for BrandingService). */
    public static final String TOPIC_BRANDING = "kitehub.branding";

    /** Dead-letter topic for MIGRATION_FAILED — ops alerting receiver. */
    public static final String TOPIC_MIGRATION_DLQ = "kitehub.migration.dlq";

    private MigrationEventType() {
        // constants holder
    }
}
