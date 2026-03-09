package com.kitehub.subscription.dto;

import com.kitehub.platform.domain.enums.InstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for trial status information.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrialStatusResponse {

    /**
     * Instance UUID.
     */
    private UUID instanceId;

    /**
     * Instance subdomain.
     */
    private String subdomain;

    /**
     * Current instance status.
     */
    private InstanceStatus status;

    /**
     * Whether instance is currently on trial.
     */
    private Boolean isOnTrial;

    /**
     * Trial start timestamp.
     */
    private LocalDateTime trialStartedAt;

    /**
     * Trial expiration timestamp.
     */
    private LocalDateTime trialExpiresAt;

    /**
     * Number of days left in trial.
     */
    private Long daysLeft;

    /**
     * Whether instance needs warning notification.
     */
    private Boolean needsWarning;

    /**
     * Warning level: NONE, MEDIUM (2-3 days), HIGH (1 day), EXPIRED.
     */
    private String warningLevel;
}
