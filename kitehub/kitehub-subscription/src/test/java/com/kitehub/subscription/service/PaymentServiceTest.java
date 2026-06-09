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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        when(vietQRService.generateQRCode(any(UUID.class), any(Long.class), anyString()))
            .thenReturn("https://img.vietqr.io/image/VCB-1234567890-compact.png");

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID()); // mimic Hibernate id generation on persist
            }
            return p;
        });

        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .subscriptionId(subscriptionId)
            .amountVnd(150_000L)
            .paymentMethod(PaymentMethod.VIETQR)
            .build();

        // Act
        PaymentResponse response = paymentService.createPayment(request);

        // Capture Payment entity passed to save() — assert bank info snapshot.
        // GAP-1087 / Bug D: a single save() now — the KH3SUB txnRef is generated BEFORE
        // the save (was previously derived from the id with a second save).
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        org.mockito.Mockito.verify(paymentRepository, org.mockito.Mockito.times(1)).save(captor.capture());
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
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID()); // mimic Hibernate id generation on persist
            }
            return p;
        });

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

    @Test
    @DisplayName("GAP-975: generateTxnRef derives KH3SUB + first 8 uppercase hex of the payment id")
    void generateTxnRef_matchesApiContractFormat() {
        UUID id = UUID.fromString("1a2b3c4d-0000-0000-0000-000000000000");
        assertThat(PaymentService.generateTxnRef(id)).isEqualTo("KH3SUB1A2B3C4D");
        assertThat(PaymentService.generateTxnRef(UUID.randomUUID()))
            .as("must match api-contract regex KH3SUB[A-F0-9]{8}")
            .matches("KH3SUB[0-9A-F]{8}");
    }

    @Test
    @DisplayName("GAP-975: createPayment stamps a txnRef matching the api-contract format")
    void createPayment_generatesUniqueTxnRef() {
        Subscription subscription = new Subscription();
        subscription.setInstanceId(instanceId);
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            return p;
        });

        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .subscriptionId(subscriptionId)
            .amountVnd(150_000L)
            .paymentMethod(PaymentMethod.BANK_TRANSFER)
            .build();

        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response.getTxnRef()).matches("KH3SUB[0-9A-F]{8}");
    }

    @Test
    @DisplayName("GAP-975: beta-mode enabled overrides amount to the symbolic value")
    void createPayment_appliesBetaModeOverride_whenFlagEnabled() {
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "betaModeEnabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "betaOverrideAmountVnd", 10_000L);

        Subscription subscription = new Subscription();
        subscription.setInstanceId(instanceId);
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            return p;
        });

        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .subscriptionId(subscriptionId)
            .amountVnd(599_000L)
            .paymentMethod(PaymentMethod.BANK_TRANSFER)
            .build();

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        paymentService.createPayment(request);
        org.mockito.Mockito.verify(paymentRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());

        assertThat(captor.getValue().getAmountVnd())
            .as("beta-mode override charges the symbolic amount, not the real tier price")
            .isEqualTo(10_000L);
    }

    @Test
    @DisplayName("GAP-975: beta-mode disabled keeps the real requested amount")
    void createPayment_keepsRealAmount_whenFlagDisabled() {
        // betaModeEnabled left at its default (false)
        Subscription subscription = new Subscription();
        subscription.setInstanceId(instanceId);
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            return p;
        });

        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .subscriptionId(subscriptionId)
            .amountVnd(599_000L)
            .paymentMethod(PaymentMethod.BANK_TRANSFER)
            .build();

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        paymentService.createPayment(request);
        org.mockito.Mockito.verify(paymentRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());

        assertThat(captor.getValue().getAmountVnd()).isEqualTo(599_000L);
    }

    // ==================== Bucket B — SePay webhook (GAP-976) ====================

    @Test
    @DisplayName("GAP-976: extractTxnRef finds the KH3SUB token embedded in a bank memo")
    void extractTxnRef_findsEmbeddedReference() {
        assertThat(PaymentService.extractTxnRef("KH3SUB1A2B3C4D")).isEqualTo("KH3SUB1A2B3C4D");
        assertThat(PaymentService.extractTxnRef("CT DEN KH3SUB1A2B3C4D GD 123")).isEqualTo("KH3SUB1A2B3C4D");
        assertThat(PaymentService.extractTxnRef("no reference here")).isNull();
        assertThat(PaymentService.extractTxnRef(null)).isNull();
    }

    @Test
    @DisplayName("GAP-976: replayed SePay transaction id is idempotent — no re-processing")
    void processSepayWebhook_idempotentOnReplay() {
        Payment completed = new Payment();
        completed.setId(UUID.randomUUID());
        when(paymentRepository.findByTransactionId("SEPAY-1")).thenReturn(Optional.of(completed));

        paymentService.processSepayWebhook("SEPAY-1", 10_000L, "KH3SUB1A2B3C4D");

        org.mockito.Mockito.verify(paymentRepository, org.mockito.Mockito.never()).save(any(Payment.class));
        org.mockito.Mockito.verify(subscriptionService, org.mockito.Mockito.never())
            .applyPendingUpgrade(any(UUID.class), any(UUID.class));
    }

    @Test
    @DisplayName("GAP-976: matching txnRef + amount completes payment and applies upgrade")
    void processSepayWebhook_completesPaymentOnMatch() {
        when(paymentRepository.findByTransactionId("SEPAY-2")).thenReturn(Optional.empty());

        Payment pending = new Payment();
        pending.setId(UUID.randomUUID());
        pending.setSubscriptionId(subscriptionId);
        pending.setAmountVnd(10_000L);
        pending.setTxnRef("KH3SUB1A2B3C4D");
        when(paymentRepository.findByTxnRef("KH3SUB1A2B3C4D")).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.processSepayWebhook("SEPAY-2", 10_000L, "KH3SUB1A2B3C4D");

        assertThat(pending.isCompleted()).isTrue();
        assertThat(pending.getTransactionId()).isEqualTo("SEPAY-2");
        org.mockito.Mockito.verify(subscriptionService).applyPendingUpgrade(subscriptionId, pending.getId());
    }

    @Test
    @DisplayName("GAP-976: orphan txnRef (no matching payment) throws IllegalArgumentException")
    void processSepayWebhook_orphanThrows() {
        when(paymentRepository.findByTransactionId("SEPAY-3")).thenReturn(Optional.empty());
        when(paymentRepository.findByTxnRef("KH3SUB1A2B3C4D")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            paymentService.processSepayWebhook("SEPAY-3", 10_000L, "KH3SUB1A2B3C4D"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("GAP-976: amount mismatch is logged and skipped — payment not completed")
    void processSepayWebhook_amountMismatchSkips() {
        when(paymentRepository.findByTransactionId("SEPAY-4")).thenReturn(Optional.empty());

        Payment pending = new Payment();
        pending.setId(UUID.randomUUID());
        pending.setSubscriptionId(subscriptionId);
        pending.setAmountVnd(10_000L);
        pending.setTxnRef("KH3SUB1A2B3C4D");
        when(paymentRepository.findByTxnRef("KH3SUB1A2B3C4D")).thenReturn(Optional.of(pending));

        paymentService.processSepayWebhook("SEPAY-4", 9_999_999L, "KH3SUB1A2B3C4D");

        assertThat(pending.isCompleted()).isFalse();
        org.mockito.Mockito.verify(paymentRepository, org.mockito.Mockito.never()).save(any(Payment.class));
        org.mockito.Mockito.verify(subscriptionService, org.mockito.Mockito.never())
            .applyPendingUpgrade(any(UUID.class), any(UUID.class));
    }
}
