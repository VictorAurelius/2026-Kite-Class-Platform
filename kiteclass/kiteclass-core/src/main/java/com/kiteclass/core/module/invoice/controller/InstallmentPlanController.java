package com.kiteclass.core.module.invoice.controller;

import com.kiteclass.core.module.invoice.dto.CreateInstallmentPlanRequest;
import com.kiteclass.core.module.invoice.dto.InstallmentPlanResponse;
import com.kiteclass.core.module.invoice.service.InstallmentPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * REST controller for installment plan management.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@RestController
@RequestMapping("/api/v1/installment-plans")
@RequiredArgsConstructor
@Slf4j
public class InstallmentPlanController {

    private final InstallmentPlanService installmentPlanService;

    /**
     * Requests an installment plan.
     *
     * @param request plan creation request
     * @return created installment plan response DTO
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PLATFORM_ADMIN', 'STAFF')")
    public ResponseEntity<InstallmentPlanResponse> requestInstallmentPlan(
            @Valid @RequestBody CreateInstallmentPlanRequest request) {

        log.info("POST /api/v1/installment-plans: invoiceId={}, installments={}",
                request.getInvoiceId(), request.getNumberOfInstallments());

        InstallmentPlanResponse plan = installmentPlanService.requestInstallmentPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }

    /**
     * Gets installment plan by ID.
     *
     * @param id the plan ID
     * @return installment plan response DTO
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'OWNER', 'PLATFORM_ADMIN', 'STAFF')")
    public ResponseEntity<InstallmentPlanResponse> getInstallmentPlanById(@PathVariable Long id) {
        log.info("GET /api/v1/installment-plans/{}", id);
        InstallmentPlanResponse plan = installmentPlanService.getInstallmentPlanById(id);
        return ResponseEntity.ok(plan);
    }

    /**
     * Approves an installment plan (admin endpoint).
     *
     * @param id the plan ID
     * @param approvedBy the user ID who approved (from security context in real app)
     * @return approved installment plan response DTO
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PLATFORM_ADMIN', 'STAFF')")
    public ResponseEntity<InstallmentPlanResponse> approveInstallmentPlan(
            @PathVariable Long id,
            @RequestParam Long approvedBy) {

        log.info("PUT /api/v1/installment-plans/{}/approve by user {}", id, approvedBy);
        InstallmentPlanResponse plan = installmentPlanService.approveInstallmentPlan(id, approvedBy);
        return ResponseEntity.ok(plan);
    }

    /**
     * Rejects an installment plan (admin endpoint).
     *
     * @param id the plan ID
     * @param reason the rejection reason
     * @return rejected installment plan response DTO
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PLATFORM_ADMIN', 'STAFF')")
    public ResponseEntity<InstallmentPlanResponse> rejectInstallmentPlan(
            @PathVariable Long id,
            @RequestParam String reason) {

        log.info("PUT /api/v1/installment-plans/{}/reject: {}", id, reason);
        InstallmentPlanResponse plan = installmentPlanService.rejectInstallmentPlan(id, reason);
        return ResponseEntity.ok(plan);
    }

    /**
     * Records payment for an installment.
     *
     * @param installmentId the installment ID
     * @param amount the payment amount
     * @return updated installment plan response DTO
     */
    @PostMapping("/installments/{installmentId}/payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PLATFORM_ADMIN', 'STAFF')")
    public ResponseEntity<InstallmentPlanResponse> recordInstallmentPayment(
            @PathVariable Long installmentId,
            @RequestParam BigDecimal amount) {

        log.info("POST /api/v1/installment-plans/installments/{}/payment: amount={}",
                installmentId, amount);

        InstallmentPlanResponse plan = installmentPlanService.recordInstallmentPayment(
                installmentId, amount);
        return ResponseEntity.ok(plan);
    }
}
