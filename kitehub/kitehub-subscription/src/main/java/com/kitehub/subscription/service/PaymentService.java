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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

        // Create payment entity
        Payment payment = new Payment();
        payment.setSubscriptionId(request.getSubscriptionId());
        payment.setInstanceId(subscription.getInstanceId()); // V58 RLS: instance_id NOT NULL
        payment.setAmountVnd(request.getAmountVnd());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);

        // Generate QR code if payment method is VietQR
        if (request.getPaymentMethod() == PaymentMethod.VIETQR) {
            String qrCodeUrl = vietQRService.generateQRCode(
                UUID.randomUUID(), // Will be replaced after save
                request.getAmountVnd(),
                request.getSubscriptionId()
            );
            payment.setQrCodeUrl(qrCodeUrl);
            payment.setBankCode(vietQRService.getBankInfo());
        }

        // Generate payment content
        String paymentContent = vietQRService.generatePaymentContent(request.getSubscriptionId());
        payment.setPaymentContent(paymentContent);

        Payment saved = paymentRepository.save(payment);

        log.info("Created payment: {} for subscription: {}", saved.getId(), request.getSubscriptionId());
        return PaymentResponse.fromEntity(saved);
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
