package com.kiteclass.core.module.invoice.entity;

import com.kiteclass.core.common.constant.InstallmentPlanStatus;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Installment plan entity for invoice payment plans.
 *
 * <p>Allows students to split invoice payment into multiple installments (2-12).
 * Requires admin approval before activation.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>2-12 installments allowed per plan</li>
 *   <li>One plan per invoice</li>
 *   <li>Must be approved by admin before activation</li>
 *   <li>Each installment has due date (typically 30 days apart)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Entity
@Table(
        name = "installment_plans",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_plans_invoice",
                        columnNames = {"invoice_id"}
                )
        },
        indexes = {
                @Index(name = "idx_plans_invoice", columnList = "invoice_id"),
                @Index(name = "idx_plans_instance", columnList = "instance_id"),
                @Index(name = "idx_plans_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPlan extends BaseEntity {

    /**
     * Foreign key to invoice.
     * Required, one plan per invoice.
     */
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    /**
     * Number of installments (2-12).
     * Determines payment schedule frequency.
     */
    @Column(name = "number_of_installments", nullable = false)
    private Integer numberOfInstallments;

    /**
     * Plan status (PENDING, APPROVED, ACTIVE, etc.).
     * Defaults to PENDING, requires approval.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    @Builder.Default
    private InstallmentPlanStatus status = InstallmentPlanStatus.PENDING;

    /**
     * Timestamp when plan was requested.
     */
    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();

    /**
     * Timestamp when plan was approved.
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * User ID who approved the plan.
     */
    @Column(name = "approved_by")
    private Long approvedBy;

    /**
     * Timestamp when plan was rejected.
     */
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    /**
     * Reason for rejection.
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /**
     * Individual installments.
     * Cascade ALL - installments are part of plan lifecycle.
     */
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Installment> installments = new ArrayList<>();

    /**
     * Adds an installment to this plan.
     * Sets bidirectional relationship.
     *
     * @param installment the installment to add
     */
    public void addInstallment(Installment installment) {
        installments.add(installment);
        installment.setPlan(this);
    }

    /**
     * Checks if plan can be approved.
     *
     * @return true if status is PENDING
     */
    public boolean canApprove() {
        return status.canApprove();
    }

    /**
     * Checks if plan can be rejected.
     *
     * @return true if status is PENDING
     */
    public boolean canReject() {
        return status.canReject();
    }

    /**
     * Approves the plan.
     * Updates status and sets approval timestamp.
     *
     * @param approvedByUserId the user ID who approved
     */
    public void approve(Long approvedByUserId) {
        if (!canApprove()) {
            throw new IllegalStateException("Cannot approve plan with status: " + status);
        }
        this.status = InstallmentPlanStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.approvedBy = approvedByUserId;
    }

    /**
     * Rejects the plan.
     * Updates status and sets rejection details.
     *
     * @param reason the reason for rejection
     */
    public void reject(String reason) {
        if (!canReject()) {
            throw new IllegalStateException("Cannot reject plan with status: " + status);
        }
        this.status = InstallmentPlanStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    /**
     * Activates the plan (after approval).
     * Transitions from APPROVED to ACTIVE.
     */
    public void activate() {
        if (this.status != InstallmentPlanStatus.APPROVED) {
            throw new IllegalStateException("Can only activate APPROVED plan");
        }
        this.status = InstallmentPlanStatus.ACTIVE;
    }
}
