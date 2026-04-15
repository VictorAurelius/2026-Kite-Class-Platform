package com.kiteclass.core.common.security;

/**
 * Gate outbound URLs (AI provider callbacks, webhook delivery targets) against a
 * per-tenant allowlist + default deny rules (private IP ranges, link-local).
 *
 * <p>Per ADR-011 §SSRF: private ranges BLOCKED by default —
 * 169.254/16, 10/8, 172.16/12, 192.168/16, loopback, localhost.
 *
 * <p>Implementation provided in Sub-PR 4.2; tenant allowlist config pending schema in 4.2.
 *
 * @since 3.23.0 (Wave 4 Sub-PR 4.0 scaffold)
 */
public interface UrlAllowlistValidator {

    /**
     * @param url absolute URL to validate (http:// or https://)
     * @param tenantId tenant whose allowlist governs (null → only default-deny rules)
     * @return true if URL is permitted to be contacted
     */
    boolean isAllowed(String url, String tenantId);
}
