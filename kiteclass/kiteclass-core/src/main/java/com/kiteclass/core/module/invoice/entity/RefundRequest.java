package com.kiteclass.core.module.invoice.entity;

import com.kiteclass.core.common.constant.RefundStatus;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Refund request entity for invoice refunds.
 *
 * <p>Manages refund workflow with approval:
 * <ul>
 *   <li>Request creation by user</li>
 *   <li>Admin approval/rejection</li>
 *   <li>Refund processing</li>
 *   <li>Invoice adjustment creation</li>
 * </ul>
 *
 * <p>Business Rules:
 * <ul>
 *   <li>Refund amount must not exceed amount paid</li>
 *   <li>Requires admin approval</li>
 *   <li>Creates InvoiceAdjustment when processed</li>
 *   <li>Updates invoice status to REFUNDED</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Entity
@Table(
        name = "refund_requests",
        indexes = {
                @Index(name = "idx_refunds_invoice", columnList = "invoice_id"),
                @Index(name = "idx_refunds_instance", columnList = "instance_id"),
                @Index(name = "idx_refunds_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest extends BaseEntity {

    /**
     * Foreign key to invoice.
     * Required, must reference an existing invoice.
     */
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    /**
     * Refund amount requested.
     * Required, must be positive and not exceed amount paid.
     */
    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    /**
     * Reason for refund request.
     * Required, for audit trail.
     */
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    /**
     * Refund status (PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED).
     * Defaults to PENDING.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    @Builder.Default
    private RefundStatus status = RefundStatus.PENDING;

    /**
     * User ID who requested the refund.
     */
    @Column(name = "requested_by")
    private Long requestedBy;

    /**
     * Timestamp when refund was requested.
     */
    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();

    /**
     * User ID who approved the refund.
     */
    @Column(name = "approved_by")
    private Long approvedBy;

    /**
     * Timestamp when refund was approved.
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * User ID who rejected the refund.
     */
    @Column(name = "rejected_by")
    private Long rejectedBy;

    /**
     * Timestamp when refund was rejected.
     */
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    /**
     * Reason for rejection.
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /**
     * Timestamp when refund was processed.
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Checks if refund can be approved.
     *
     * @return true if status is PENDING
     */
    public boolean canApprove() {
        return status.canApprove();
    }

    /**
     * Checks if refund can be rejected.
     *
     * @return true if status is PENDING
     */
    public boolean canReject() {
        return status.canReject();
    }

    /**
     * Checks if refund can be processed.
     *
     * @return true if status is APPROVED
     */
    public boolean canProcess() {
        return status.canProcess();
    }

    /**
     * Approves the refund request.
     * Updates status and sets approval details.
     *
     * @param approvedByUserId the user ID who approved
     */
    public void approve(Long approvedByUserId) {
        if (!canApprove()) {
            throw new IllegalStateException("Cannot approve refund with status: " + status);
        }
        this.status = RefundStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.approvedBy = approvedByUserId;
    }

    /**
     * Rejects the refund request.
     * Updates status and sets rejection details.
     *
     * @param rejectedByUserId the user ID who rejected
     * @param reason the reason for rejection
     */
    public void reject(Long rejectedByUserId, String reason) {
        if (!canReject()) {
            throw new IllegalStateException("Cannot reject refund with status: " + status);
        }
        this.status = RefundStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
        this.rejectedBy = rejectedByUserId;
        this.rejectionReason = reason;
    }

    /**
     * Marks refund as processed (completed).
     * Updates status and sets processing timestamp.
     */
    public void markAsProcessed() {
        if (!canProcess()) {
            throw new IllegalStateException("Cannot process refund with status: " + status);
        }
        this.status = RefundStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }
}
