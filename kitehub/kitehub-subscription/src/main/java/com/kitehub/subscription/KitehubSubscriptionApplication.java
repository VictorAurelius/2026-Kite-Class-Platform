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
    // Wave beta-readiness-4 Bucket B — GAP-353b PDPL immutable + hash chain
    "com.kitehub.subscription.consent.immutable",
    // Wave 26 Bucket A — GAP-353c PDPL Phase 2 DSAR self-service
    "com.kitehub.subscription.dsar.entity",
    // Wave 33 Bucket C — GAP-372 Phase 1 BETA invite mechanism
    "com.kitehub.subscription.beta.entity",
    // Wave 72a Bucket B — GAP-521 admin action audit log (OWASP A07)
    "com.kitehub.subscription.audit",
    // Wave 72b Bucket A — GAP-516 TOTP 2FA + recovery codes (OWASP A07)
    "com.kitehub.subscription.auth.twofactor",
    // Wave 78 Bucket B — GAP-538 onboarding progress tracker
    "com.kitehub.subscription.onboarding.entity",
    // Wave 78 Bucket F — GAP-542 feedback widget + survey scheduler
    "com.kitehub.subscription.feedback.entity",
    // Wave 79 Bucket B — GAP-561/562 staff invitation + RBAC (OWNER/STAFF)
    "com.kitehub.subscription.staff.entity",
    // Wave 79 Bucket F-bis — GAP-040 admin "View as tenant" impersonation
    "com.kitehub.subscription.impersonation",
    // Wave flow KH Enterprise sales-lead — GAP-1101 public /contact persist
    "com.kitehub.subscription.saleslead.entity"
})
@EnableJpaAuditing
@EnableScheduling
public class KitehubSubscriptionApplication {

    public static void main(String[] args) {
        SpringApplication.run(KitehubSubscriptionApplication.class, args);
    }
}
