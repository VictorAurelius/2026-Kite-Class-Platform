package com.kitehub.admin.controller;

import com.kitehub.admin.dto.PaymentsSummaryResponse;
import com.kitehub.subscription.dto.PaymentResponse;
import com.kitehub.subscription.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Admin v1 - Payments", description = "Admin payments listing + summary (Wave 92 Bucket D — Wave 90 404 fix)")
public class AdminPaymentsController {

    /**
     * GAP-1360: hard cap on the pending-payment listing. The queue can accumulate when admins
     * fall behind on confirmation; without a bound the endpoint materialised + serialised the
     * whole queue. The {@code List<PaymentResponse>} response shape is retained because the
     * frontend ({@code useAdminPendingPayments}) consumes a JSON array — emitting a {@code Page}
     * envelope would break that caller. A full {@code Page<>} envelope + FE consumption is
     * deferred to a frontend-coordinated change (gap stays PARTIAL).
     */
    static final int PENDING_PAYMENTS_MAX = 500;

    private final PaymentService paymentService;

    /**
     * Get list of pending payments (bounded to {@link #PENDING_PAYMENTS_MAX}).
     *
     * @return list of pending payments
     */
    @GetMapping("/pending")
    @Operation(summary = "List pending payments", description = "Payments awaiting admin confirmation")
    public ResponseEntity<List<PaymentResponse>> listPendingPayments() {
        log.info("Admin v1 list pending payments");
        List<PaymentResponse> payments = paymentService
                .getPendingPayments(PageRequest.of(0, PENDING_PAYMENTS_MAX))
                .getContent();
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payments summary statistics.
     *
     * <p>Returns aggregate counters useful for admin dashboard. Per Wave 92 Bucket D scope this is
     * a pending-only stub: {@code totalCount}/{@code totalAmountVnd} reflect the pending set and
     * {@code completedCount} stays {@code 0} (confirmed/historical breakdown deferred to Phase 1.5+).
     * Returns a typed {@link PaymentsSummaryResponse} (GAP-654) replacing the prior untyped Map so
     * the contract is springdoc-discoverable + compiler-checked; business values unchanged from the
     * prior stub (pending count is the only real signal computed).</p>
     *
     * @return payments summary stats
     */
    @GetMapping("/summary")
    @Operation(summary = "Payments summary stats", description = "Aggregate payment counters for dashboard")
    public ResponseEntity<PaymentsSummaryResponse> getPaymentsSummary() {
        log.info("Admin v1 payments summary");

        List<PaymentResponse> pending = paymentService.getPendingPayments();

        long pendingCount = pending.size();
        long totalPendingAmountVnd = pending.stream()
                .map(PaymentResponse::getAmountVnd)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();

        PaymentsSummaryResponse summary = new PaymentsSummaryResponse(
                totalPendingAmountVnd,
                pendingCount,
                "VND",
                pendingCount,
                0L
        );

        return ResponseEntity.ok(summary);
    }
}
