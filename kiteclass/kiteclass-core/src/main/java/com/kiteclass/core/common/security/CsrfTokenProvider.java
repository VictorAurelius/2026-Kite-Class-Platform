package com.kiteclass.core.common.security;

/**
 * Issues + verifies CSRF tokens (double-submit cookie pattern per ADR-011).
 *
 * <p>Gateway-level CsrfTokenFilter (Sub-PR 4.2) wires this into state-changing routes.
 *
 * @since 3.23.0 (Wave 4 Sub-PR 4.0 scaffold)
 */
public interface CsrfTokenProvider {

    /** Mint a fresh token (opaque, high-entropy, tenant-scoped). */
    String issue();

    /**
     * @param token value submitted in request header (X-CSRF-Token)
     * @param cookie value submitted in cookie (double-submit)
     * @return true when tokens match and aren't expired
     */
    boolean verify(String token, String cookie);
}
