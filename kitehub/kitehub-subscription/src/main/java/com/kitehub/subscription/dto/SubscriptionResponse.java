package com.kitehub.subscription.dto;

import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for subscription.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private UUID id;
    private UUID instanceId;
    private PricingTier tier;
    private BillingCycle billingCycle;
    private Long priceVnd;
    private SubscriptionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private Boolean autoRenew;
    private PricingTier pendingTier;
    private UUID pendingPaymentId;
    private Boolean isActive;
    private Boolean isExpired;

    /**
     * Convert Subscription entity to response DTO.
     *
     * @param subscription Subscription entity
     * @return SubscriptionResponse
     */
    public static SubscriptionResponse fromEntity(Subscription subscription) {
        return SubscriptionResponse.builder()
            .id(subscription.getId())
            .instanceId(subscription.getInstanceId())
            .tier(subscription.getTier())
            .billingCycle(subscription.getBillingCycle())
            .priceVnd(subscription.getPriceVnd())
            .status(subscription.getStatus())
            .startedAt(subscription.getStartedAt())
            .expiresAt(subscription.getExpiresAt())
            .autoRenew(subscription.getAutoRenew())
            .pendingTier(subscription.getPendingTier())
            .pendingPaymentId(subscription.getPendingPaymentId())
            .isActive(subscription.isActive())
            .isExpired(subscription.isExpired())
            .build();
    }
}
