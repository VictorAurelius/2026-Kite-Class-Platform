package com.kitehub.subscription.controller.admin;

import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.dto.RetryProvisioningRequest;
import com.kitehub.subscription.service.AdminTenantProvisioningService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminTenantProvisioningController} (GAP-953).
 *
 * <p>Authz is enforced by {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")} at the Spring
 * Security AOP layer — see {@link AdminTenantProvisioningControllerIntegrationTest} for the
 * 401/403/200 chain. These pure-Mockito tests bypass the proxy and verify the controller
 * contract (header parsing, service delegation, exception propagation).</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminTenantProvisioningController")
class AdminTenantProvisioningControllerTest {

    @Mock
    private AdminTenantProvisioningService adminTenantProvisioningService;

    private AdminTenantProvisioningController controller;

    private UUID instanceId;
    private UUID adminUserId;

    @BeforeEach
    void setUp() {
        controller = new AdminTenantProvisioningController(adminTenantProvisioningService);
        instanceId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
    }

    @Test
    @DisplayName("happy path: parses X-User-Id + reason, returns 200")
    void retryHappy() {
        InstanceResponse instance = InstanceResponse.builder().id(instanceId).build();
        when(adminTenantProvisioningService.retryProvisioning(eq(instanceId), eq(adminUserId), eq("stuck")))
            .thenReturn(instance);

        RetryProvisioningRequest req = RetryProvisioningRequest.builder().reason("stuck").build();
        ResponseEntity<InstanceResponse> resp =
            controller.retryProvisioning(instanceId, adminUserId.toString(), req);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(instance);
        verify(adminTenantProvisioningService).retryProvisioning(instanceId, adminUserId, "stuck");
    }

    @Test
    @DisplayName("null body + missing header: passes null adminUserId + null reason")
    void retryNullBodyAndHeader() {
        when(adminTenantProvisioningService.retryProvisioning(eq(instanceId), isNull(), isNull()))
            .thenReturn(InstanceResponse.builder().id(instanceId).build());

        ResponseEntity<InstanceResponse> resp =
            controller.retryProvisioning(instanceId, null, null);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(adminTenantProvisioningService).retryProvisioning(instanceId, null, null);
    }

    @Test
    @DisplayName("invalid X-User-Id header parses to null (does not throw)")
    void retryInvalidHeader() {
        when(adminTenantProvisioningService.retryProvisioning(eq(instanceId), isNull(), isNull()))
            .thenReturn(InstanceResponse.builder().id(instanceId).build());

        ResponseEntity<InstanceResponse> resp =
            controller.retryProvisioning(instanceId, "not-a-uuid", null);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(adminTenantProvisioningService).retryProvisioning(instanceId, null, null);
    }

    @Test
    @DisplayName("missing instance propagates EntityNotFoundException (→ 404 via handler)")
    void retryNotFound() {
        when(adminTenantProvisioningService.retryProvisioning(eq(instanceId), eq(adminUserId), isNull()))
            .thenThrow(new EntityNotFoundException("Instance not found: " + instanceId));

        assertThatThrownBy(() ->
            controller.retryProvisioning(instanceId, adminUserId.toString(), null))
            .isInstanceOf(EntityNotFoundException.class);
    }
}
