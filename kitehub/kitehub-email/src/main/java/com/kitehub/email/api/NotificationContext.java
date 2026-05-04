package com.kitehub.email.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Context bag for {@link NotificationChannel#send} — carries optional metadata that
 * channel adapters MAY consume (e.g., template name, branding instance id, locale,
 * tenant id, structured variables for templating).
 *
 * <p>Designed as a value object so callers can safely build with only what they need.
 * Phase 1 (GAP-063 Wave 18a Bucket B) only the EMAIL channel is wired; future
 * channels (SMS / ZALO / PUSH per GAP-063b) consume the same context with their
 * own selection logic.</p>
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationContext {

    /**
     * Subject line (email) or short title (SMS/Zalo/push).
     */
    private String subject;

    /**
     * Optional template name (Thymeleaf for email; Zalo template id for ZNS).
     */
    private String templateName;

    /**
     * Variables for the template engine (email) or structured payload (Zalo ZNS).
     */
    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();

    /**
     * Tenant instance id (KiteHub managed instance) — used to fetch branding,
     * choose sending domain, attribute cost. Null for cross-tenant system mail.
     */
    private Long instanceId;

    /**
     * Tenant id header value (multi-tenant isolation marker).
     */
    private String tenantId;

    /**
     * Locale for content (defaults to "vi-VN" for KiteClass tenants).
     */
    @Builder.Default
    private String locale = "vi-VN";
}
