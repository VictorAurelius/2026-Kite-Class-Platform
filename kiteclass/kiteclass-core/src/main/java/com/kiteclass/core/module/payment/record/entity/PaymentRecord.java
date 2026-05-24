package com.kiteclass.core.module.payment.record.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Records a manual payment received by a teacher or admin at the trung tâm.
 *
 * <p>A PaymentRecord is created when a teacher physically collects tuition from a student/parent
 * and marks the associated invoice as paid. This is distinct from online gateway payments
 * (VNPAY, MoMo redirect flows) which are handled by the payment gateway module.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-PAYMENT-METHOD-001: PaymentRecordMethod values are CASH, BANK_TRANSFER, VIETQR, MOMO</li>
 *   <li>BR-PAYMENT-METHOD-002: amount must be positive (> 0) for non-FREE courses</li>
 * </ul>
 *
 * <p>Multi-tenancy: inherits {@code instanceId} (UUID) from {@link BaseEntity} for tenant isolation.
 * Cross-tenant access MUST be blocked at the service layer via {@code @PreAuthorize}.
 *
 * <p>Idempotency: callers supply an {@code Idempotency-Key} header; the PAYMENT scope
 * in the {@code idempotency_keys} table (V66 migration) prevents duplicate recording
 * for the same key within the same tenant.
 *
 * @author KiteClass Team
 * @since V67b
 */
@Entity
@Table(name = "payment_records", indexes = {
        @Index(name = "idx_payment_records_invoice_id", columnList = "invoice_id"),
        @Index(name = "idx_payment_records_instance_id", columnList = "instance_id"),
        @Index(name = "idx_payment_records_paid_at", columnList = "paid_at"),
        @Index(name = "idx_payment_records_method", columnList = "method")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecord extends BaseEntity {

    /**
     * The invoice this payment record is associated with.
     * Foreign key to invoices.id — NOT a JPA relationship to avoid coupling modules.
     * Must reference a valid invoice within the same tenant (instanceId).
     */
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    /**
     * Payment method used by the parent/student.
     * Stored as VARCHAR per EnumType.STRING — future-proof if values added.
     * VN edu market typical order: CASH > BANK_TRANSFER > VIETQR > MOMO.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 30)
    private PaymentRecordMethod method;

    /**
     * Amount received in VND.
     * Precision 19 scale 2 accommodates VND amounts up to 9.99 × 10^16 đ.
     * Must be > 0 for non-FREE invoices (enforced at service layer).
     *
     * <p>Example: 1.500.000đ recorded as 1500000.00
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * Timestamp when the payment was physically received.
     * Defaults to now() if not supplied by caller; stored as Instant (UTC).
     * Used for audit trail and monthly revenue reporting.
     */
    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    /**
     * Optional free-text note from the teacher (e.g., "Phụ huynh em Hồng thanh toán 2 tháng").
     * Max 500 characters. May be null.
     */
    @Column(name = "note", length = 500)
    private String note;

    /**
     * ID of the user (teacher/admin) who recorded this payment.
     * Populated from the authenticated principal at creation time.
     * Used for audit trail.
     */
    @Column(name = "recorded_by", nullable = false)
    private Long recordedBy;
}
