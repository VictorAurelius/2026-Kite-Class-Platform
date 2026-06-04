package com.kitehub.subscription.controller.admin;

import com.kitehub.subscription.dto.ForceConvertRequest;
import com.kitehub.subscription.dto.RollbackRequest;
import com.kitehub.subscription.dto.RollbackResponse;
import com.kitehub.subscription.dto.UpgradeRequest;
import com.kitehub.subscription.dto.UpgradeResponse;
import com.kitehub.subscription.service.TrialToPaidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin-only endpoints for ops-driven migration operations (GAP-192 Phase 4b-i).
 *
 * <p>All routes require a JWT with role {@code PLATFORM_ADMIN}. The gateway forwards the
 * role as {@code X-User-Roles} header; Spring Security maps it to {@code ROLE_PLATFORM_ADMIN}
 * and {@link PreAuthorize} on each method enforces access (GAP-938, Wave flow-kh3 —
 * supersedes the legacy {@code X-Admin-Key} interceptor that was deleted because Wave 79
 * default-deny made it dead code).</p>
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code POST /admin/instances/{id}/force-convert} — UC-T2P-05: ops verifies
 *       payment out-of-band (bank transfer, invoice) and skips the gateway step.</li>
 *   <li>{@code POST /admin/instances/{id}/rollback-migration} — UC-T2P-02: manual
 *       rollback trigger within the 24h reversal window (T2P-04).</li>
 * </ul>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192 Phase 4b-i)
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Migration", description = "Ops endpoints for force-convert + rollback")
public class AdminMigrationController {

    private final TrialToPaidService trialToPaidService;

    /**
     * UC-T2P-05 force-convert — admin-only. Builds the canonical {@link UpgradeRequest}
     * from the admin payload (no payment-method id because the payment is already
     * booked out-of-band) and advances the instance to PAYMENT_CAPTURED.
     */
    @Operation(summary = "Admin: force-convert a trial to paid (UC-T2P-05)",
        description = "Skips gateway capture — used for verified bank transfers / enterprise invoices.")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @PostMapping("/instances/{id}/force-convert")
    public ResponseEntity<UpgradeResponse> forceConvert(
        @PathVariable UUID id,
        @Valid @RequestBody ForceConvertRequest request) {

        UpgradeRequest upgrade = UpgradeRequest.builder()
            .tier(request.getTier())
            .billingCycle(request.getBillingCycle())
            // Admin flow has no payment-method id — synthesise a stable sentinel so
            // the @NotBlank validator on UpgradeRequest is satisfied.
            .paymentMethodId("admin-force-convert:" + request.getInvoiceRef())
            // Admin flow uses invoice ref as idempotency key so double-clicks are safe.
            .idempotencyKey("admin:" + request.getInvoiceRef())
            .build();

        UpgradeResponse response = trialToPaidService.forceConvert(
            id, upgrade, request.getInvoiceRef(), request.getReason());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * UC-T2P-02 manual rollback — admin-only. Errors (e.g. REVERSAL_WINDOW_EXPIRED)
     * flow through {@link com.kitehub.subscription.exception.GlobalExceptionHandler}.
     */
    @Operation(summary = "Admin: rollback a completed migration (UC-T2P-02)",
        description = "Only works within the 24h reversal window — else returns 410.")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @PostMapping("/instances/{id}/rollback-migration")
    public ResponseEntity<RollbackResponse> rollback(
        @PathVariable UUID id,
        @Valid @RequestBody RollbackRequest request) {

        RollbackResponse response = trialToPaidService.rollback(id, request.getReason());
        return ResponseEntity.ok(response);
    }
}
