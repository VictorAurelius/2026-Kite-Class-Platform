package com.kiteclass.core.module.tenantsettings.controller;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.module.tenantsettings.dto.request.UpdateTenantSettingsRequest;
import com.kiteclass.core.module.tenantsettings.dto.response.TenantSettingsResponse;
import com.kiteclass.core.module.tenantsettings.service.TenantSettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TenantSettingsController} — focus on the cross-tenant
 * isolation guard (authz negative test per Bucket F task §6).
 *
 * @since Wave provisioning-1 (GAP-947)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantSettingsController Tests")
class TenantSettingsControllerTest {

    @Mock
    private TenantSettingsService tenantSettingsService;

    @InjectMocks
    private TenantSettingsController controller;

    private UUID currentTenant;

    @BeforeEach
    void setUp() {
        currentTenant = UUID.randomUUID();
        TenantContext.setCurrentTenant(currentTenant);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("GET own tenant settings → delegates to service")
    void getOwnTenant_ok() {
        when(tenantSettingsService.getSettings())
                .thenReturn(TenantSettingsResponse.builder().academicYear("2026-2027").build());

        ResponseEntity<ApiResponse<TenantSettingsResponse>> result = controller.getSettings(currentTenant);

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().isSuccess()).isTrue();
        assertThat(result.getBody().getData().getAcademicYear()).isEqualTo("2026-2027");
        verify(tenantSettingsService).getSettings();
    }

    @Test
    @DisplayName("GET another tenant's settings → PermissionDeniedException (cross-tenant IDOR)")
    void getOtherTenant_denied() {
        UUID otherTenant = UUID.randomUUID();

        assertThatThrownBy(() -> controller.getSettings(otherTenant))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("TENANT_ACCESS_DENIED");

        verify(tenantSettingsService, never()).getSettings();
    }

    @Test
    @DisplayName("PUT own tenant settings → delegates to service")
    void putOwnTenant_ok() {
        UpdateTenantSettingsRequest request = UpdateTenantSettingsRequest.builder().locale("en").build();
        when(tenantSettingsService.updateSettings(request))
                .thenReturn(TenantSettingsResponse.builder().locale("en").build());

        ResponseEntity<ApiResponse<TenantSettingsResponse>> result =
                controller.updateSettings(currentTenant, request);

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData().getLocale()).isEqualTo("en");
        verify(tenantSettingsService).updateSettings(request);
    }

    @Test
    @DisplayName("PUT another tenant's settings → PermissionDeniedException (cross-tenant IDOR)")
    void putOtherTenant_denied() {
        UUID otherTenant = UUID.randomUUID();
        UpdateTenantSettingsRequest request = UpdateTenantSettingsRequest.builder().locale("en").build();

        assertThatThrownBy(() -> controller.updateSettings(otherTenant, request))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("TENANT_ACCESS_DENIED");

        verify(tenantSettingsService, never()).updateSettings(request);
    }
}
