package com.kiteclass.core.module.invoice.repository;

import com.kiteclass.core.module.invoice.entity.InstallmentPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Finds installment plan by ID with {@code installments} collection prefetched
     * in a single round-trip — GAP-134 anti-N+1 path. GAP-128 flagged
     * {@code InstallmentPlanServiceImpl.recordInstallmentPayment} as a documented
     * offender that walks the installments collection; callers from that path should
     * prefer this method.
     *
     * @param id the plan ID
     * @return Optional containing plan with installments prefetched
     * @since 2.8.2 (GAP-134)
     */
    @EntityGraph(attributePaths = {"installments"})
    @Query("SELECT p FROM InstallmentPlan p WHERE p.id = :id AND p.deleted = false")
    Optional<InstallmentPlan> findByIdWithInstallments(@Param("id") Long id);

    /**
     * Finds installment plan by invoice ID (excluding soft-deleted).
     * One plan per invoice constraint.
     *
     * @param invoiceId the invoice ID
     * @return Optional containing plan if found
     */
    Optional<InstallmentPlan> findByInvoiceIdAndDeletedFalse(Long invoiceId);

    /**
     * Finds installment plan by invoice ID with {@code installments} prefetched.
     *
     * <p>GAP-134 counterpart to {@link #findByInvoiceIdAndDeletedFalse(Long)} — use
     * this when the caller is about to iterate the installments schedule (payment
     * reminder scheduler, invoice detail page).
     *
     * @param invoiceId the invoice ID
     * @return Optional containing plan with installments prefetched
     * @since 2.8.2 (GAP-134)
     */
    @EntityGraph(attributePaths = {"installments"})
    @Query("SELECT p FROM InstallmentPlan p WHERE p.invoiceId = :invoiceId AND p.deleted = false")
    Optional<InstallmentPlan> findByInvoiceIdWithInstallments(@Param("invoiceId") Long invoiceId);

    /**
     * Checks if installment plan exists for invoice (excluding soft-deleted).
     *
     * @param invoiceId the invoice ID
     * @return true if plan exists
     */
    boolean existsByInvoiceIdAndDeletedFalse(Long invoiceId);
}
