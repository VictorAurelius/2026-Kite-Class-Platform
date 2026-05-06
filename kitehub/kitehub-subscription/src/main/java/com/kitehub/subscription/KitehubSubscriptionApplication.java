package com.kitehub.subscription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * KiteHub Subscription Service main application.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@SpringBootApplication
@EntityScan(basePackages = {
    "com.kitehub.platform.domain.entity",
    "com.kitehub.subscription.domain",
    "com.kitehub.subscription.outbox",
    "com.kitehub.subscription.idempotency",
    // Wave 18a Bucket B — GAP-063 Phase 1
    "com.kitehub.subscription.notification.entity",
    // Wave 25 Bucket A — GAP-353b PDPL Phase 2 server consent
    "com.kitehub.subscription.consent.entity",
    // Wave 26 Bucket A — GAP-353c PDPL Phase 2 DSAR self-service
    "com.kitehub.subscription.dsar.entity"
})
@EnableJpaAuditing
@EnableScheduling
public class KitehubSubscriptionApplication {

    public static void main(String[] args) {
        SpringApplication.run(KitehubSubscriptionApplication.class, args);
    }
}
