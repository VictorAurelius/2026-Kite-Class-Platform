package com.kitehub.subscription.dto;

import com.kitehub.platform.domain.enums.PricingTier;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for tier change (upgrade/downgrade).
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierChangeRequest {

    /**
     * New pricing tier.
     */
    @NotNull(message = "New tier is required")
    private PricingTier newTier;
}
