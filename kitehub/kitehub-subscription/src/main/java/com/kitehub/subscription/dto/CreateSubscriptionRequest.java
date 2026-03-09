package com.kitehub.subscription.dto;

import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.PricingTier;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a subscription.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {

    /**
     * Instance ID.
     */
    @NotNull(message = "Instance ID is required")
    private UUID instanceId;

    /**
     * Pricing tier.
     */
    @NotNull(message = "Tier is required")
    private PricingTier tier;

    /**
     * Billing cycle.
     */
    @NotNull(message = "Billing cycle is required")
    private BillingCycle billingCycle;

    /**
     * Auto-renew flag (default true).
     */
    private Boolean autoRenew = true;
}
