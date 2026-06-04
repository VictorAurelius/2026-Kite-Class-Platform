package com.kitehub.subscription.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.config.SecurityConfig;
import com.kitehub.subscription.dto.AdminConfirmPaymentRequest;
import com.kitehub.subscription.dto.AdminRejectPaymentRequest;
import com.kitehub.subscription.dto.PaymentResponse;
import com.kitehub.subscription.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GAP-940 — Spring Security {@code @PreAuthorize} integration tests for
 * {@link AdminPaymentController}.
 *
 * <p>Per-method {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")} annotations
 * (per PR #2152 GAP-938) only fire when the controller bean flows through the
 * Spring AOP proxy. Pure Mockito tests that {@code @InjectMocks} the controller
 * bypass the proxy and report PASS without the annotation actually being
 * enforced. This integration test loads {@link SecurityConfig} via
 * {@code @Import} so the full filter chain + AOP advice executes, locking the
 * UC-SUB-07 reconciliation flow auth invariant in CI.</p>
 *
 * @since Wave flow-kh3 (2026-06-04)
 */
@WebMvcTest(controllers = AdminPaymentController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminPaymentController — Spring Security @PreAuthorize integration")
class AdminPaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(paymentService);
    }

    // ─── GET /api/platform/admin/payments/pending ────────────────────────

    @Nested
    @DisplayName("GET /pending — list pending payments (UC-SUB-07)")
    class ListPending {

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous → 401")
        void anonymous_returns401() throws Exception {
            mockMvc.perform(get("/api/platform/admin/payments/pending"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER (non-admin) → 403")
        void ownerRole_returns403() throws Exception {
            mockMvc.perform(get("/api/platform/admin/payments/pending"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STAFF")
        @DisplayName("STAFF (non-admin) → 403")
        void staffRole_returns403() throws Exception {
            mockMvc.perform(get("/api/platform/admin/payments/pending"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PLATFORM_ADMIN")
        @DisplayName("PLATFORM_ADMIN → 200")
        void platformAdmin_returns200() throws Exception {
            when(paymentService.getPendingPayments()).thenReturn(List.of());
            mockMvc.perform(get("/api/platform/admin/payments/pending"))
                    .andExpect(status().isOk());
        }
    }

    // ─── POST /api/platform/admin/payments/{id}/confirm ──────────────────

    @Nested
    @DisplayName("POST /{id}/confirm — confirm pending payment (UC-SUB-07)")
    class ConfirmPayment {

        private AdminConfirmPaymentRequest sampleRequest() {
            AdminConfirmPaymentRequest req = new AdminConfirmPaymentRequest();
            req.setTransactionId("BANK-TX-20260604-001");
            return req;
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous → 401")
        void anonymous_returns401() throws Exception {
            mockMvc.perform(post("/api/platform/admin/payments/{id}/confirm", UUID.randomUUID())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER (non-admin) → 403")
        void ownerRole_returns403() throws Exception {
            mockMvc.perform(post("/api/platform/admin/payments/{id}/confirm", UUID.randomUUID())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PLATFORM_ADMIN")
        @DisplayName("PLATFORM_ADMIN → 200")
        void platformAdmin_returns200() throws Exception {
            when(paymentService.confirmPayment(any(), any()))
                    .thenReturn(Mockito.mock(PaymentResponse.class));
            mockMvc.perform(post("/api/platform/admin/payments/{id}/confirm", UUID.randomUUID())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRequest())))
                    .andExpect(status().isOk());
        }
    }

    // ─── POST /api/platform/admin/payments/{id}/reject ───────────────────

    @Nested
    @DisplayName("POST /{id}/reject — reject pending payment (UC-SUB-07)")
    class RejectPayment {

        private AdminRejectPaymentRequest sampleRequest() {
            AdminRejectPaymentRequest req = new AdminRejectPaymentRequest();
            req.setReason("Bank statement amount mismatch");
            return req;
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous → 401")
        void anonymous_returns401() throws Exception {
            mockMvc.perform(post("/api/platform/admin/payments/{id}/reject", UUID.randomUUID())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "STAFF")
        @DisplayName("STAFF (non-admin) → 403")
        void staffRole_returns403() throws Exception {
            mockMvc.perform(post("/api/platform/admin/payments/{id}/reject", UUID.randomUUID())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PLATFORM_ADMIN")
        @DisplayName("PLATFORM_ADMIN → 200")
        void platformAdmin_returns200() throws Exception {
            when(paymentService.rejectPayment(any(), any()))
                    .thenReturn(Mockito.mock(PaymentResponse.class));
            mockMvc.perform(post("/api/platform/admin/payments/{id}/reject", UUID.randomUUID())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleRequest())))
                    .andExpect(status().isOk());
        }
    }
}
