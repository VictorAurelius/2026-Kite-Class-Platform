package com.kitehub.subscription.dto;

import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for Instance.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceResponse {

    private UUID id;
    private String subdomain;
    /** Normalized URL/routing slug (GAP-535 Wave 77 + GAP-823 Wave local-doable-9). */
    private String slug;
    private String customDomain;
    private String organizationName;
    private UUID ownerId;
    private String contactEmail;
    private PricingTier tier;
    private InstanceStatus status;
    private LocalDateTime trialStartedAt;
    private LocalDateTime trialExpiresAt;
    private Long trialDaysLeft;
    private UUID subscriptionId;
    private LocalDateTime subscriptionExpiresAt;
    private Boolean isActive;
    private Boolean isOnTrial;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Notification preferences (GAP-098)
    private Boolean emailNotifications;
    private Boolean trialReminders;
}
