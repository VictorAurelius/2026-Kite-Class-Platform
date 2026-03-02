package com.kiteclass.core.module.invoice.service;

import com.kiteclass.core.module.invoice.dto.CreateRefundRequestRequest;
import com.kiteclass.core.module.invoice.dto.RefundRequestResponse;
import jakarta.validation.Valid;

/**
 * Service interface for refund request management.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
public interface RefundRequestService {

    /**
     * Creates a refund request for an invoice.
     *
     * @param request refund request creation request
     * @return created refund request response DTO
     */
    RefundRequestResponse createRefundRequest(@Valid CreateRefundRequestRequest request);

    /**
     * Approves a refund request (admin action).
     *
     * @param id the refund request ID
     * @param approvedBy the user ID who approved
     * @return approved refund request response DTO
     */
    RefundRequestResponse approveRefund(Long id, Long approvedBy);

    /**
     * Rejects a refund request (admin action).
     *
     * @param id the refund request ID
     * @param rejectedBy the user ID who rejected
     * @param reason the rejection reason
     * @return rejected refund request response DTO
     */
    RefundRequestResponse rejectRefund(Long id, Long rejectedBy, String reason);

    /**
     * Processes (completes) a refund request.
     *
     * <p>Creates InvoiceAdjustment and updates invoice total.
     *
     * @param id the refund request ID
     * @return processed refund request response DTO
     */
    RefundRequestResponse processRefund(Long id);

    /**
     * Gets refund request by ID.
     *
     * @param id the refund request ID
     * @return refund request response DTO
     */
    RefundRequestResponse getRefundRequestById(Long id);
}
