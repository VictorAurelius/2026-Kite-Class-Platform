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
        "com.kitehub.subscription.notification.repository"
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
        "com.kitehub.subscription.notification.entity"
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
