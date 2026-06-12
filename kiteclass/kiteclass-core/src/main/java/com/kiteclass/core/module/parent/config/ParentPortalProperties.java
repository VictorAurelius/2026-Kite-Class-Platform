package com.kiteclass.core.module.parent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for the parent portal feature.
 *
 * <p>Bound from {@code kiteclass.parent-portal.*} in {@code application.yml}.
 * The feature flag ({@link #enabled}) exists because Wave 2 ships identity
 * only — messaging, fee payment, and the dashboard widgets land in Wave 5,
 * at which point marketing / PDPL wording will be finalised.
 *
 * @param enabled              master switch; when {@code false} the invitation and
 *                             self-service endpoints return {@code 503}.
 * @param invitationTtlHours   token lifetime — default 24 h per BR-PARENT-003.
 * @param redeemBaseUrl        absolute URL prefix for the email link (e.g.
 *                             {@code https://{tenant}.kitehub.me/parent-invite/}).
 *                             The token is appended at send time.
 * @since 2.14.0
 */
@ConfigurationProperties(prefix = "kiteclass.parent-portal")
public record ParentPortalProperties(
        boolean enabled,
        int invitationTtlHours,
        String redeemBaseUrl
) {

    /**
     * Applies sane defaults when the YAML block is missing entirely — keeps
     * tests that bootstrap with minimal config working without mandatory
     * property stubs.
     */
    public ParentPortalProperties {
        if (invitationTtlHours <= 0) {
            invitationTtlHours = 24;
        }
        if (redeemBaseUrl == null || redeemBaseUrl.isBlank()) {
            redeemBaseUrl = "https://app.kitehub.me/parent-invite/";
        }
    }
}
