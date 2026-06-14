package com.kitehub.admin.controller;

import com.kitehub.admin.dto.PaymentsSummaryResponse;
import com.kitehub.subscription.dto.PaymentResponse;
import com.kitehub.subscription.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        // GAP-1360: controller now pages through pending payments (bounded) but keeps the
        // List array shape for the FE caller.
        PaymentResponse payment = PaymentResponse.builder().build();
        Page<PaymentResponse> page = new PageImpl<>(List.of(payment));
        when(paymentService.getPendingPayments(any(Pageable.class))).thenReturn(page);

        ResponseEntity<List<PaymentResponse>> response = controller.listPendingPayments();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(1);
    }

    @Test
    void listPendingPayments_emptyResult_returnsHttp200WithEmptyList() {
        when(paymentService.getPendingPayments(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        ResponseEntity<List<PaymentResponse>> response = controller.listPendingPayments();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }

    @Test
    void getPaymentsSummary_returnsHttp200WithTypedShape() {
        when(paymentService.getPendingPayments()).thenReturn(List.of(
                PaymentResponse.builder().amountVnd(100_000L).currency("VND").build(),
                PaymentResponse.builder().amountVnd(250_000L).currency("VND").build(),
                PaymentResponse.builder().amountVnd(50_000L).currency("VND").build()
        ));

        ResponseEntity<PaymentsSummaryResponse> response = controller.getPaymentsSummary();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PaymentsSummaryResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.pendingCount()).isEqualTo(3L);
        assertThat(body.totalCount()).isEqualTo(3L);
        assertThat(body.totalAmountVnd()).isEqualTo(400_000L);
        assertThat(body.currency()).isEqualTo("VND");
        assertThat(body.completedCount()).isZero();
    }

    @Test
    void getPaymentsSummary_emptyPending_returnsZeroCount() {
        when(paymentService.getPendingPayments()).thenReturn(Collections.emptyList());

        ResponseEntity<PaymentsSummaryResponse> response = controller.getPaymentsSummary();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().pendingCount()).isZero();
        assertThat(response.getBody().totalAmountVnd()).isZero();
    }

    @Test
    void getPaymentsSummary_nullAmount_treatedAsZero() {
        when(paymentService.getPendingPayments()).thenReturn(List.of(
                PaymentResponse.builder().amountVnd(null).build(),
                PaymentResponse.builder().amountVnd(70_000L).build()
        ));

        ResponseEntity<PaymentsSummaryResponse> response = controller.getPaymentsSummary();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().pendingCount()).isEqualTo(2L);
        assertThat(response.getBody().totalAmountVnd()).isEqualTo(70_000L);
    }
}
