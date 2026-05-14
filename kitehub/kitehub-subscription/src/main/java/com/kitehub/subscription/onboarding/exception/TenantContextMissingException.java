package com.kitehub.subscription.onboarding.exception;

/**
 * Thrown when an authenticated request lacks the {@code X-Tenant-Id} header
 * required to scope onboarding progress (Wave 78 GAP-538).
 *
 * <p>Maps to HTTP 403 {@code TENANT_CONTEXT_MISSING} per contract.</p>
 *
 * @since Wave 78 — GAP-538
 */
public class TenantContextMissingException extends RuntimeException {
    public TenantContextMissingException(String message) {
        super(message);
    }
}
