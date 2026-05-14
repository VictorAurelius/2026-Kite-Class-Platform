package com.kitehub.subscription.audit.login;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Bean wiring + async enablement for the login-audit sub-package (GAP-517).
 *
 * <p>Components in this package use stereotype annotations ({@link
 * org.springframework.stereotype.Service @Service},
 * {@link org.springframework.stereotype.Component @Component}) so they are
 * auto-discovered by Spring's component scan. This config class exists to
 * activate {@link EnableAsync} for the
 * {@link AdminLoginAlertEventListener#onAdminNewLoginFingerprint async event
 * listener}.</p>
 *
 * <p>If another part of the application has already enabled async (e.g. via
 * a parent {@code @EnableAsync}), Spring will use that single ProxyAsync
 * instance — repeated declarations are idempotent.</p>
 *
 * @since 1.0.0 (Wave 72b Bucket C GAP-517)
 */
@Configuration
@EnableAsync
public class LoginAuditConfig {
    // Marker config — no beans needed; @Service / @Component handles wiring.
}
