package com.kitehub.admin.controller;

import com.kitehub.subscription.dto.PaymentResponse;
import com.kitehub.subscription.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Smoke tests for {@link AdminPaymentsController} — verifies HTTP 200 + JSON shape
 * for the canonical {@code /api/v1/admin/payments} path (Wave 92 Bucket D — fixes Wave 90
 * walkthrough 404 sub-finding).
 *
 * <p>Pure unit-level test using Mockito stubs — avoids full Spring context overhead per
 * existing {@link AdminControllerPaginationTest} pattern.</p>
 */
class AdminPaymentsControllerTest {

    private PaymentService paymentService;
    private AdminPaymentsController controller;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        controller = new AdminPaymentsController(paymentService);
    }

    @Test
    void listPendingPayments_returnsHttp200AndList() {
        PaymentResponse payment = PaymentResponse.builder().build();
        when(paymentService.getPendingPayments()).thenReturn(List.of(payment));

        ResponseEntity<List<PaymentResponse>> response = controller.listPendingPayments();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(1);
    }

    @Test
    void listPendingPayments_emptyResult_returnsHttp200WithEmptyList() {
        when(paymentService.getPendingPayments()).thenReturn(Collections.emptyList());

        ResponseEntity<List<PaymentResponse>> response = controller.listPendingPayments();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }

    @Test
    void getPaymentsSummary_returnsHttp200WithSummaryShape() {
        when(paymentService.getPendingPayments()).thenReturn(List.of(
                PaymentResponse.builder().build(),
                PaymentResponse.builder().build(),
                PaymentResponse.builder().build()
        ));

        ResponseEntity<Map<String, Object>> response = controller.getPaymentsSummary();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("pendingCount")).isEqualTo(3L);
        assertThat(response.getBody().get("scope")).isEqualTo("pending-only-v1-stub");
        assertThat(response.getBody()).containsKey("note");
    }

    @Test
    void getPaymentsSummary_emptyPending_returnsZeroCount() {
        when(paymentService.getPendingPayments()).thenReturn(Collections.emptyList());

        ResponseEntity<Map<String, Object>> response = controller.getPaymentsSummary();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("pendingCount")).isEqualTo(0L);
    }
}
