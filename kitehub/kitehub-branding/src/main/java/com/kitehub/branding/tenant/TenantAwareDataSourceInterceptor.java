package com.kitehub.branding.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Sets the Postgres session-local GUCs {@code app.current_tenant_id} (and
 * {@code app.is_platform_admin} for admin requests) at the start of every Spring-managed
 * {@link org.springframework.transaction.annotation.Transactional @Transactional} boundary so the
 * Row-Level Security policies on branding tenant-scoped tables actually filter rows.
 *
 * <p><strong>GAP-1020 (Part 1):</strong> branding migrations V34 + V58 enable RLS on
 * {@code branding_jobs} / {@code branding_outbox} / {@code ai_usage_log} /
 * {@code branding_instance_state} / {@code branding_lifecycle_events} /
 * {@code branding_regenerate_usage} with policies of the form
 * {@code instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid}
 * (plus an {@code app.is_platform_admin} bypass after V75). Before this aspect, branding never
 * issued {@code set_config('app.current_tenant_id', ...)}, so the policy compared against
 * {@code NULL} → zero rows under a non-superuser DB role (latent because the {@code kitehub}
 * owner role bypasses non-forced RLS).</p>
 *
 * <h2>Why session-local ({@code SET LOCAL} via {@code set_config(..., true)})?</h2>
 * The setting auto-clears at transaction commit/rollback, so connection-pool reuse cannot leak the
 * value to the next request on the same physical connection.
 *
 * <h2>What happens if {@link TenantContext} is empty?</h2>
 * The aspect leaves the GUC unset. Combined with the policy's
 * {@code current_setting('app.current_tenant_id', true)} ({@code true} = return {@code NULL} for an
 * unset GUC instead of erroring), this yields default-deny: any query returns zero rows. This is
 * the desired posture for code paths that have not bound a tenant — a loud empty result beats a
 * silent cross-tenant query.
 *
 * <p>Mirrors {@code com.kiteclass.core.common.datasource.TenantAwareDataSourceInterceptor}
 * (GUC slice only — branding entities carry no Hibernate {@code @Filter}).</p>
 */
@Slf4j
@Aspect
@Component
public class TenantAwareDataSourceInterceptor {

    /** Resource key marking that the GUC was already set for the current physical transaction. */
    private static final String TENANT_GUC_SET_MARKER =
            "TenantAwareDataSourceInterceptor.GUCSetForCurrentTx";

    @PersistenceContext
    private EntityManager entityManager;

    @Around(
        "@annotation(org.springframework.transaction.annotation.Transactional) || "
        + "@within(org.springframework.transaction.annotation.Transactional) || "
        + "@annotation(jakarta.transaction.Transactional) || "
        + "@within(jakarta.transaction.Transactional)"
    )
    public Object setTenantGucIfNeeded(ProceedingJoinPoint pjp) throws Throwable {
        applyTenantGucIfPossible();
        return pjp.proceed();
    }

    private void applyTenantGucIfPossible() {
        boolean tenantBound = TenantContext.isSet();
        boolean admin = TenantContext.isPlatformAdmin();

        if (!tenantBound && !admin) {
            // Default-deny path: leave GUC unset; RLS NULL-compares → zero rows.
            return;
        }

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // No active transaction → SET LOCAL would have no effect; skip rather than emit a
            // session-wide SET that would leak across pooled connections.
            return;
        }

        if (Boolean.TRUE.equals(TransactionSynchronizationManager.getResource(TENANT_GUC_SET_MARKER))) {
            // Already set for the current physical transaction (nested @Transactional propagation).
            return;
        }

        if (admin) {
            entityManager
                .createNativeQuery("SELECT set_config('app.is_platform_admin', 'true', true)")
                .getSingleResult();
        }

        if (tenantBound) {
            UUID tenantId = TenantContext.getCurrentTenant();
            // Parameter binding via set_config() — no string concatenation. Third arg
            // is_local := true ≡ SET LOCAL (wiped at end of the current transaction).
            entityManager
                .createNativeQuery("SELECT set_config('app.current_tenant_id', :tenantId, true)")
                .setParameter("tenantId", tenantId.toString())
                .getSingleResult();
        }

        TransactionSynchronizationManager.bindResource(TENANT_GUC_SET_MARKER, Boolean.TRUE);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (TransactionSynchronizationManager.hasResource(TENANT_GUC_SET_MARKER)) {
                    TransactionSynchronizationManager.unbindResource(TENANT_GUC_SET_MARKER);
                }
            }
        });

        log.debug("Set app.current_tenant_id={} app.is_platform_admin={} (SET LOCAL via set_config)",
                TenantContext.getCurrentTenant(), admin);
    }
}
