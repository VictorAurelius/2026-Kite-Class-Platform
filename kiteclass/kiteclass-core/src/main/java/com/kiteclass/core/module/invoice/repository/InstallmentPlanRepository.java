package com.kiteclass.core.module.invoice.repository;

import com.kiteclass.core.module.invoice.entity.InstallmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link InstallmentPlan} entity.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Repository
public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, Long> {

    /**
     * Finds installment plan by ID (excluding soft-deleted).
     *
     * @param id the plan ID
     * @return Optional containing plan if found
     */
    Optional<InstallmentPlan> findByIdAndDeletedFalse(Long id);

    /**
     * Finds installment plan by invoice ID (excluding soft-deleted).
     * One plan per invoice constraint.
     *
     * @param invoiceId the invoice ID
     * @return Optional containing plan if found
     */
    Optional<InstallmentPlan> findByInvoiceIdAndDeletedFalse(Long invoiceId);

    /**
     * Checks if installment plan exists for invoice (excluding soft-deleted).
     *
     * @param invoiceId the invoice ID
     * @return true if plan exists
     */
    boolean existsByInvoiceIdAndDeletedFalse(Long invoiceId);
}
