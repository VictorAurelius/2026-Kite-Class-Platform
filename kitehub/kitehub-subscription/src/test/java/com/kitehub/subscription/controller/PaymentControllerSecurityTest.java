package com.kitehub.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.platform.domain.enums.PaymentMethod;
import com.kitehub.subscription.config.SecurityConfig;
import com.kitehub.subscription.dto.CreatePaymentRequest;
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
import org.springframework.data.domain.PageImpl;
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
 * RBAC security tests for {@link PaymentController} (GAP-562b Wave 80 Bucket C).
 *
 * <p>Verifies that:</p>
 * <ul>
 *   <li>STAFF can READ payment resources (HTTP 200 / 404 from service layer).</li>
 *   <li>STAFF cannot WRITE payment resources (HTTP 403 — Owner-only mutations).</li>
 *   <li>OWNER + legacy PLATFORM_ADMIN / ADMIN aliases retain full access.</li>
 *   <li>Anonymous calls fail with HTTP 401.</li>
 * </ul>
 *
 * <p>Pattern mirrors {@code BetaAccessControllerTest} — explicit {@code Mockito.reset}
 * in {@link #beforeEach()} guards against mock-state leak (per
 * {@code feedback_webmvctest_mock_reset.md}).</p>
 */
@WebMvcTest(controllers = PaymentController.class)
@Import(SecurityConfig.class)
@DisplayName("PaymentController — RBAC security")
class PaymentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private com.kitehub.subscription.billing.service.ReceiptService receiptService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void beforeEach() {
        Mockito.reset(paymentService);
    }

    private CreatePaymentRequest sampleCreateRequest() {
        return CreatePaymentRequest.builder()
                .subscriptionId(UUID.randomUUID())
                .amountVnd(100_000L)
                .paymentMethod(PaymentMethod.VIETQR)
                .build();
    }

    @Nested
    @DisplayName("POST /api/platform/payments — Owner mutation")
    class CreatePayment {

        @Test
        @WithMockUser(roles = "STAFF")
        @DisplayName("STAFF role → HTTP 403 (privilege escalation blocked)")
        void staffCreatePayment_returns403() throws Exception {
            mockMvc.perform(post("/api/platform/payments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleCreateRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous → HTTP 401")
        void anonymousCreatePayment_returns401() throws Exception {
            mockMvc.perform(post("/api/platform/payments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleCreateRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER role → 2xx (proceeds to service)")
        void ownerCreatePayment_returns201() throws Exception {
            when(paymentService.createPayment(any())).thenReturn(Mockito.mock(PaymentResponse.class));
            mockMvc.perform(post("/api/platform/payments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleCreateRequest())))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = "PLATFORM_ADMIN")
        @DisplayName("Legacy PLATFORM_ADMIN alias → 2xx (Wave 81 cutoff window)")
        void legacyAdminCreatePayment_returns201() throws Exception {
            when(paymentService.createPayment(any())).thenReturn(Mockito.mock(PaymentResponse.class));
            mockMvc.perform(post("/api/platform/payments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sampleCreateRequest())))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("GET /api/platform/payments — Owner+Staff read")
    class ListPayments {

        @Test
        @WithMockUser(roles = "STAFF")
        @DisplayName("STAFF role → HTTP 200 (read allowed)")
        void staffListPayments_returns200() throws Exception {
            when(paymentService.getAllPayments(any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));
            mockMvc.perform(get("/api/platform/payments"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "OWNER")
        @DisplayName("OWNER role → HTTP 200")
        void ownerListPayments_returns200() throws Exception {
            when(paymentService.getAllPayments(any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));
            mockMvc.perform(get("/api/platform/payments"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous → HTTP 401")
        void anonymousListPayments_returns401() throws Exception {
            mockMvc.perform(get("/api/platform/payments"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
