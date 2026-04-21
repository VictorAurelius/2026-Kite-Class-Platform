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
 * Request body for {@code POST /api/platform/instances/{id}/upgrade}.
 *
 * <p>See {@code documents/01-business/kitehub/trial-to-paid-migration/api-contract.md}
 * for the full endpoint contract.</p>
 *
 * @author KiteHub Team
 * @since 1.0.0 (GAP-192)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeRequest {

    /** Target paid tier. */
    @NotNull(message = "tier is required")
    private PricingTier tier;

    /** Billing cycle label — kept as free string (MONTHLY / ANNUAL); validated by BillingService. */
    @NotBlank(message = "billingCycle is required")
    @Size(max = 32)
    private String billingCycle;

    /** Pre-registered payment-method id (e.g. pm_xxx from gateway). */
    @NotBlank(message = "paymentMethodId is required")
    @Size(max = 128)
    private String paymentMethodId;

    /**
     * Client-supplied idempotency key (uuid-v4). Persistence deferred to Phase 4b —
     * for MVP the service accepts the field but does not short-circuit on duplicates.
     */
    @Size(max = 64)
    private String idempotencyKey;
}
