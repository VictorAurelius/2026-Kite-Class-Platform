package com.kiteclass.core.common.datasource;

import com.kiteclass.core.common.context.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Sets the Postgres session-local GUC {@code app.current_tenant_id} at the start of every
 * Spring-managed {@link org.springframework.transaction.annotation.Transactional @Transactional}
 * boundary so that Row-Level Security (RLS) policies defined in
 * {@code V58__enable_rls_tenant_scoped_tables.sql} can filter rows at the database layer.
 *
 * <h2>Why session-local GUC?</h2>
 * The RLS policy reads {@code current_setting('app.current_tenant_id', true)} and rejects rows
 * where {@code instance_id} (or {@code tenant_id} for {@code kh-subscription.consent_record}) does
 * not match. The value is set per-transaction via {@code SET LOCAL ...} so that:
 * <ul>
 *   <li>The setting auto-clears when the transaction commits or rolls back.</li>
 *   <li>Connection-pool reuse cannot leak the value across requests
 *       (verified by {@code RLSEnforcementIT.shouldClearTenantOnConnectionRelease}).</li>
 * </ul>
 *
 * <h2>What happens if {@link TenantContext} is empty?</h2>
 * The aspect skips setting the GUC. Combined with the RLS policy's
 * {@code current_setting('app.current_tenant_id', true)} (the {@code true} flag returns
 * {@code NULL} for an unset GUC instead of throwing), this yields default-deny semantics:
 * any query returns zero rows. This is the desired behaviour for background jobs that have
 * not explicitly wrapped themselves with {@code TenantContext.runAs(tenantId, ...)} — the
 * loud failure (zero results) is preferable to a silent cross-tenant query.
 *
 * <h2>Scope</h2>
 * Pointcut targets:
 * <ul>
 *   <li>Any method annotated {@code @Transactional} (Spring or Jakarta).</li>
 *   <li>Any method on a class annotated {@code @Transactional} at type level.</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since GAP-466 / Wave 56
 * @see com.kiteclass.core.common.context.TenantContext
 */
@Slf4j
@Aspect
@Component
public class TenantAwareDataSourceInterceptor {

    /**
     * Resource key used with {@link TransactionSynchronizationManager} to record that the
     * GUC has already been set for the current transaction so we do not issue the same
     * {@code SET LOCAL} repeatedly when nested {@code @Transactional} methods participate
     * in the same physical transaction.
     */
    private static final String TENANT_GUC_SET_MARKER = "TenantAwareDataSourceInterceptor.GUCSetForCurrentTx";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Intercepts every Spring-managed transactional method and, if a tenant is present in
     * {@link TenantContext}, ensures the Postgres session-local GUC
     * {@code app.current_tenant_id} is set for the duration of the transaction.
     *
     * <p>The interceptor is idempotent across nested {@code @Transactional} calls because
     * the same physical transaction is reused and the GUC has already been set.</p>
     *
     * @param pjp the AspectJ join point representing the intercepted method
     * @return the return value of the intercepted method
     * @throws Throwable propagates any exception thrown by the intercepted method
     */
    @Around(
        "@annotation(org.springframework.transaction.annotation.Transactional) || " +
        "@within(org.springframework.transaction.annotation.Transactional) || " +
        "@annotation(jakarta.transaction.Transactional) || " +
        "@within(jakarta.transaction.Transactional)"
    )
    public Object setTenantGucIfNeeded(ProceedingJoinPoint pjp) throws Throwable {
        applyTenantGucIfPossible();
        return pjp.proceed();
    }

    private void applyTenantGucIfPossible() {
        if (!TenantContext.isSet()) {
            // Default-deny path: leave GUC unset; RLS policy NULL-compares and returns zero rows.
            return;
        }

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // No active transaction (e.g., the method is @Transactional(propagation = NOT_SUPPORTED)
            // or the aspect ran for a method that does not actually open a transaction). SET LOCAL
            // would have no effect, so skip rather than emit a session-wide SET.
            return;
        }

        if (Boolean.TRUE.equals(TransactionSynchronizationManager.getResource(TENANT_GUC_SET_MARKER))) {
            // Already set for the current physical transaction (nested @Transactional propagation).
            return;
        }

        UUID tenantId = TenantContext.getCurrentTenant();
        // Use parameter binding via set_config() to avoid string concatenation. The third arg
        // `is_local := true` is equivalent to `SET LOCAL ...` — the setting is wiped at the
        // end of the current transaction.
        entityManager
            .createNativeQuery("SELECT set_config('app.current_tenant_id', :tenantId, true)")
            .setParameter("tenantId", tenantId.toString())
            .getSingleResult();

        TransactionSynchronizationManager.bindResource(TENANT_GUC_SET_MARKER, Boolean.TRUE);
        TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (TransactionSynchronizationManager.hasResource(TENANT_GUC_SET_MARKER)) {
                        TransactionSynchronizationManager.unbindResource(TENANT_GUC_SET_MARKER);
                    }
                }
            }
        );

        log.debug("Set app.current_tenant_id = {} (SET LOCAL via set_config)", tenantId);
    }
}
