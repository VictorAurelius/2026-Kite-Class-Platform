package com.kitehub.branding.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GAP-1020 (Part 1) — verifies the aspect issues {@code set_config('app.current_tenant_id', ...)}
 * inside a transaction when a tenant is bound, sets the admin-bypass GUC for platform admins, and
 * stays a no-op (default-deny) when no tenant context is present.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantAwareDataSourceInterceptor — sets RLS GUC at @Transactional boundary")
class TenantAwareDataSourceInterceptorTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @Mock
    private ProceedingJoinPoint pjp;

    private final TenantAwareDataSourceInterceptor interceptor = new TenantAwareDataSourceInterceptor();

    private final UUID tenantId = UUID.randomUUID();

    private void wireEntityManager() {
        ReflectionTestUtils.setField(interceptor, "entityManager", entityManager);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.setParameter(anyString(), anyString())).thenReturn(query);
        lenient().when(query.getSingleResult()).thenReturn(0);
    }

    private void beginTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
        // Marker resource bound by the aspect — unbind so other tests start clean.
        TransactionSynchronizationManager.getResourceMap().keySet().stream()
                .filter(k -> k instanceof String && ((String) k).contains("GUCSetForCurrentTx"))
                .findFirst()
                .ifPresent(TransactionSynchronizationManager::unbindResourceIfPossible);
        TenantContext.clear();
    }

    @Test
    @DisplayName("Tenant bound + active tx → set_config app.current_tenant_id issued")
    void setsTenantGucWhenBound() throws Throwable {
        wireEntityManager();
        beginTransaction();
        TenantContext.setCurrentTenant(tenantId);
        when(pjp.proceed()).thenReturn("OK");

        Object result = interceptor.setTenantGucIfNeeded(pjp);

        assertThat(result).isEqualTo("OK");
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sql.capture());
        assertThat(sql.getValue()).contains("set_config('app.current_tenant_id'");
        verify(query).setParameter(eq("tenantId"), eq(tenantId.toString()));
    }

    @Test
    @DisplayName("Platform admin + active tx → set_config app.is_platform_admin issued")
    void setsAdminBypassGuc() throws Throwable {
        wireEntityManager();
        beginTransaction();
        TenantContext.setPlatformAdmin(true);
        when(pjp.proceed()).thenReturn("OK");

        interceptor.setTenantGucIfNeeded(pjp);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sql.capture());
        assertThat(sql.getValue()).contains("set_config('app.is_platform_admin'");
    }

    @Test
    @DisplayName("No tenant context → default-deny, no set_config (proceeds normally)")
    void noGucWhenContextUnset() throws Throwable {
        // No EM wiring needed — aspect must short-circuit before touching the EntityManager.
        beginTransaction();
        when(pjp.proceed()).thenReturn("OK");

        Object result = interceptor.setTenantGucIfNeeded(pjp);

        assertThat(result).isEqualTo("OK");
        verify(entityManager, never()).createNativeQuery(anyString());
    }
}
