package com.kitehub.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * KiteHub Admin Service - Platform management and analytics.
 *
 * @since 1.0
 */
@SpringBootApplication(scanBasePackages = {
        "com.kitehub.admin",
        "com.kitehub.platform",
        "com.kitehub.subscription"
})
@EnableJpaRepositories(basePackages = {
        // GAP-240: include all subscription repository packages — admin context
        // autowires repositories transitively via SubscriptionService → PaymentService →
        // SubscriptionEventEmitter (outbox) → MigrationIdempotencyKeyService.
        "com.kitehub.subscription.repository",
        "com.kitehub.subscription.outbox",
        "com.kitehub.subscription.idempotency",
        // Wave 18a Bucket B (GAP-063 Phase 1): NotificationPreferenceService is auto-scanned
        // via subscription's @ComponentScan; its repository must be JPA-enabled here too,
        // else admin context fails with UnsatisfiedDependencyException.
        "com.kitehub.subscription.notification.repository",
        // Wave 25 Bucket A (GAP-353b): ConsentService is auto-scanned via @ComponentScan;
        // its repository must be JPA-enabled here too.
        "com.kitehub.subscription.consent.repository",
        // Wave 26 Bucket A (GAP-353c): DsarService is auto-scanned via @ComponentScan;
        // its repository must be JPA-enabled here too.
        "com.kitehub.subscription.dsar.repository",
        // Wave 33 Bucket C (GAP-372): BetaAccessService auto-scanned via @ComponentScan;
        // repository must be JPA-enabled here per feedback_admin_scan_packages_after_module_add.md.
        "com.kitehub.subscription.beta.repository",
        // Wave 72a Bucket B (GAP-521): AdminAuditLogRepository — admin context pulls
        // AdminAuditAspect (it's a @Component) so its repository must be JPA-enabled too.
        "com.kitehub.subscription.audit",
        // Wave 72b Bucket A (GAP-516): RecoveryCodeRepository — admin context @ComponentScan
        // pulls TwoFactorEnrollmentService → RecoveryCodeService → RecoveryCodeRepository.
        "com.kitehub.subscription.auth.twofactor"
})
@EntityScan(basePackages = {
        // GAP-240: include all subscription entity packages — must mirror subscription's
        // own KitehubSubscriptionApplication scan list because admin pulls subscription
        // beans into its context for cross-service queries.
        "com.kitehub.platform.domain.entity",
        "com.kitehub.subscription.domain",
        "com.kitehub.subscription.outbox",
        "com.kitehub.subscription.idempotency",
        // Wave 18a Bucket B (GAP-063 Phase 1): NotificationPreference entity.
        "com.kitehub.subscription.notification.entity",
        // Wave 25 Bucket A (GAP-353b): ConsentRecord entity.
        "com.kitehub.subscription.consent.entity",
        // Wave 26 Bucket A (GAP-353c): DsarTicket entity.
        "com.kitehub.subscription.dsar.entity",
        // Wave 33 Bucket C (GAP-372): BetaAccessRequest entity.
        "com.kitehub.subscription.beta.entity",
        // Wave 72a Bucket B (GAP-521): AdminAuditLog entity.
        "com.kitehub.subscription.audit",
        // Wave 72b Bucket A (GAP-516): RecoveryCode entity (totp_* columns added to existing
        // platform User entity via V37 migration — no separate entity scan needed for User).
        "com.kitehub.subscription.auth.twofactor"
})
public class KiteHubAdminApplication {

    /**
     * Main entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(KiteHubAdminApplication.class, args);
    }
}
