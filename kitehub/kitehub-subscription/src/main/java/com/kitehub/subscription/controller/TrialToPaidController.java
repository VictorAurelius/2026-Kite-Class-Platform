package com.kitehub.subscription.controller;

import com.kitehub.subscription.dto.UpgradeRequest;
import com.kitehub.subscription.dto.UpgradeResponse;
import com.kitehub.subscription.service.TrialToPaidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST endpoints for user-initiated trial-to-paid migration (GAP-192).
 *
 * <p>Admin-only operations (force-convert, rollback) live in
 * {@link com.kitehub.subscription.controller.admin.AdminMigrationController} so
 * the {@code AdminApiKeyInterceptor} guard (under {@code /api/platform/admin/**})
 * applies correctly.</p>
 *
 * <p>See {@code documents/01-business/kitehub/trial-to-paid-migration/api-contract.md}.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192)
 */
@RestController
@RequestMapping("/api/platform")
@RequiredArgsConstructor
@Tag(name = "Trial → Paid Migration",
    description = "Upgrade from TRIAL to paid subscription with zero-downtime state machine")
public class TrialToPaidController {

    private final TrialToPaidService trialToPaidService;

    /**
     * POST /api/platform/instances/{id}/upgrade — user-initiated upgrade (UC-T2P-01).
     * Returns 202 Accepted with the initial phase + poll URL.
     */
    @Operation(summary = "Initiate trial-to-paid upgrade",
        description = "Validates eligibility + moves to PAYMENT_PENDING. Client polls trial-status until COMPLETED.")
    @PostMapping("/instances/{id}/upgrade")
    public ResponseEntity<UpgradeResponse> upgrade(
        @PathVariable UUID id,
        @Valid @RequestBody UpgradeRequest request) {
        UpgradeResponse response = trialToPaidService.initiateUpgrade(id, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
