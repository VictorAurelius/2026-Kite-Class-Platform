package com.kitehub.subscription.controller.admin;

import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.dto.RetryProvisioningRequest;
import com.kitehub.subscription.service.AdminTenantProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin-only endpoint to manually retry tenant provisioning (GAP-953, UC-PROV-05).
 *
 * <p>Closes the gap where a PLATFORM_ADMIN seeing FAILED/stuck instances had no UI/HTTP path
 * to retry provisioning and had to SSH into RDS to flip the status by hand (audit-invisible,
 * dangerous). This endpoint re-publishes {@code tenant.created} to re-drive kiteclass-core's
 * provisioning saga + writes an audit row.</p>
 *
 * <p>Route requires JWT role {@code PLATFORM_ADMIN}. The gateway forwards the role as
 * {@code X-User-Roles} (→ {@code ROLE_PLATFORM_ADMIN}) + the acting user as {@code X-User-Id};
 * {@link PreAuthorize} enforces access (mirrors {@link AdminMigrationController}).</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-953, Wave provisioning-1 Bucket E)
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Tenant Provisioning",
     description = "Ops endpoint to retry a failed tenant provisioning (UC-PROV-05)")
public class AdminTenantProvisioningController {

    private final AdminTenantProvisioningService adminTenantProvisioningService;

    /**
     * UC-PROV-05 retry-provisioning — admin-only. Missing instance → 404 via
     * {@link com.kitehub.subscription.exception.GlobalExceptionHandler}.
     */
    @Operation(summary = "Admin: retry provisioning for a failed/stuck instance (UC-PROV-05, GAP-953)",
        description = "Re-publishes tenant.created to re-run the KiteClass provisioning saga.")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @PostMapping("/instances/{id}/retry-provisioning")
    public ResponseEntity<InstanceResponse> retryProvisioning(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String adminUserIdHeader,
            @Valid @RequestBody(required = false) RetryProvisioningRequest request) {

        UUID adminUserId = parseUuid(adminUserIdHeader);
        String reason = request != null ? request.getReason() : null;
        InstanceResponse response = adminTenantProvisioningService.retryProvisioning(id, adminUserId, reason);
        return ResponseEntity.ok(response);
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid X-User-Id header on retry-provisioning: {}", value);
            return null;
        }
    }
}
