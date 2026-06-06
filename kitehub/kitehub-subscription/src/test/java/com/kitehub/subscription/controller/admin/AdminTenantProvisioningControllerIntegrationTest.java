package com.kitehub.subscription.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.config.SecurityConfig;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.dto.RetryProvisioningRequest;
import com.kitehub.subscription.service.AdminTenantProvisioningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GAP-953 — Spring Security {@code @PreAuthorize} integration tests for
 * {@link AdminTenantProvisioningController}.
 *
 * <p>The {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")} guard only fires when the controller
 * bean flows through the Spring AOP proxy. This {@code @WebMvcTest} loads {@link SecurityConfig}
 * so the full filter chain + AOP advice executes — locking the UC-PROV-05 admin auth invariant
 * (anonymous 401 / non-admin 403 / PLATFORM_ADMIN 200) in CI.</p>
 *
 * @since Wave provisioning-1 Bucket E (GAP-953)
 */
@WebMvcTest(controllers = AdminTenantProvisioningController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminTenantProvisioningController — Spring Security @PreAuthorize integration")
class AdminTenantProvisioningControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private AdminTenantProvisioningService adminTenantProvisioningService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(adminTenantProvisioningService);
    }

    private RetryProvisioningRequest sampleRequest() {
        RetryProvisioningRequest req = new RetryProvisioningRequest();
        req.setReason("Instance stuck in INITIALIZING for 24h — manual retry");
        return req;
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Anonymous → 401")
    void anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/platform/admin/instances/{id}/retry-provisioning", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("OWNER (non-admin) → 403")
    void ownerRole_returns403() throws Exception {
        mockMvc.perform(post("/api/platform/admin/instances/{id}/retry-provisioning", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    @DisplayName("STAFF (non-admin) → 403")
    void staffRole_returns403() throws Exception {
        mockMvc.perform(post("/api/platform/admin/instances/{id}/retry-provisioning", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("PLATFORM_ADMIN → 200 OK")
    void platformAdmin_returns200() throws Exception {
        when(adminTenantProvisioningService.retryProvisioning(any(), any(), any()))
                .thenReturn(Mockito.mock(InstanceResponse.class));
        mockMvc.perform(post("/api/platform/admin/instances/{id}/retry-provisioning", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isOk());
    }
}
