package com.kitehub.branding.tenant;

import java.util.UUID;

/**
 * Thread-local holder for the gateway-trusted tenant (instance) id + platform-admin flag of the
 * current request OR background job.
 *
 * <p><strong>GAP-1020 (Part 1 — RLS GUC):</strong> kitehub-branding shares the {@code kitehub}
 * database with kitehub-subscription. The branding tenant-scoped tables ({@code branding_jobs},
 * {@code branding_outbox}, {@code ai_usage_log}, {@code branding_instance_state},
 * {@code branding_lifecycle_events}, {@code branding_regenerate_usage}) carry Row-Level Security
 * policies keyed on the Postgres GUC {@code app.current_tenant_id} (migrations V34 + V58). Until
 * this class shipped, branding NEVER set that GUC, so RLS evaluated {@code NULL} and was inert —
 * masked locally only because the {@code kitehub} DB role owns the tables and bypasses non-forced
 * RLS. Under a production non-superuser role the policy would default-deny every query.</p>
 *
 * <p>The value is populated:
 * <ul>
 *   <li>per HTTP request by {@link TenantContextFilter} from the gateway-trusted
 *       {@code X-Tenant-Id} header (re-injected from the verified JWT {@code tenantId} claim by
 *       the gateway {@code TenantHeaderGuardFilter} — never client-supplied);</li>
 *   <li>per background branding job by {@code BrandingJobConsumer} via {@link #runAs} from the
 *       job's {@code instanceId} (consumers carry no HTTP request, so without this they would
 *       default-deny under a non-superuser role).</li>
 * </ul>
 *
 * <p>Mirrors the proven {@code com.kiteclass.core.common.context.TenantContext} pattern.</p>
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> PLATFORM_ADMIN = new ThreadLocal<>();

    private TenantContext() {
    }

    /** Set the current tenant (instance) id for this thread. */
    public static void setCurrentTenant(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /** @return the current tenant id, or {@code null} when no tenant is bound. */
    public static UUID getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    /** @return {@code true} when a tenant id is bound on this thread. */
    public static boolean isSet() {
        return CURRENT_TENANT.get() != null;
    }

    /** Mark the current request as a platform-admin (RLS cross-tenant bypass). */
    public static void setPlatformAdmin(boolean platformAdmin) {
        PLATFORM_ADMIN.set(platformAdmin);
    }

    /** @return {@code true} when the current request carries a platform-admin authority. */
    public static boolean isPlatformAdmin() {
        return Boolean.TRUE.equals(PLATFORM_ADMIN.get());
    }

    /** Clear all tenant context for this thread — MUST be called in a {@code finally} per request. */
    public static void clear() {
        CURRENT_TENANT.remove();
        PLATFORM_ADMIN.remove();
    }

    /**
     * Run a (checked-exception-throwing) action with the given tenant bound, restoring the prior
     * context afterwards. Used by background consumers that have no HTTP request to derive a tenant
     * from. The platform-admin flag is forced {@code false} inside the scope (a background job is
     * never an admin cross-tenant operation — it acts as the job's own instance).
     */
    public static void runAs(UUID tenantId, ThrowingRunnable action) throws Exception {
        UUID previousTenant = CURRENT_TENANT.get();
        Boolean previousAdmin = PLATFORM_ADMIN.get();
        try {
            CURRENT_TENANT.set(tenantId);
            PLATFORM_ADMIN.set(Boolean.FALSE);
            action.run();
        } finally {
            restore(previousTenant, previousAdmin);
        }
    }

    private static void restore(UUID tenant, Boolean admin) {
        if (tenant == null) {
            CURRENT_TENANT.remove();
        } else {
            CURRENT_TENANT.set(tenant);
        }
        if (admin == null) {
            PLATFORM_ADMIN.remove();
        } else {
            PLATFORM_ADMIN.set(admin);
        }
    }

    /** Runnable that may throw a checked exception (job processing rethrows for DLQ routing). */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
