package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.enums.PaymentMethod;
import com.kitehub.platform.domain.enums.PaymentStatus;
import com.kitehub.subscription.dto.CreatePaymentRequest;
import com.kitehub.subscription.dto.CursorPage;
import com.kitehub.subscription.dto.PaymentResponse;
import com.kitehub.subscription.repository.PaymentRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for payment processing.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final VietQRService vietQRService;

    /**
     * Phase 1 BETA symbolic-amount override (Wave flow-kh3-2, GAP-975). When
     * enabled, every created payment charges {@link #betaOverrideAmountVnd}
     * instead of the real tier price so beta testers move a token 10.000đ via a
     * real bank transfer. Disabled by default → real amount preserved.
     */
    @Value("${kitehub.payment.beta-mode.enabled:false}")
    private boolean betaModeEnabled;

    @Value("${kitehub.payment.beta-mode.override-amount-vnd:10000}")
    private long betaOverrideAmountVnd;

    /**
     * SePay transfer-memo reference pattern (Wave flow-kh3-2). Matches the
     * {@code KH3SUB<8 uppercase hex>} token embedded by {@link #generateTxnRef}.
     */
    private static final Pattern TXN_REF_PATTERN = Pattern.compile("KH3SUB[A-F0-9]{8}");

    /**
     * Create a new payment.
     *
     * @param request Create payment request
     * @return Created payment response
     * @throws IllegalArgumentException if subscription not found
     */
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        log.info("Creating payment for subscription: {} (amount: {} VND)",
            request.getSubscriptionId(), request.getAmountVnd());

        // Verify subscription exists
        var subscription = subscriptionRepository.findById(request.getSubscriptionId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Subscription not found: " + request.getSubscriptionId()));

        // Phase 1 BETA symbolic-amount override (GAP-975): charge a token amount
        // (default 10.000đ) via a real bank transfer when beta-mode is enabled.
        long effectiveAmountVnd = betaModeEnabled ? betaOverrideAmountVnd : request.getAmountVnd();
        if (betaModeEnabled) {
            log.info("Beta payment override active: charging {} VND instead of {} VND",
                effectiveAmountVnd, request.getAmountVnd());
        }

        // Create payment entity
        Payment payment = new Payment();
        payment.setSubscriptionId(request.getSubscriptionId());
        payment.setInstanceId(subscription.getInstanceId()); // V58 RLS: instance_id NOT NULL
        payment.setAmountVnd(effectiveAmountVnd);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);

        // GAP-1087 / Bug D (KH-3 G2 SePay walk): the QR memo (addInfo) + paymentContent
        // MUST equal payment.txnRef (KH3SUB<8hex>) so the bank-transfer description SePay
        // forwards carries the token processSepayWebhook matches on (findByTxnRef).
        // Previously this path used a "KITECLASS <subId>" QR memo + paymentContent and
        // derived txnRef from the payment id AFTER save (memo != txnRef) → SePay could
        // never confirm. Mirror SubscriptionService.createPendingPayment: generate the
        // standalone txnRef token FIRST, use it as QR memo + paymentContent + txnRef
        // (all three equal), single save.
        String txnRef = generateTxnRef(UUID.randomUUID());
        payment.setTxnRef(txnRef);
        payment.setPaymentContent(txnRef);

        // Generate QR code if payment method is VietQR
        if (request.getPaymentMethod() == PaymentMethod.VIETQR) {
            payment.setQrCodeUrl(vietQRService.generateQRCode(
                UUID.randomUUID(), effectiveAmountVnd, txnRef));
            // GAP-939: snapshot bank account info from VietQRService defaults so Owner
            // sees "Số tài khoản" + "Tên chủ tài khoản" on /billing/payment/{id}.
            // Prior to fix: setBankCode(getBankInfo()) stored multi-line "Bank: VCB\n..."
            // (truncated to "VCB" by column length) + account_number/account_name empty.
            payment.setBankCode(vietQRService.getBankCode());
            payment.setAccountNumber(vietQRService.getAccountNumber());
            payment.setAccountName(vietQRService.getAccountName());
        }

        Payment saved = paymentRepository.save(payment);

        log.info("Created payment: {} (txnRef: {}) for subscription: {}",
            saved.getId(), saved.getTxnRef(), request.getSubscriptionId());
        return PaymentResponse.fromEntity(saved);
    }

    /**
     * Build the SePay matching reference for a payment.
     *
     * @param paymentId generated payment UUID
     * @return reference of the form {@code KH3SUB<8 uppercase hex>}
     */
    static String generateTxnRef(UUID paymentId) {
        return "KH3SUB" + paymentId.toString().substring(0, 8).toUpperCase();
    }

    /**
     * Get payment by ID.
     *
     * @param paymentId Payment UUID
     * @return Payment response
     * @throws IllegalArgumentException if payment not found
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        return PaymentResponse.fromEntity(payment);
    }

    /**
     * Get all payments for subscription.
     *
     * @param subscriptionId Subscription UUID
     * @return List of payment responses
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsBySubscription(UUID subscriptionId) {
        List<Payment> payments = paymentRepository.findBySubscriptionId(subscriptionId);
        return payments.stream()
            .map(PaymentResponse::fromEntity)
            .toList();
    }

    /**
     * Get all payments with optional status filter, paginated.
     *
     * <p><strong>GAP-432 (Wave 41 Bucket C):</strong> the prior implementation
     * called {@code paymentRepository.findAll()} when no status was supplied,
     * which scans the full payments table. Payments grow unbounded with usage,
     * so this was a future performance cliff. Now every call MUST supply a
     * {@link Pageable} (controller defaults to size 50). DB-side soft-delete
     * filter is also pushed into the WHERE clause via the new
     * {@link PaymentRepository#findAllNotDeleted}/
     * {@link PaymentRepository#findByStatusNotDeleted} repository methods.</p>
     *
     * @param status   Payment status filter (optional, may be {@code null})
     * @param pageable Page request (size, page, sort) — required, never null
     * @return Page of payment responses (preserves total counts + paging metadata)
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(PaymentStatus status, Pageable pageable) {
        Page<Payment> payments = (status != null)
            ? paymentRepository.findByStatusNotDeleted(status, pageable)
            : paymentRepository.findAllNotDeleted(pageable);

        return payments.map(PaymentResponse::fromEntity);
    }

    /**
     * Keyset-paginate payments after the given cursor — Wave 85 Bucket D D-AC1.
     *
     * <p>Use this variant when expected dataset exceeds ~1M rows; avoids
     * {@code OFFSET N} cliff cost. Order is fixed {@code id ASC}.</p>
     *
     * @param status   optional status filter
     * @param cursorId UUID of the last row from the prior page (null for first page)
     * @param size     page size (caller-capped to 1..200)
     * @return cursor page with content + nextCursor + hasNext
     */
    @Transactional(readOnly = true)
    public CursorPage<PaymentResponse> getPaymentsByCursor(PaymentStatus status,
                                                            UUID cursorId,
                                                            int size) {
        // Fetch size+1 to detect hasNext without a separate count query.
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Payment> rows = (status != null)
            ? paymentRepository.findByStatusAfterCursor(status, cursorId, pageable)
            : paymentRepository.findAfterCursor(cursorId, pageable);

        boolean hasNext = rows.size() > size;
        List<Payment> trimmed = hasNext ? rows.subList(0, size) : rows;
        List<PaymentResponse> content = trimmed.stream()
            .map(PaymentResponse::fromEntity)
            .toList();

        String nextCursor = (hasNext && !trimmed.isEmpty())
            ? CursorPage.encodeCursor(trimmed.get(trimmed.size() - 1).getId())
            : null;

        return new CursorPage<>(content, size, nextCursor, hasNext);
    }

    /**
     * Process payment webhook notification.
     * Called when payment gateway confirms payment.
     *
     * @param transactionId Bank transaction ID
     * @param amountVnd Payment amount
     * @param paymentContent Payment description
     * @throws IllegalArgumentException if payment not found or verification fails
     */
    @Transactional
    public void processPaymentWebhook(String transactionId, Long amountVnd, String paymentContent) {
        log.info("Processing payment webhook: {} (amount: {}, content: {})",
            transactionId, amountVnd, paymentContent);

        // Find payment by content (contains subscription ID)
        Payment payment = findPaymentByContent(paymentContent);

        // Verify payment amount matches
        if (!payment.getAmountVnd().equals(amountVnd)) {
            log.error("Payment amount mismatch: expected {}, got {}", payment.getAmountVnd(), amountVnd);
            throw new IllegalArgumentException("Payment amount mismatch");
        }

        // Verify with VietQR/Bank API
        boolean verified = vietQRService.verifyPayment(transactionId, amountVnd, paymentContent);
        if (!verified) {
            log.error("Payment verification failed: {}", transactionId);
            payment.fail();
            paymentRepository.save(payment);
            throw new IllegalArgumentException("Payment verification failed");
        }

        // Mark payment as completed
        payment.complete(transactionId);
        paymentRepository.save(payment);

        log.info("Payment completed: {} for subscription: {}", payment.getId(), payment.getSubscriptionId());

        // Apply pending subscription upgrade after payment capture.
        try {
            subscriptionService.applyPendingUpgrade(payment.getSubscriptionId(), payment.getId());
            log.info("Pending upgrade applied for subscription: {}", payment.getSubscriptionId());
        } catch (Exception e) {
            log.error("Failed to apply pending upgrade for subscription: {}", payment.getSubscriptionId(), e);
            // Payment is still completed, but subscription update failed.
            // This should be handled by admin/retry mechanism.
        }
    }

    /**
     * Process a SePay merchant-gateway webhook notification (Wave flow-kh3-2,
     * GAP-976). Locates the exact payment via its {@code txnRef} extracted from
     * the bank transfer description, verifies the amount, completes it, and
     * applies the pending subscription upgrade.
     *
     * <p>Idempotent: a replayed SePay {@code id} (already stamped on a completed
     * payment) returns early without re-processing. A genuine orphan reference
     * (no matching payment) throws {@link IllegalArgumentException} so the caller
     * surfaces HTTP 400. An amount mismatch is logged and skipped (not surfaced)
     * to avoid an endless SePay retry loop.</p>
     *
     * @param sepayId          SePay transaction id (idempotency key)
     * @param transferAmountVnd amount credited by the bank, in VND
     * @param description      bank transfer memo containing the {@code txnRef}
     * @throws IllegalArgumentException if no payment matches the extracted txnRef
     */
    @Transactional
    public void processSepayWebhook(String sepayId, long transferAmountVnd, String description) {
        log.info("Processing SePay webhook: id={}, amount={}, description={}",
            sepayId, transferAmountVnd, description);

        // Idempotency — this SePay transaction already completed a payment.
        if (sepayId != null && paymentRepository.findByTransactionId(sepayId).isPresent()) {
            log.info("SePay webhook replay ignored — transaction {} already processed", sepayId);
            return;
        }

        String txnRef = extractTxnRef(description);
        if (txnRef == null) {
            throw new IllegalArgumentException("No txnRef found in SePay description: " + description);
        }

        // Exact-match lookup — never a LIKE scan (cross-tenant collision guard).
        Payment payment = paymentRepository.findByTxnRef(txnRef)
            .orElseThrow(() -> new IllegalArgumentException("No payment found for txnRef: " + txnRef));

        if (payment.isCompleted()) {
            log.info("Payment {} already completed — SePay webhook idempotent", payment.getId());
            return;
        }

        if (!payment.getAmountVnd().equals(transferAmountVnd)) {
            log.error("SePay amount mismatch for payment {} (txnRef {}): expected {}, got {}",
                payment.getId(), txnRef, payment.getAmountVnd(), transferAmountVnd);
            return; // logic error logged, not surfaced — no double-process
        }

        payment.complete(sepayId);
        paymentRepository.save(payment);
        log.info("Payment {} completed via SePay transaction {} for subscription {}",
            payment.getId(), sepayId, payment.getSubscriptionId());

        try {
            subscriptionService.applyPendingUpgrade(payment.getSubscriptionId(), payment.getId());
            log.info("Pending upgrade applied for subscription: {}", payment.getSubscriptionId());
        } catch (Exception e) {
            log.error("Failed to apply pending upgrade for subscription: {}",
                payment.getSubscriptionId(), e);
            // Payment captured; subscription update retried by admin/job mechanism.
        }
    }

    /**
     * Extract the SePay matching reference from a bank transfer description.
     *
     * @param description bank transfer memo (may be {@code null})
     * @return the {@code KH3SUB<8 hex>} reference, or {@code null} if absent
     */
    static String extractTxnRef(String description) {
        if (description == null) {
            return null;
        }
        Matcher matcher = TXN_REF_PATTERN.matcher(description);
        return matcher.find() ? matcher.group() : null;
    }

    /**
     * Find payment by payment content.
     *
     * @param paymentContent Payment description
     * @return Payment entity
     * @throws IllegalArgumentException if payment not found
     */
    private Payment findPaymentByContent(String paymentContent) {
        // Extract subscription ID from payment content (format: "KITECLASS {subscription_id_short}")
        String[] parts = paymentContent.split(" ");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid payment content format: " + paymentContent);
        }

        String shortId = parts[1];

        // Find pending payments and match by content
        List<Payment> pendingPayments = paymentRepository.findPendingPayments();
        for (Payment payment : pendingPayments) {
            if (payment.getPaymentContent() != null && payment.getPaymentContent().contains(shortId)) {
                return payment;
            }
        }

        throw new IllegalArgumentException("Payment not found for content: " + paymentContent);
    }

    /**
     * Get QR code URL for payment.
     *
     * @param paymentId Payment UUID
     * @return QR code URL
     * @throws IllegalArgumentException if payment not found or has no QR code
     */
    @Transactional(readOnly = true)
    public String getQRCode(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getQrCodeUrl() == null) {
            throw new IllegalArgumentException("Payment has no QR code");
        }

        return payment.getQrCodeUrl();
    }

    // ==================== ADMIN METHODS ====================

    /**
     * Get all pending payments (Admin).
     *
     * @return List of pending payment responses
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPendingPayments() {
        log.info("Admin fetching pending payments");
        List<Payment> payments = paymentRepository.findPendingPayments();
        return payments.stream()
            .map(PaymentResponse::fromEntity)
            .toList();
    }

    /**
     * Confirm payment manually (Admin).
     *
     * @param paymentId Payment UUID
     * @param transactionId Bank transaction ID
     * @return Updated payment response
     * @throws IllegalArgumentException if payment not found or not pending
     */
    @Transactional
    public PaymentResponse confirmPayment(UUID paymentId, String transactionId) {
        log.info("Admin confirming payment: {} with transactionId: {}", paymentId, transactionId);

        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Payment is not pending: " + payment.getStatus());
        }

        // Mark payment as completed
        payment.complete(transactionId);
        payment = paymentRepository.save(payment);

        log.info("Payment confirmed: {} for subscription: {}", payment.getId(), payment.getSubscriptionId());

        // Apply pending subscription upgrade after manual admin confirmation.
        try {
            subscriptionService.applyPendingUpgrade(payment.getSubscriptionId(), payment.getId());
            log.info("Pending upgrade applied for subscription: {}", payment.getSubscriptionId());
        } catch (Exception e) {
            log.error("Failed to apply pending upgrade for subscription: {}", payment.getSubscriptionId(), e);
        }

        return PaymentResponse.fromEntity(payment);
    }

    /**
     * Reject payment manually (Admin).
     *
     * @param paymentId Payment UUID
     * @param reason Rejection reason
     * @return Updated payment response
     * @throws IllegalArgumentException if payment not found or not pending
     */
    @Transactional
    public PaymentResponse rejectPayment(UUID paymentId, String reason) {
        log.info("Admin rejecting payment: {} with reason: {}", paymentId, reason);

        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Payment is not pending: " + payment.getStatus());
        }

        // Mark payment as failed
        payment.fail();
        payment = paymentRepository.save(payment);

        try {
            subscriptionService.clearPendingUpgrade(payment.getSubscriptionId(), payment.getId());
        } catch (Exception e) {
            log.error("Failed to clear pending upgrade for subscription: {}", payment.getSubscriptionId(), e);
        }

        log.info("Payment rejected: {} (reason: {})", payment.getId(), reason);

        return PaymentResponse.fromEntity(payment);
    }
}
