package com.kitehub.subscription.service;

import com.kitehub.subscription.audit.TenantAuditService;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.service.migration.SubscriptionEventEmitter;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminTenantProvisioningService} (GAP-953, Wave provisioning-1 Bucket E).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminTenantProvisioningService")
class AdminTenantProvisioningServiceTest {

    @Mock
    private InstanceService instanceService;
    @Mock
    private SubscriptionEventEmitter tenantEventEmitter;
    @Mock
    private TenantAuditService tenantAuditService;

    private AdminTenantProvisioningService service;

    private UUID instanceId;
    private UUID adminUserId;

    @BeforeEach
    void setUp() {
        service = new AdminTenantProvisioningService(
            instanceService, tenantEventEmitter, tenantAuditService);
        instanceId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
    }

    @Test
    @DisplayName("happy path: re-publishes tenant.created + writes retry audit row")
    void retryProvisioningHappy() {
        InstanceResponse instance = InstanceResponse.builder()
            .id(instanceId)
            .subdomain("sky-edu")
            .slug("sky-edu")
            .build();
        when(instanceService.getInstanceById(instanceId)).thenReturn(instance);

        InstanceResponse result = service.retryProvisioning(instanceId, adminUserId, "stuck 24h");

        assertThat(result).isSameAs(instance);

        // tenant.created re-published with the saga-expected field shape.
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(tenantEventEmitter).emit(eq(instanceId),
            eq(AdminTenantProvisioningService.TENANT_CREATED_EVENT),
            eq(AdminTenantProvisioningService.TENANT_CREATED_TOPIC),
            payload.capture());
        assertThat(payload.getValue())
            .contains("\"tenantId\":\"" + instanceId + "\"")
            .contains("\"slug\":\"sky-edu\"")
            .contains("\"audience\":")
            .contains("\"tone\":");

        // Audit row recorded for the retry action.
        verify(tenantAuditService).recordTenantRetryRequested(instanceId, adminUserId, "stuck 24h");
    }

    @Test
    @DisplayName("falls back to subdomain when slug is null")
    void retryProvisioningFallsBackToSubdomain() {
        InstanceResponse instance = InstanceResponse.builder()
            .id(instanceId)
            .subdomain("quang-minh")
            .slug(null)
            .build();
        when(instanceService.getInstanceById(instanceId)).thenReturn(instance);

        service.retryProvisioning(instanceId, adminUserId, null);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(tenantEventEmitter).emit(eq(instanceId), any(), any(), payload.capture());
        assertThat(payload.getValue()).contains("\"slug\":\"quang-minh\"");
    }

    @Test
    @DisplayName("missing instance propagates EntityNotFoundException + no event/audit")
    void retryProvisioningMissingInstance() {
        when(instanceService.getInstanceById(instanceId))
            .thenThrow(new EntityNotFoundException("Instance not found: " + instanceId));

        assertThatThrownBy(() -> service.retryProvisioning(instanceId, adminUserId, null))
            .isInstanceOf(EntityNotFoundException.class);

        verify(tenantEventEmitter, never()).emit(any(UUID.class), any(), any(), any());
        verify(tenantAuditService, never()).recordTenantRetryRequested(any(), any(), any());
    }
}
