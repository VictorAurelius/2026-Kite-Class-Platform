package com.kitehub.platform.domain.entity;

import com.kitehub.platform.domain.enums.PaymentMethod;
import com.kitehub.platform.domain.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment entity for subscription billing.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    /**
     * Owning tenant/instance. Added NOT NULL by migration V58 (RLS sweep) for
     * row-level tenant isolation; every Payment MUST set this from its
     * subscription's instanceId before persist, else the V58 NOT NULL constraint
     * rejects the insert (SQLState 23502).
     */
    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "amount_vnd", nullable = false)
    private Long amountVnd;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_name", length = 200)
    private String accountName;

    @Column(name = "payment_content", length = 500)
    private String paymentContent;

    /**
     * SePay matching reference (Wave flow-kh3-2, GAP-975). Format
     * {@code KH3SUB<8 hex>} derived from the payment id, embedded in the VietQR
     * transfer memo so the SePay webhook can locate the exact payment via an
     * exact-match lookup (no substring scan → cross-tenant collision guard).
     * Nullable for legacy rows + non-VietQR methods; UNIQUE among non-null values.
     */
    @Column(name = "txn_ref", length = 32, unique = true)
    private String txnRef;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    /**
     * Optimistic-lock version (GAP-895). Guards concurrent payment status transitions
     * (PENDING -> COMPLETED vs admin REFUNDED). Field-level @Version (not BaseEntity) so the
     * column set matches V59 exactly.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Mark payment as completed.
     *
     * @param transactionId Bank transaction ID
     */
    public void complete(String transactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.transactionId = transactionId;
        this.paidAt = LocalDateTime.now();
    }

    /**
     * Mark payment as failed.
     */
    public void fail() {
        this.status = PaymentStatus.FAILED;
    }

    /**
     * Refund payment.
     *
     * @param reason Refund reason
     */
    public void refund(String reason) {
        this.status = PaymentStatus.REFUNDED;
        this.refundReason = reason;
        this.refundedAt = LocalDateTime.now();
    }

    /**
     * Cancel payment.
     */
    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
    }

    /**
     * Check if payment is completed.
     *
     * @return true if payment is completed
     */
    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }

    /**
     * Check if payment is pending.
     *
     * @return true if payment is pending
     */
    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }
}
