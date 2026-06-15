package com.kiteclass.core.module.parent.notification.impl;

import com.kiteclass.core.common.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

/**
 * GAP-1413 — verifies the Zalo OA notification outbox row carries the REAL
 * request tenant ({@code instance_id}) resolved from {@link TenantContext},
 * not the former hardcoded nil-UUID stub that scoped every tenant's
 * notification to a single phantom tenant (RLS isolation hole).
 *
 * <p>Focused unit test (mocked {@link JdbcTemplate}) — captures the
 * {@code instance_id} bind arg of the INSERT and asserts it equals the
 * thread-local tenant, and differs across tenants.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ZaloOaNotificationServiceImpl — tenant resolver (GAP-1413)")
class ZaloOaNotificationServiceImplTest {

    private static final UUID TENANT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String NIL_UUID = "00000000-0000-0000-0000-000000000000";

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ZaloOaNotificationServiceImpl service;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("recordPaymentConfirm binds instance_id = request tenant (not nil-UUID)")
    void recordPaymentConfirm_uses_request_tenant() {
        TenantContext.setCurrentTenant(TENANT_A);

        service.recordPaymentConfirm(10L, 1L, 1500000L, "Trần Thị Hồng");

        // INSERT_SQL first bind arg = instance_id (?::uuid).
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(anyString(), argsCaptor.capture());
        Object[] binds = argsCaptor.getValue();

        assertThat(binds[0])
                .isEqualTo(TENANT_A.toString())
                .isNotEqualTo(NIL_UUID);
    }

    @Test
    @DisplayName("different tenant → different instance_id scope (isolation)")
    void recordPaymentConfirm_scopes_per_tenant() {
        TenantContext.setCurrentTenant(TENANT_B);

        service.recordParentInviteSent(20L, 5L);

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(anyString(), argsCaptor.capture());

        assertThat(argsCaptor.getValue()[0])
                .isEqualTo(TENANT_B.toString())
                .isNotEqualTo(TENANT_A.toString());
    }

    @Test
    @DisplayName("best-effort: tenant unset → no exception propagates to caller (swallowed)")
    void recordAttendanceAlert_unset_tenant_is_swallowed() {
        // No TenantContext set → resolveTenantId() throws TenantNotSetException internally;
        // REQUIRES_NEW + try/catch must swallow it so the caller's flow is never blocked.
        service.recordAttendanceAlert(30L, 7L, "PRESENT");
        // No exception = pass. (jdbcTemplate.update never reached — resolver throws first.)
    }
}
