package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.PaymentMethod;
import com.kitehub.subscription.dto.CreatePaymentRequest;
import com.kitehub.subscription.dto.PaymentResponse;
import com.kitehub.subscription.repository.PaymentRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PaymentService — focused on GAP-939 snapshot regression
 * (Payment.account_number + Payment.account_name must be populated from
 * VietQRService defaults so Owner sees full transfer instructions).
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    private static final String BANK_CODE = "VCB";
    private static final String ACCOUNT_NUMBER = "1234567890";
    private static final String ACCOUNT_NAME = "CONG TY KITECLASS";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private VietQRService vietQRService;

    @InjectMocks
    private PaymentService paymentService;

    private UUID subscriptionId;
    private UUID instanceId;

    @BeforeEach
    void setUp() {
        subscriptionId = UUID.randomUUID();
        instanceId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GAP-939: createPayment with VietQR snapshots bankCode + accountNumber + accountName from VietQRService defaults")
    void createPayment_vietqr_snapshotsBankAccountInfoFromService() {
        // Arrange — VietQR mock returns defaults
        Subscription subscription = new Subscription();
        subscription.setInstanceId(instanceId);
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        when(vietQRService.getBankCode()).thenReturn(BANK_CODE);
        when(vietQRService.getAccountNumber()).thenReturn(ACCOUNT_NUMBER);
        when(vietQRService.getAccountName()).thenReturn(ACCOUNT_NAME);
        when(vietQRService.generateQRCode(any(UUID.class), any(Long.class), any(UUID.class)))
            .thenReturn("https://img.vietqr.io/image/VCB-1234567890-compact.png");
        when(vietQRService.generatePaymentContent(subscriptionId))
            .thenReturn("KITECLASS " + subscriptionId.toString().substring(0, 8).toUpperCase());

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .subscriptionId(subscriptionId)
            .amountVnd(150_000L)
            .paymentMethod(PaymentMethod.VIETQR)
            .build();

        // Act
        PaymentResponse response = paymentService.createPayment(request);

        // Capture Payment entity passed to save() — assert bank info snapshot
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        org.mockito.Mockito.verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();

        assertThat(saved.getBankCode())
            .as("GAP-939: bank_code must be short bank code (e.g. 'VCB'), NOT multi-line getBankInfo() output")
            .isEqualTo(BANK_CODE);
        assertThat(saved.getAccountNumber())
            .as("GAP-939: account_number must be snapshotted from VietQRService.getAccountNumber() default")
            .isEqualTo(ACCOUNT_NUMBER);
        assertThat(saved.getAccountName())
            .as("GAP-939: account_name must be snapshotted from VietQRService.getAccountName() default")
            .isEqualTo(ACCOUNT_NAME);
        assertThat(saved.getQrCodeUrl()).isNotBlank();
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("createPayment with non-VietQR method does not call VietQR snapshots")
    void createPayment_nonVietqr_doesNotSetBankInfo() {
        // Arrange
        Subscription subscription = new Subscription();
        subscription.setInstanceId(instanceId);
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(vietQRService.generatePaymentContent(subscriptionId)).thenReturn("KITECLASS X");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .subscriptionId(subscriptionId)
            .amountVnd(100_000L)
            .paymentMethod(PaymentMethod.BANK_TRANSFER)
            .build();

        // Act
        paymentService.createPayment(request);

        // Assert — VietQR helpers not invoked when method != VIETQR
        org.mockito.Mockito.verify(vietQRService, org.mockito.Mockito.never()).getBankCode();
        org.mockito.Mockito.verify(vietQRService, org.mockito.Mockito.never()).getAccountNumber();
        org.mockito.Mockito.verify(vietQRService, org.mockito.Mockito.never()).getAccountName();
    }
}
