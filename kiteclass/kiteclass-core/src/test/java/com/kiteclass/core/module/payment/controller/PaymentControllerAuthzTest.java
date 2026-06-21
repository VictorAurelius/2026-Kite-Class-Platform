package com.kiteclass.core.module.payment.controller;

import com.kiteclass.core.module.payment.service.PaymentService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization tests for {@link PaymentController} (GAP-1491, OWASP A01).
 *
 * <p>{@code PaymentController} originally shipped with NO {@code @PreAuthorize} on any
 * endpoint while {@link com.kiteclass.core.config.SecurityConfig} permits all requests at
 * the URL layer — so a low-privilege role that cleared the gateway could call financial
 * mutations ({@code cancel}, {@code refund}, {@code create}). The fix adds role guards:
 * mutation tier = {@code hasAnyRole('ADMIN','OWNER','PLATFORM_ADMIN','STAFF')},
 * read tier = {@code hasAnyRole('TEACHER',...)}.
 *
 * <p>Mirrors {@code InvoiceControllerAuthzTest}: {@code @WebMvcTest} + {@code @EnableMethodSecurity}
 * so {@code @PreAuthorize} actually fires, with {@link PaymentService} mocked.
 */
@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc
@Import({PaymentControllerAuthzTest.TestSecurityConfig.class, PaymentControllerAuthzTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("PaymentController @PreAuthorize role gate (GAP-1491, OWASP A01)")
class PaymentControllerAuthzTest {

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
        PaymentService paymentService() {
            return Mockito.mock(PaymentService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentService paymentService;

    private static final String TENANT_HEADER = UUID.randomUUID().toString();
    private static final Long PAYMENT_ID = 12L;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(paymentService);
    }

    @Test
    @DisplayName("OWNER → 200 on GET /payments/{id} (read tier, service invoked)")
    @WithMockUser(roles = "OWNER")
    void getById_owner_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{id}", PAYMENT_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(status().isOk());
        verify(paymentService).getPaymentById(PAYMENT_ID);
    }

    @Test
    @DisplayName("OWASP A01: STUDENT → denied PUT /payments/{id}/cancel (service NOT invoked)")
    @WithMockUser(roles = "STUDENT")
    void cancel_student_denied() throws Exception {
        mockMvc.perform(put("/api/v1/payments/{id}/cancel", PAYMENT_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "STUDENT cancel"));
        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("OWASP A01: PARENT → denied POST /payments/{id}/refund (service NOT invoked)")
    @WithMockUser(roles = "PARENT")
    void refund_parent_denied() throws Exception {
        mockMvc.perform(post("/api/v1/payments/{id}/refund", PAYMENT_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "PARENT refund"));
        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("OWASP A01: STUDENT → denied GET /payments/{id} (read tier excludes STUDENT)")
    @WithMockUser(roles = "STUDENT")
    void getById_student_denied() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{id}", PAYMENT_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "STUDENT read"));
        verifyNoInteractions(paymentService);
    }

    private static void assertDenied(int statusCode, String label) {
        if (statusCode >= 200 && statusCode < 300) {
            throw new AssertionError(label + " must be denied by @PreAuthorize, got " + statusCode);
        }
    }
}
