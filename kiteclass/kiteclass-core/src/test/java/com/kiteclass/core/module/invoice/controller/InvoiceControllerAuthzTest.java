package com.kiteclass.core.module.invoice.controller;

import com.kiteclass.core.common.constant.InvoiceStatus;
import com.kiteclass.core.module.invoice.dto.InvoiceResponse;
import com.kiteclass.core.module.invoice.service.InvoiceBatchService;
import com.kiteclass.core.module.invoice.service.InvoiceService;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CI-bound web-slice authorization tests for {@link InvoiceController} (GAP-1005).
 *
 * <p>{@code InvoiceController} originally shipped with NO {@code @PreAuthorize} on
 * any endpoint (OWASP A01 broken access control — KC-7 invoice→payment G1 walk):
 * a low-privilege role that cleared the gateway could call the financial-mutation
 * endpoints ({@code mark-paid}, {@code cancel}, {@code adjustments}) inside its
 * tenant. The code fix added {@code @PreAuthorize} to all 13 mappings — read tier =
 * {@code hasAnyRole('ADMIN','OWNER','PRINCIPAL','PLATFORM_ADMIN','STAFF')} (GAP-1527:
 * TEACHER dropped — tenant-wide financial reads are not a teacher concern, intra-tenant IDOR),
 * financial-mutation tier = {@code hasAnyRole('ADMIN','OWNER','PLATFORM_ADMIN','STAFF')}.
 *
 * <p>This {@code *Test} is the residual GAP-1005 CI regression guard for that role
 * gate (the code fix had compile-only verification before). It mirrors the proven
 * {@code ReportControllerAuthzTest} web-slice config: {@code @WebMvcTest} +
 * {@code @EnableMethodSecurity} so {@code @PreAuthorize} actually fires, with the
 * {@link InvoiceService} / {@link InvoiceBatchService} mocked. Asserts:
 *
 * <ul>
 *   <li>OWNER → 200 on read ({@code GET /{id}}) + financial-mutation
 *       ({@code POST /{id}/mark-paid}, {@code PUT /{id}/cancel});</li>
 *   <li>STUDENT / PARENT (low-privilege, not in any invoice allowlist) → denied
 *       (non-2xx) on the financial-mutation endpoints, service never invoked;</li>
 *   <li>STUDENT → denied on the read endpoint (read tier excludes STUDENT/PARENT).</li>
 * </ul>
 *
 * <p>Per-row tenant data isolation (a tenant's OWNER only sees its own invoices) is
 * enforced at the persistence layer by the Hibernate {@code tenantFilter} and is
 * exercised by the tenant-filter integration tests, not by this controller slice
 * (service is mocked here).
 */
@WebMvcTest(InvoiceController.class)
@AutoConfigureMockMvc
@Import({InvoiceControllerAuthzTest.TestSecurityConfig.class, InvoiceControllerAuthzTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("InvoiceController @PreAuthorize role gate (GAP-1005, OWASP A01)")
class InvoiceControllerAuthzTest {

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
        InvoiceService invoiceService() {
            return Mockito.mock(InvoiceService.class);
        }

        @Bean
        @Primary
        InvoiceBatchService invoiceBatchService() {
            return Mockito.mock(InvoiceBatchService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceBatchService invoiceBatchService;

    /** Gateway-forwarded tenant header carried on every legitimate request. */
    private static final String TENANT_HEADER = UUID.randomUUID().toString();

    private static final Long INVOICE_ID = 28L;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(invoiceService, invoiceBatchService);
    }

    private InvoiceResponse sampleInvoice(InvoiceStatus status) {
        return InvoiceResponse.builder()
                .id(INVOICE_ID)
                .invoiceNumber("INV-2026-0028")
                .studentId(7L)
                .status(status)
                .total(new BigDecimal("1500000"))
                .amountPaid(new BigDecimal("1500000"))
                .balanceDue(BigDecimal.ZERO)
                .build();
    }

    // ----- OWNER happy paths (read + financial-mutation) -----

    @Test
    @DisplayName("OWNER → 200 on GET /invoices/{id} (read tier)")
    @WithMockUser(roles = "OWNER")
    void getById_owner_returns200() throws Exception {
        when(invoiceService.getInvoiceById(INVOICE_ID)).thenReturn(sampleInvoice(InvoiceStatus.PAID));

        mockMvc.perform(get("/api/v1/invoices/{id}", INVOICE_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(INVOICE_ID));

        verify(invoiceService).getInvoiceById(INVOICE_ID);
    }

    @Test
    @DisplayName("OWNER → 200 on POST /invoices/{id}/mark-paid (financial-mutation tier)")
    @WithMockUser(roles = "OWNER")
    void markPaid_owner_returns200() throws Exception {
        when(invoiceService.markInvoiceAsPaid(INVOICE_ID)).thenReturn(sampleInvoice(InvoiceStatus.PAID));

        mockMvc.perform(post("/api/v1/invoices/{id}/mark-paid", INVOICE_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        verify(invoiceService).markInvoiceAsPaid(INVOICE_ID);
    }

    @Test
    @DisplayName("OWNER → 200 on PUT /invoices/{id}/cancel (financial-mutation tier)")
    @WithMockUser(roles = "OWNER")
    void cancel_owner_returns200() throws Exception {
        when(invoiceService.cancelInvoice(INVOICE_ID)).thenReturn(sampleInvoice(InvoiceStatus.CANCELLED));

        mockMvc.perform(put("/api/v1/invoices/{id}/cancel", INVOICE_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        verify(invoiceService).cancelInvoice(INVOICE_ID);
    }

    // ----- Low-privilege deny on financial-mutation (OWASP A01) -----

    @Test
    @DisplayName("OWASP A01: STUDENT → denied POST /invoices/{id}/mark-paid (non-2xx, service NOT invoked)")
    @WithMockUser(roles = "STUDENT")
    void markPaid_student_denied() throws Exception {
        mockMvc.perform(post("/api/v1/invoices/{id}/mark-paid", INVOICE_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "STUDENT mark-paid"));

        verifyNoInteractions(invoiceService);
    }

    @Test
    @DisplayName("OWASP A01: PARENT → denied POST /invoices/{id}/mark-paid (non-2xx, service NOT invoked)")
    @WithMockUser(roles = "PARENT")
    void markPaid_parent_denied() throws Exception {
        mockMvc.perform(post("/api/v1/invoices/{id}/mark-paid", INVOICE_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "PARENT mark-paid"));

        verifyNoInteractions(invoiceService);
    }

    @Test
    @DisplayName("OWASP A01: STUDENT → denied PUT /invoices/{id}/cancel (non-2xx, service NOT invoked)")
    @WithMockUser(roles = "STUDENT")
    void cancel_student_denied() throws Exception {
        mockMvc.perform(put("/api/v1/invoices/{id}/cancel", INVOICE_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "STUDENT cancel"));

        verifyNoInteractions(invoiceService);
    }

    @Test
    @DisplayName("OWASP A01: STUDENT → denied GET /invoices/{id} (read tier excludes STUDENT/PARENT)")
    @WithMockUser(roles = "STUDENT")
    void getById_student_denied() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/{id}", INVOICE_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "STUDENT read"));

        verifyNoInteractions(invoiceService);
    }

    @Test
    @DisplayName("GAP-1527 OWASP A01: TEACHER → denied GET /invoices/{id} (TEACHER dropped from financial reads — intra-tenant IDOR)")
    @WithMockUser(roles = "TEACHER")
    void getById_teacher_denied() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/{id}", INVOICE_ID).header("X-Tenant-Id", TENANT_HEADER))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "TEACHER read"));

        verifyNoInteractions(invoiceService);
    }

    private static void assertDenied(int statusCode, String label) {
        if (statusCode >= 200 && statusCode < 300) {
            throw new AssertionError(label + " must be denied by @PreAuthorize, got " + statusCode);
        }
    }
}
