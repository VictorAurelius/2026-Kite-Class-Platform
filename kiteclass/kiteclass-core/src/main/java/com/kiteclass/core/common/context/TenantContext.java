package com.kiteclass.core.common.context;

import com.kiteclass.core.common.exception.TenantNotSetException;

import java.util.UUID;

/**
 * Thread-local storage for current tenant context.
 *
 * <p>Stores the current tenant ID (instanceId) in ThreadLocal to isolate data between tenants.
 * The tenant ID is extracted from X-Tenant-Id header by TenantFilterInterceptor.
 *
 * <p>Usage:
 * <pre>{@code
 * // Set current tenant (done by TenantFilterInterceptor)
 * TenantContext.setCurrentTenant(instanceId);
 *
 * // Get current tenant
 * UUID tenantId = TenantContext.getCurrentTenant();
 *
 * // Clear context (done in afterCompletion)
 * TenantContext.clear();
 * }</pre>
 *
 * <p>IMPORTANT: Always clear context after request completion to prevent memory leaks
 * and cross-request data leakage.
 *
 * @author KiteClass Team
 * @since 2.2.0
 * @see com.kiteclass.core.config.TenantFilterInterceptor
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private TenantContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Sets the current tenant ID for this thread.
     *
     * @param instanceId the tenant ID (must not be null)
     * @throws IllegalArgumentException if instanceId is null
     */
    public static void setCurrentTenant(UUID instanceId) {
        if (instanceId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        CURRENT_TENANT.set(instanceId);
    }

    /**
     * Gets the current tenant ID for this thread.
     *
     * @return the current tenant ID
     * @throws TenantNotSetException if tenant context is not set
     */
    public static UUID getCurrentTenant() {
        UUID tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new TenantNotSetException(
                "Tenant context not set for current thread. " +
                "Ensure X-Tenant-Id header is provided in request."
            );
        }
        return tenantId;
    }

    /**
     * Clears the tenant context for this thread.
     * Must be called after request completion to prevent memory leaks.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }

    /**
     * Checks if tenant context is set for current thread.
     *
     * @return true if tenant context is set, false otherwise
     */
    public static boolean isSet() {
        return CURRENT_TENANT.get() != null;
    }
}
