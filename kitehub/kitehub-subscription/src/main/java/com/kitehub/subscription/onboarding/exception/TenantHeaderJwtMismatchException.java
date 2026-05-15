package com.kitehub.subscription.onboarding.exception;

/**
 * Thrown when the gateway-forwarded {@code X-Tenant-Id} header disagrees with
 * the {@code tenantId} claim carried inside the caller's JWT (Wave 79 GAP-554 —
 * defense-in-depth against gateway-bypass tenant spoof).
 *
 * <p>Maps to HTTP 403 {@code TENANT_HEADER_JWT_MISMATCH}.</p>
 *
 * @since Wave 79 — GAP-554
 */
public class TenantHeaderJwtMismatchException extends RuntimeException {
    public TenantHeaderJwtMismatchException(String message) {
        super(message);
    }
}
