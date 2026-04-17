package com.kitehub.subscription.dto;

import com.kitehub.platform.domain.enums.PricingTier;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an instance.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInstanceRequest {

    @Size(min = 2, max = 200, message = "Organization name must be between 2 and 200 characters")
    private String organizationName;

    private PricingTier tier;

    @Size(max = 255, message = "Custom domain must not exceed 255 characters")
    private String customDomain;
}
