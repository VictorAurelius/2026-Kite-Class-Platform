package com.kiteclass.core.module.invoice.controller;

import com.kiteclass.core.module.invoice.service.RefundRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization tests for {@link RefundRequestController} (GAP-1491, OWASP A01).
 *
 * <p>All refund-request endpoints originally had NO {@code @PreAuthorize}; with
 * {@code SecurityConfig.anyRequest().permitAll()} a low-privilege role could approve/reject
 * a refund. The fix guards every endpoint at the financial tier
 * ({@code hasAnyRole('ADMIN','OWNER','PLATFORM_ADMIN','STAFF')}); reads add {@code TEACHER}.
 */
@WebMvcTest(RefundRequestController.class)
@AutoConfigureMockMvc
@Import({RefundRequestControllerAuthzTest.TestSecurityConfig.class, RefundRequestControllerAuthzTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("RefundRequestController @PreAuthorize role gate (GAP-1491, OWASP A01)")
class RefundRequestControllerAuthzTest {

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        RefundRequestService refundRequestService() {
            return Mockito.mock(RefundRequestService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RefundRequestService refundRequestService;

    private static final String TENANT_HEADER = UUID.randomUUID().toString();
    private static final Long REFUND_ID = 5L;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(refundRequestService);
    }

    @Test
    @DisplayName("OWNER → 200 on GET /refund-requests/{id} (read tier, service invoked)")
    @WithMockUser(roles = "OWNER")
    void getById_owner_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/refund-requests/{id}", REFUND_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(status().isOk());
        verify(refundRequestService).getRefundRequestById(REFUND_ID);
    }

    @Test
    @DisplayName("OWASP A01: STUDENT → denied PUT /refund-requests/{id}/approve (service NOT invoked)")
    @WithMockUser(roles = "STUDENT")
    void approve_student_denied() throws Exception {
        mockMvc.perform(put("/api/v1/refund-requests/{id}/approve", REFUND_ID)
                        .param("approvedBy", "1")
                        .header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "STUDENT approve"));
        verifyNoInteractions(refundRequestService);
    }

    @Test
    @DisplayName("OWASP A01: PARENT → denied PUT /refund-requests/{id}/reject (service NOT invoked)")
    @WithMockUser(roles = "PARENT")
    void reject_parent_denied() throws Exception {
        mockMvc.perform(put("/api/v1/refund-requests/{id}/reject", REFUND_ID)
                        .param("rejectedBy", "1")
                        .param("reason", "x")
                        .header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "PARENT reject"));
        verifyNoInteractions(refundRequestService);
    }

    private static void assertDenied(int statusCode, String label) {
        if (statusCode >= 200 && statusCode < 300) {
            throw new AssertionError(label + " must be denied by @PreAuthorize, got " + statusCode);
        }
    }
}
