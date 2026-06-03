package com.kiteclass.core.module.payment.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.payment.enums.PaymentMethod;
import com.kiteclass.core.module.payment.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;

/**
 * Payment entity for tracking invoice and installment payments.
 * Supports multiple payment methods including online gateways (VNPay, MoMo, ZaloPay).
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

    @Column(name = "payment_number", nullable = false, unique = true, length = 50)
    private String paymentNumber;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 100)
    private String transactionId;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "installment_id")
    private Long installmentId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "gateway_transaction_id", length = 255)
    private String gatewayTransactionId;

    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;

    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    private String qrCodeUrl;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "receipt_number", length = 50)
    private String receiptNumber;

    @Column(name = "receipt_url", columnDefinition = "TEXT")
    private String receiptUrl;

    @CreatedDate
    @Column(name = "initiated_at", nullable = false, updatable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Transient
    private java.util.UUID createdBy;

    /**
     * Marks payment as completed after gateway callback.
     *
     * @param gatewayTransactionId transaction ID from gateway
     * @param gatewayResponse full JSON response from gateway
     * @throws IllegalStateException if payment is not in PENDING or PROCESSING status
     */
    public void complete(String gatewayTransactionId, String gatewayResponse) {
        if (this.paymentStatus != PaymentStatus.PENDING
            && this.paymentStatus != PaymentStatus.PROCESSING) {
            throw new IllegalStateException(
                "Cannot complete payment with status: " + this.paymentStatus);
        }

        this.paymentStatus = PaymentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.gatewayTransactionId = gatewayTransactionId;
        this.gatewayResponse = gatewayResponse;
        this.receiptNumber = generateReceiptNumber();
    }

    /**
     * Marks payment as failed with reason.
     *
     * @param reason failure reason from gateway or system
     * @throws IllegalStateException if payment is already COMPLETED
     */
    public void fail(String reason) {
        if (this.paymentStatus == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot fail a completed payment");
        }

        this.paymentStatus = PaymentStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.failureReason = reason;
    }

    /**
     * Cancels payment (before completion).
     *
     * @throws IllegalStateException if payment is already COMPLETED
     */
    public void cancel() {
        if (this.paymentStatus == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed payment");
        }

        this.paymentStatus = PaymentStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.failureReason = "Cancelled by user";
    }

    /**
     * Processes refund for completed payment.
     *
     * @throws IllegalStateException if payment is not COMPLETED
     */
    public void refund() {
        if (this.paymentStatus != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Can only refund completed payments");
        }

        this.paymentStatus = PaymentStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
    }

    /**
     * Checks if payment has expired.
     *
     * @return true if current time is after expiry time
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Generates receipt number for completed payment.
     * Pattern: RCP-{year}-{sequence}
     * Example: RCP-2026-000001
     *
     * @return generated receipt number
     */
    private String generateReceiptNumber() {
        // Pattern: RCP-2026-000001
        // Note: ID is available after save, this will be called in complete() method
        return "RCP-" + Year.now().getValue() + "-" + String.format("%06d", this.getId());
    }
}
