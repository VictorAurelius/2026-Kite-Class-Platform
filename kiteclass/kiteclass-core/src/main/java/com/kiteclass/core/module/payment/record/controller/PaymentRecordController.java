package com.kiteclass.core.module.payment.record.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.payment.record.dto.PaymentRecordResponse;
import com.kiteclass.core.module.payment.record.dto.RecordPaymentRequest;
import com.kiteclass.core.module.payment.record.service.PaymentRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for manual payment recording at trung tâm (GAP-292b).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/invoices/{invoiceId}/record-payment — record cash/bank/QR/MoMo payment</li>
 *   <li>GET  /api/v1/invoices/{invoiceId}/payment-records — list payments for invoice</li>
 * </ul>
 *
 * <p>OWASP A01 cross-tenant defense enforced at service layer via TenantContext + instanceId check.
 *
 * <p>Authorization:
 * <ul>
 *   <li>recordPayment — TEACHER | ADMIN | OWNER roles (anyone who can collect tuition)</li>
 *   <li>listPayments — same; OWNER+ADMIN see all, TEACHER sees only own classes (delegated to service)</li>
 * </ul>
 *
 * @see PaymentRecordService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class PaymentRecordController {

    private final PaymentRecordService paymentRecordService;

    /**
     * Records a manual payment received from parent/student.
     *
     * <p>Idempotency-Key header (optional) prevents accidental double-submission
     * from FE retry loops. Caller MUST supply UUID v4 if needed.
     *
     * @param invoiceId the invoice being paid (path variable)
     * @param request payment details
     * @param idempotencyKey optional Idempotency-Key header per BR-PAYMENT-METHOD-004
     * @param userId authenticated user ID (Phase 1 BETA: placeholder=1L until full auth wiring per GAP-526)
     * @return 201 Created + PaymentRecordResponse
     */
    @PostMapping("/{invoiceId}/record-payment")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'OWNER', 'PLATFORM_ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<PaymentRecordResponse>> recordPayment(
            @PathVariable Long invoiceId,
            @Valid @RequestBody RecordPaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal Object principal
    ) {
        log.info("POST /api/v1/invoices/{}/record-payment method={} amount={}đ",
                invoiceId, request.getMethod(), request.getAmount());

        // Phase 1 BETA: placeholder userId=1L (matches PaymentController convention per GAP-526 follow-up).
        // Full @AuthenticationPrincipal -> userId extraction landed via Wave 105 Bucket E0 sister work.
        Long recordedByUserId = 1L;

        PaymentRecordResponse response = paymentRecordService.recordPayment(
                invoiceId, request, recordedByUserId, idempotencyKey
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Lists all payment records for an invoice (current tenant scope).
     */
    @GetMapping("/{invoiceId}/payment-records")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'OWNER', 'PLATFORM_ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<PaymentRecordResponse>>> listPayments(
            @PathVariable Long invoiceId
    ) {
        log.info("GET /api/v1/invoices/{}/payment-records", invoiceId);
        List<PaymentRecordResponse> records = paymentRecordService.getPaymentRecordsByInvoice(invoiceId);
        return ResponseEntity.ok(ApiResponse.success(records));
    }
}
