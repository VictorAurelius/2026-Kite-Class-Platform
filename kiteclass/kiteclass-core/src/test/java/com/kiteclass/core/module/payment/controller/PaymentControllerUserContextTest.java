package com.kiteclass.core.module.payment.controller;

import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.payment.dto.CreateInstallmentPaymentRequest;
import com.kiteclass.core.module.payment.dto.CreatePaymentRequest;
import com.kiteclass.core.module.payment.dto.PaymentResponse;
import com.kiteclass.core.module.payment.enums.PaymentMethod;
import com.kiteclass.core.module.payment.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Wave 105 Bucket E0 Bug 1 (failure-mode matrix B1/D1) — verify
 * {@link PaymentController} extracts {@code userId} from
 * {@link UserContext} instead of hardcoded {@code 1L}.
 *
 * <p>Per {@code .claude/rules/pre-handoff-self-test-completeness.md} §2.6
 * Payment flow gap class (g) audit-log integrity:
 * two different users → two different {@code user_id} values in
 * payment service call (not always {@code 1L}).
 *
 * @since Wave 105 Bucket E0
 */
@DisplayName("PaymentController UserContext-aware payment creation (Bug 1)")
class PaymentControllerUserContextTest {

    private static final UUID USER_42 = UUID.fromString("00000000-0000-0000-0000-000000000042");
    private static final UUID USER_77 = UUID.fromString("00000000-0000-0000-0000-000000000077");
    private static final UUID USER_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_101 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID USER_202 = UUID.fromString("00000000-0000-0000-0000-000000000202");

    private final PaymentService paymentService = mock(PaymentService.class);
    private final PaymentController controller = new PaymentController(paymentService);

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    @DisplayName("createPayment passes UserContext userId to service (not hardcoded 1L)")
    void createPaymentUsesUserContext() {
        UserContext.setCurrentUser(USER_42);
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setInvoiceId(100L);
        request.setPaymentMethod(PaymentMethod.CASH);
        request.setAmount(BigDecimal.valueOf(1500000L));
        PaymentResponse stub = new PaymentResponse();
        when(paymentService.createPayment(any(), any())).thenReturn(stub);

        controller.createPayment(request);

        ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(paymentService).createPayment(any(CreatePaymentRequest.class), userIdCaptor.capture());
        assertThat(userIdCaptor.getValue()).isEqualTo(USER_42);
        assertThat(userIdCaptor.getValue()).isNotEqualTo(USER_1); // Bug 1 regression guard
    }

    @Test
    @DisplayName("createInstallmentPayment passes UserContext userId to service")
    void createInstallmentPaymentUsesUserContext() {
        UserContext.setCurrentUser(USER_77);
        CreateInstallmentPaymentRequest request = new CreateInstallmentPaymentRequest();
        request.setInstallmentId(200L);
        request.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        request.setAmount(BigDecimal.valueOf(500000L));
        PaymentResponse stub = new PaymentResponse();
        when(paymentService.createInstallmentPayment(any(), any())).thenReturn(stub);

        controller.createInstallmentPayment(request);

        ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(paymentService).createInstallmentPayment(any(CreateInstallmentPaymentRequest.class),
                userIdCaptor.capture());
        assertThat(userIdCaptor.getValue()).isEqualTo(USER_77);
        assertThat(userIdCaptor.getValue()).isNotEqualTo(USER_1);
    }

    @Test
    @DisplayName("createPayment throws AUTH_REQUIRED when UserContext absent")
    void createPaymentRequiresUserContext() {
        // No UserContext.setCurrentUser called → context empty
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setInvoiceId(100L);
        request.setPaymentMethod(PaymentMethod.CASH);
        request.setAmount(BigDecimal.valueOf(1500000L));

        assertThatThrownBy(() -> controller.createPayment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AUTH_REQUIRED");
    }

    @Test
    @DisplayName("Two users → two distinct user_id values (audit trail integrity)")
    void twoUsersGetTwoDistinctUserIds() {
        PaymentResponse stub = new PaymentResponse();
        when(paymentService.createPayment(any(), any())).thenReturn(stub);
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setInvoiceId(100L);
        request.setPaymentMethod(PaymentMethod.CASH);
        request.setAmount(BigDecimal.valueOf(1500000L));

        // User A
        UserContext.setCurrentUser(USER_101);
        controller.createPayment(request);
        UserContext.clear();

        // User B
        UserContext.setCurrentUser(USER_202);
        controller.createPayment(request);

        ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(paymentService, org.mockito.Mockito.times(2))
                .createPayment(any(CreatePaymentRequest.class), userIdCaptor.capture());
        assertThat(userIdCaptor.getAllValues()).containsExactly(USER_101, USER_202);
    }
}
