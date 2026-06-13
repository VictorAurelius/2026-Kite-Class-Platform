package com.kitehub.subscription.dto;

/**
 * Response for {@code POST /api/v1/auth/sso/issue-code} (ADR-040 Option A,
 * GAP-1138 cross-product SSO KiteHub → KiteClass).
 *
 * <p>Carries ONLY the short-lived opaque one-time exchange code — never a raw
 * JWT. The KiteHub frontend appends {@code code} to the KiteClass callback URL
 * ({@code :3000/sso/callback?code=...}); KiteClass then exchanges it for a real
 * session via {@code POST /api/v1/auth/sso/exchange}. The opaque code, not the
 * JWT, is what travels through the browser URL (history / referer / logs) so the
 * signed token is never exposed.</p>
 *
 * @param code      single-use opaque code (256-bit URL-safe); consumed on first exchange
 * @param expiresIn TTL in seconds (≤60 per ADR-040)
 * @since GAP-1138 (Wave RBAC-SSO 1)
 */
public record SsoIssueCodeResponse(String code, long expiresIn) {
}
