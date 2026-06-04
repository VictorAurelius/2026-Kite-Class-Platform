package com.kitehub.subscription.controller.admin;

import com.kitehub.platform.domain.enums.PaymentMethod;
import com.kitehub.platform.domain.enums.PaymentStatus;
import com.kitehub.subscription.dto.AdminConfirmPaymentRequest;
import com.kitehub.subscription.dto.AdminRejectPaymentRequest;
import com.kitehub.subscription.dto.PaymentResponse;
import com.kitehub.subscription.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminPaymentController} (Wave flow-kh3, UC-SUB-07).
 *
 * <p>Admin authentication is enforced by Spring Security {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")}
 * on each handler (GAP-938, Wave flow-kh3). Authorization is verified at the Spring Security filter
 * chain level — these pure Mockito tests instantiate the controller directly and therefore bypass
 * the security chain, focusing instead on the controller-level contract (payload mapping, service
 * calls, exception propagation through {@code GlobalExceptionHandler}).</p>
 *
 * <p>Authorization round-trip (gateway → X-User-Roles → ROLE_PLATFORM_ADMIN → @PreAuthorize)
 * is covered by integration / MockMvc tests in the broader suite.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminPaymentController")
class AdminPaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    private AdminPaymentController controller;

    private UUID paymentId;

    @BeforeEach
    void setUp() {
        controller = new AdminPaymentController(paymentService);
        paymentId = UUID.randomUUID();
    }

    private PaymentResponse pending() {
        return PaymentResponse.builder()
            .id(paymentId)
            .subscriptionId(UUID.randomUUID())
            .amountVnd(1_500_000L)
            .paymentMethod(PaymentMethod.VIETQR)
            .status(PaymentStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("GET /pending returns the service-supplied list with 200")
    void listPendingHappy() {
        List<PaymentResponse> stub = List.of(pending(), pending(), pending());
        when(paymentService.getPendingPayments()).thenReturn(stub);

        ResponseEntity<List<PaymentResponse>> resp = controller.listPending();

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).hasSize(3);
        assertThat(resp.getBody()).allSatisfy(p ->
            assertThat(p.getStatus()).isEqualTo(PaymentStatus.PENDING));
        verify(paymentService).getPendingPayments();
    }

    @Test
    @DisplayName("POST /{id}/confirm returns 200 with COMPLETED + delegates to service")
    void confirmHappy() {
        PaymentResponse completed = pending();
        completed.setStatus(PaymentStatus.COMPLETED);
        completed.setTransactionId("VCB-20260604-000123");
        when(paymentService.confirmPayment(eq(paymentId), eq("VCB-20260604-000123")))
            .thenReturn(completed);

        AdminConfirmPaymentRequest req = AdminConfirmPaymentRequest.builder()
            .transactionId("VCB-20260604-000123")
            .build();
        ResponseEntity<PaymentResponse> resp = controller.confirm(paymentId, req);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(resp.getBody().getTransactionId()).isEqualTo("VCB-20260604-000123");
        verify(paymentService).confirmPayment(paymentId, "VCB-20260604-000123");
    }

    @Test
    @DisplayName("POST /{id}/confirm propagates IllegalArgumentException (mapped to 400/404/409 by GlobalExceptionHandler)")
    void confirmServiceThrowsPropagates() {
        when(paymentService.confirmPayment(eq(paymentId), eq("X"))).thenThrow(
            new IllegalArgumentException("Payment is not pending: COMPLETED"));

        AdminConfirmPaymentRequest req = AdminConfirmPaymentRequest.builder()
            .transactionId("X")
            .build();
        assertThatThrownBy(() -> controller.confirm(paymentId, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not pending");
    }

    @Test
    @DisplayName("POST /{id}/reject returns 200 with FAILED + delegates to service")
    void rejectHappy() {
        PaymentResponse failed = pending();
        failed.setStatus(PaymentStatus.FAILED);
        when(paymentService.rejectPayment(eq(paymentId), eq("Wrong amount")))
            .thenReturn(failed);

        AdminRejectPaymentRequest req = AdminRejectPaymentRequest.builder()
            .reason("Wrong amount")
            .build();
        ResponseEntity<PaymentResponse> resp = controller.reject(paymentId, req);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentService).rejectPayment(paymentId, "Wrong amount");
    }

    @Test
    @DisplayName("POST /{id}/reject propagates IllegalArgumentException (mapped via GlobalExceptionHandler)")
    void rejectServiceThrowsPropagates() {
        doThrow(new IllegalArgumentException("Payment not found: " + paymentId))
            .when(paymentService).rejectPayment(eq(paymentId), eq("nope"));

        AdminRejectPaymentRequest req = AdminRejectPaymentRequest.builder()
            .reason("nope")
            .build();
        assertThatThrownBy(() -> controller.reject(paymentId, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }
}
