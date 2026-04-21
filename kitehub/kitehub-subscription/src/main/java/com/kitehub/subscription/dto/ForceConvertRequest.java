package com.kitehub.subscription.dto;

import com.kitehub.platform.domain.enums.PricingTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for {@code POST /api/platform/admin/instances/{id}/force-convert}
 * (GAP-192 Phase 4b-i, UC-T2P-05).
 *
 * <p>Admin-only — used when ops has verified payment out-of-band (bank transfer,
 * enterprise invoice) and needs to skip the gateway capture step.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192 Phase 4b-i)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForceConvertRequest {

    @NotNull(message = "tier is required")
    private PricingTier tier;

    @NotBlank(message = "billingCycle is required")
    @Size(max = 32)
    private String billingCycle;

    /** Accounting / invoice reference — persisted in the outbox event payload. */
    @NotBlank(message = "invoiceRef is required")
    @Size(max = 128)
    private String invoiceRef;

    /** Free-form audit note — required so the ops trail is never empty. */
    @NotBlank(message = "reason is required")
    @Size(max = 500)
    private String reason;
}
