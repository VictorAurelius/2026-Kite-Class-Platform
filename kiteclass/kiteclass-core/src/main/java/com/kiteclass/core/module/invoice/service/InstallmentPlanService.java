package com.kiteclass.core.module.invoice.service;

import com.kiteclass.core.module.invoice.dto.CreateInstallmentPlanRequest;
import com.kiteclass.core.module.invoice.dto.InstallmentPlanResponse;
import jakarta.validation.Valid;

import java.math.BigDecimal;

/**
 * Service interface for installment plan management.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
public interface InstallmentPlanService {

    /**
     * Requests an installment plan for an invoice.
     *
     * <p>Creates plan with N installments, status PENDING (requires approval).
     *
     * @param request plan creation request
     * @return created installment plan response DTO
     */
    InstallmentPlanResponse requestInstallmentPlan(@Valid CreateInstallmentPlanRequest request);

    /**
     * Approves an installment plan (admin action).
     *
     * @param planId the plan ID
     * @param approvedBy the user ID who approved
     * @return approved installment plan response DTO
     */
    InstallmentPlanResponse approveInstallmentPlan(Long planId, Long approvedBy);

    /**
     * Rejects an installment plan (admin action).
     *
     * @param planId the plan ID
     * @param reason the rejection reason
     * @return rejected installment plan response DTO
     */
    InstallmentPlanResponse rejectInstallmentPlan(Long planId, String reason);

    /**
     * Records payment for an installment.
     *
     * <p>Updates paid amount and status. Called by Payment Module (future PR).
     *
     * @param installmentId the installment ID
     * @param amount the payment amount
     * @return updated installment plan response DTO
     */
    InstallmentPlanResponse recordInstallmentPayment(Long installmentId, BigDecimal amount);

    /**
     * Gets installment plan by ID.
     *
     * @param id the plan ID
     * @return installment plan response DTO
     */
    InstallmentPlanResponse getInstallmentPlanById(Long id);

    /**
     * Gets installment plan by invoice ID.
     *
     * @param invoiceId the invoice ID
     * @return installment plan response DTO
     */
    InstallmentPlanResponse getInstallmentPlanByInvoiceId(Long invoiceId);
}
