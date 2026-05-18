package com.kitehub.admin.controller;

import com.kitehub.subscription.dto.PaymentResponse;
import com.kitehub.subscription.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin payments v1 REST API — exposes payments listing + summary stats at the canonical
 * {@code /api/v1/admin/payments} path expected by frontend + integration consumers.
 *
 * <p>Fixes Wave 90 walkthrough sub-finding (404 at {@code /api/v1/admin/payments}): legacy
 * {@link AdminController} mounts payment APIs at {@code /api/platform/admin/payments/...};
 * this v1 controller provides the canonical path. Both prefixes coexist in Phase 1 BETA —
 * legacy path deprecation deferred to Phase 1.5+ when frontend consolidation complete.</p>
 *
 * <p>Per Wave 92 Bucket D, this controller is a thin read-only stub: list + summary GET
 * endpoints only. Mutation operations (confirm/reject) remain on legacy {@link AdminController}
 * until v1 mutation scope plan (defer follow-up gap).</p>
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
@Tag(name = "Admin v1 - Payments", description = "Admin payments listing + summary (Wave 92 Bucket D — Wave 90 404 fix)")
public class AdminPaymentsController {

    private final PaymentService paymentService;

    /**
     * Get list of pending payments.
     *
     * @return list of pending payments
     */
    @GetMapping("/pending")
    @Operation(summary = "List pending payments", description = "Payments awaiting admin confirmation")
    public ResponseEntity<List<PaymentResponse>> listPendingPayments() {
        log.info("Admin v1 list pending payments");
        List<PaymentResponse> payments = paymentService.getPendingPayments();
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payments summary statistics.
     *
     * <p>Returns aggregate counters useful for admin dashboard: total pending count + pending
     * count snapshot. Mutation history breakdown deferred to follow-up scope.</p>
     *
     * @return payments summary stats
     */
    @GetMapping("/summary")
    @Operation(summary = "Payments summary stats", description = "Aggregate payment counters for dashboard")
    public ResponseEntity<Map<String, Object>> getPaymentsSummary() {
        log.info("Admin v1 payments summary");

        List<PaymentResponse> pending = paymentService.getPendingPayments();

        Map<String, Object> summary = new HashMap<>();
        summary.put("pendingCount", (long) pending.size());
        summary.put("scope", "pending-only-v1-stub");
        summary.put("note", "Wave 92 Bucket D stub — extended breakdown (confirmed/rejected/historical) defer Phase 1.5+");

        return ResponseEntity.ok(summary);
    }
}
