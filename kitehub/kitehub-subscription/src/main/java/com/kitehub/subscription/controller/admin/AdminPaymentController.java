package com.kitehub.subscription.controller.admin;

import com.kitehub.subscription.dto.AdminConfirmPaymentRequest;
import com.kitehub.subscription.dto.AdminRejectPaymentRequest;
import com.kitehub.subscription.dto.PaymentResponse;
import com.kitehub.subscription.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin endpoints for manual payment confirmation + rejection (Wave flow-kh3, UC-SUB-07).
 *
 * <p>Implements the {@code /api/platform/admin/payments/**} contract in
 * {@code documents/01-business/kitehub/subscription-billing/api-contract.md}. The 3
 * existing {@link com.kitehub.subscription.service.PaymentService} methods
 * ({@code getPendingPayments}, {@code confirmPayment}, {@code rejectPayment}) are wired
 * here so admins can drive the manual VietQR reconciliation flow without falling back to
 * direct DB writes.</p>
 *
 * <p><strong>Auth</strong>: all routes live under {@code /api/platform/admin/**} so the
 * existing {@link com.kitehub.subscription.config.AdminApiKeyInterceptor} enforces
 * {@code X-Admin-Key} automatically — no per-method {@code @PreAuthorize} needed (same
 * pattern as {@link AdminMigrationController}).</p>
 *
 * <p><strong>Error handling</strong>: {@code IllegalArgumentException} thrown by the
 * service (payment not found, payment not pending, amount mismatch) is mapped to
 * RFC 7807 ProblemDetail by the existing
 * {@link com.kitehub.subscription.exception.GlobalExceptionHandler}.</p>
 *
 * @author KiteHub Team
 * @since Wave flow-kh3 (2026-06-04)
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/admin/payments")
@RequiredArgsConstructor
@Tag(name = "Admin Payments", description = "Manual VietQR confirm + reject ops (UC-SUB-07)")
public class AdminPaymentController {

    private final PaymentService paymentService;

    /**
     * List all pending payments for the reconciliation queue.
     *
     * @return list of {@link PaymentResponse} with {@code status=PENDING}
     */
    @Operation(summary = "Admin: list pending payments (UC-SUB-07)",
        description = "Returns payments that admin needs to reconcile against the bank statement.")
    @GetMapping("/pending")
    public ResponseEntity<List<PaymentResponse>> listPending() {
        List<PaymentResponse> pending = paymentService.getPendingPayments();
        return ResponseEntity.ok(pending);
    }

    /**
     * Confirm a pending payment manually.
     *
     * <p>Service triggers {@code SubscriptionService.applyPendingUpgrade} on success so
     * the tier flips inside the same transaction (already wired into
     * {@link PaymentService#confirmPayment}).</p>
     *
     * @param id            payment UUID
     * @param request       confirm payload containing the bank transaction id
     * @return updated {@link PaymentResponse} with {@code status=COMPLETED}
     */
    @Operation(summary = "Admin: confirm pending payment (UC-SUB-07)",
        description = "Marks payment COMPLETED + applies pending subscription upgrade.")
    @PostMapping("/{id}/confirm")
    public ResponseEntity<PaymentResponse> confirm(
        @PathVariable UUID id,
        @Valid @RequestBody AdminConfirmPaymentRequest request) {

        log.info("Admin confirm payment id={} transactionId={}", id, request.getTransactionId());
        PaymentResponse response = paymentService.confirmPayment(id, request.getTransactionId());
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a pending payment manually.
     *
     * <p>Service triggers {@code SubscriptionService.clearPendingUpgrade} so the tenant
     * keeps the previous tier + the pending upgrade slot frees up.</p>
     *
     * @param id        payment UUID
     * @param request   reject payload containing the rejection reason
     * @return updated {@link PaymentResponse} with {@code status=FAILED}
     */
    @Operation(summary = "Admin: reject pending payment (UC-SUB-07)",
        description = "Marks payment FAILED + clears pending tier upgrade slot.")
    @PostMapping("/{id}/reject")
    public ResponseEntity<PaymentResponse> reject(
        @PathVariable UUID id,
        @Valid @RequestBody AdminRejectPaymentRequest request) {

        log.info("Admin reject payment id={} reason={}", id, request.getReason());
        PaymentResponse response = paymentService.rejectPayment(id, request.getReason());
        return ResponseEntity.ok(response);
    }
}
