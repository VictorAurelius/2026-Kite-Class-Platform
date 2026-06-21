package com.kiteclass.core.module.invoice.controller;

import com.kiteclass.core.module.invoice.dto.CreateRefundRequestRequest;
import com.kiteclass.core.module.invoice.dto.RefundRequestResponse;
import com.kiteclass.core.module.invoice.service.RefundRequestService;
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

/**
 * REST controller for refund request management.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@RestController
@RequestMapping("/api/v1/refund-requests")
@RequiredArgsConstructor
@Slf4j
public class RefundRequestController {

    private final RefundRequestService refundRequestService;

    /**
     * Creates a refund request.
     *
     * @param request refund request creation request
     * @return created refund request response DTO
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PLATFORM_ADMIN', 'STAFF')")
    public ResponseEntity<RefundRequestResponse> createRefundRequest(
            @Valid @RequestBody CreateRefundRequestRequest request) {

        log.info("POST /api/v1/refund-requests: invoiceId={}, amount={}",
                request.getInvoiceId(), request.getRefundAmount());

        RefundRequestResponse refundRequest = refundRequestService.createRefundRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(refundRequest);
    }

    /**
     * Gets refund request by ID.
     *
     * @param id the refund request ID
     * @return refund request response DTO
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PRINCIPAL', 'PLATFORM_ADMIN', 'STAFF')")
    public ResponseEntity<RefundRequestResponse> getRefundRequestById(@PathVariable Long id) {
        log.info("GET /api/v1/refund-requests/{}", id);
        RefundRequestResponse refundRequest = refundRequestService.getRefundRequestById(id);
        return ResponseEntity.ok(refundRequest);
    }

    /**
     * Approves a refund request (admin endpoint).
     *
     * @param id the refund request ID
     * @param approvedBy the user ID who approved
     * @return approved refund request response DTO
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PLATFORM_ADMIN', 'STAFF')")
    public ResponseEntity<RefundRequestResponse> approveRefund(
            @PathVariable Long id,
            @RequestParam Long approvedBy) {

        log.info("PUT /api/v1/refund-requests/{}/approve by user {}", id, approvedBy);
        RefundRequestResponse refundRequest = refundRequestService.approveRefund(id, approvedBy);
        return ResponseEntity.ok(refundRequest);
    }

    /**
     * Rejects a refund request (admin endpoint).
     *
     * @param id the refund request ID
     * @param rejectedBy the user ID who rejected
     * @param reason the rejection reason
     * @return rejected refund request response DTO
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PLATFORM_ADMIN', 'STAFF')")
    public ResponseEntity<RefundRequestResponse> rejectRefund(
            @PathVariable Long id,
            @RequestParam Long rejectedBy,
            @RequestParam String reason) {

        log.info("PUT /api/v1/refund-requests/{}/reject by user {}: {}",
                id, rejectedBy, reason);

        RefundRequestResponse refundRequest = refundRequestService.rejectRefund(
                id, rejectedBy, reason);
        return ResponseEntity.ok(refundRequest);
    }

    /**
     * Processes (completes) a refund request.
     *
     * @param id the refund request ID
     * @return processed refund request response DTO
     */
    @PostMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PLATFORM_ADMIN', 'STAFF')")
    public ResponseEntity<RefundRequestResponse> processRefund(@PathVariable Long id) {
        log.info("POST /api/v1/refund-requests/{}/process", id);
        RefundRequestResponse refundRequest = refundRequestService.processRefund(id);
        return ResponseEntity.ok(refundRequest);
    }
}
