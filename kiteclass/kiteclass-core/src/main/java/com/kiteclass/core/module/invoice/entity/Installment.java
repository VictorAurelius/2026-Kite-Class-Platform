package com.kiteclass.core.module.invoice.entity;

import com.kiteclass.core.common.constant.InstallmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Individual installment within an installment plan.
 *
 * <p>Represents one payment in a multi-installment plan:
 * <ul>
 *   <li>Sequential installment number (1, 2, 3, ...)</li>
 *   <li>Amount to pay</li>
 *   <li>Due date</li>
 *   <li>Payment tracking</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Entity
@Table(
        name = "installments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_installments_plan_number",
                        columnNames = {"plan_id", "installment_number"}
                )
        },
        indexes = {
                @Index(name = "idx_installments_plan", columnList = "plan_id"),
                @Index(name = "idx_installments_status", columnList = "status"),
                @Index(name = "idx_installments_due_date", columnList = "due_date")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent installment plan.
     * Required, installment cannot exist without plan.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private InstallmentPlan plan;

    /**
     * Installment sequence number (1, 2, 3, ...).
     * Required, unique within plan.
     */
    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    /**
     * Amount for this installment.
     * Required, must be positive.
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Payment due date.
     * Required, determines overdue status.
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /**
     * Amount paid so far.
     * For partial payments, can be less than amount.
     */
    @Column(name = "paid_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /**
     * Installment status (PENDING, PAID, OVERDUE, CANCELLED).
     * Defaults to PENDING.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    @Builder.Default
    private InstallmentStatus status = InstallmentStatus.PENDING;

    /**
     * Timestamp when installment was fully paid.
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * Timestamp when installment was created.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Checks if installment is overdue.
     *
     * @return true if past due date and not paid
     */
    public boolean isOverdue() {
        return LocalDate.now().isAfter(dueDate) &&
               status != InstallmentStatus.PAID &&
               status != InstallmentStatus.CANCELLED;
    }

    /**
     * Checks if installment is fully paid.
     *
     * @return true if paid amount >= total amount
     */
    public boolean isFullyPaid() {
        return paidAmount.compareTo(amount) >= 0;
    }

    /**
     * Records a payment for this installment.
     * Updates paid amount and status.
     *
     * @param paymentAmount the amount paid
     */
    public void recordPayment(BigDecimal paymentAmount) {
        if (!status.canPay()) {
            throw new IllegalStateException("Cannot pay installment with status: " + status);
        }

        this.paidAmount = this.paidAmount.add(paymentAmount);

        if (isFullyPaid()) {
            this.status = InstallmentStatus.PAID;
            this.paidAt = LocalDateTime.now();
        } else if (isOverdue()) {
            this.status = InstallmentStatus.OVERDUE;
        }
    }
}
