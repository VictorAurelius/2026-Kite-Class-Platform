package com.kitehub.subscription.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for {@code POST /api/platform/admin/instances/{id}/retry-provisioning}.
 *
 * <p>Admin-only (PLATFORM_ADMIN) — used by ops to manually re-trigger the KiteClass
 * tenant-provisioning saga for a FAILED/stuck instance (UC-PROV-05, GAP-953).
 * Body is optional; {@code reason} is recorded in the admin audit log for forensics.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-953, Wave provisioning-1 Bucket E)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryProvisioningRequest {

    /** Optional admin-supplied reason for the retry (audit payload). */
    @Size(max = 500, message = "Reason must be 500 characters or fewer")
    private String reason;
}
