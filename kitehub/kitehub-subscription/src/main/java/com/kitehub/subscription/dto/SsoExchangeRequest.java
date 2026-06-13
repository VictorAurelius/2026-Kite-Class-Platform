package com.kitehub.subscription.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/auth/sso/exchange} (ADR-040 Option A,
 * GAP-1138).
 *
 * <p>The one-time {@code code} arrives ONLY in the JSON request body — never a
 * cookie, query string, or custom header. Requiring an {@code application/json}
 * body (enforced via the controller {@code consumes} attribute) is the CSRF
 * guard for this public exchange: a cross-site {@code <form>} auto-submit can
 * only produce {@code application/x-www-form-urlencoded} /
 * {@code multipart/form-data} / {@code text/plain} content types, none of which
 * match, so a forged request is rejected with 415 before reaching the handler.</p>
 *
 * @param code the one-time exchange code issued by {@code /sso/issue-code}
 * @since GAP-1138 (Wave RBAC-SSO 1)
 */
public record SsoExchangeRequest(@NotBlank String code) {
}
