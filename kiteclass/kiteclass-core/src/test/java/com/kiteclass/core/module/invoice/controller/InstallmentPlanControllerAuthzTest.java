package com.kiteclass.core.module.invoice.controller;

import com.kiteclass.core.module.invoice.service.InstallmentPlanService;
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
 * Web-slice authorization tests for {@link InstallmentPlanController} (GAP-1491, OWASP A01).
 *
 * <p>Installment-plan endpoints originally had NO {@code @PreAuthorize}; with
 * {@code SecurityConfig.anyRequest().permitAll()} a low-privilege role could approve plans
 * or record payments. The fix guards every endpoint at the financial tier
 * ({@code hasAnyRole('ADMIN','OWNER','PLATFORM_ADMIN','STAFF')}); reads add {@code TEACHER}.
 */
@WebMvcTest(InstallmentPlanController.class)
@AutoConfigureMockMvc
@Import({InstallmentPlanControllerAuthzTest.TestSecurityConfig.class, InstallmentPlanControllerAuthzTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("InstallmentPlanController @PreAuthorize role gate (GAP-1491, OWASP A01)")
class InstallmentPlanControllerAuthzTest {

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
        InstallmentPlanService installmentPlanService() {
            return Mockito.mock(InstallmentPlanService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstallmentPlanService installmentPlanService;

    private static final String TENANT_HEADER = UUID.randomUUID().toString();
    private static final Long PLAN_ID = 9L;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(installmentPlanService);
    }

    @Test
    @DisplayName("OWNER → 200 on GET /installment-plans/{id} (read tier, service invoked)")
    @WithMockUser(roles = "OWNER")
    void getById_owner_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/installment-plans/{id}", PLAN_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(status().isOk());
        verify(installmentPlanService).getInstallmentPlanById(PLAN_ID);
    }

    @Test
    @DisplayName("OWASP A01: STUDENT → denied PUT /installment-plans/{id}/approve (service NOT invoked)")
    @WithMockUser(roles = "STUDENT")
    void approve_student_denied() throws Exception {
        mockMvc.perform(put("/api/v1/installment-plans/{id}/approve", PLAN_ID)
                        .param("approvedBy", "1")
                        .header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "STUDENT approve"));
        verifyNoInteractions(installmentPlanService);
    }

    @Test
    @DisplayName("OWASP A01: PARENT → denied PUT /installment-plans/{id}/reject (service NOT invoked)")
    @WithMockUser(roles = "PARENT")
    void reject_parent_denied() throws Exception {
        mockMvc.perform(put("/api/v1/installment-plans/{id}/reject", PLAN_ID)
                        .param("reason", "x")
                        .header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "PARENT reject"));
        verifyNoInteractions(installmentPlanService);
    }

    private static void assertDenied(int statusCode, String label) {
        if (statusCode >= 200 && statusCode < 300) {
            throw new AssertionError(label + " must be denied by @PreAuthorize, got " + statusCode);
        }
    }
}
