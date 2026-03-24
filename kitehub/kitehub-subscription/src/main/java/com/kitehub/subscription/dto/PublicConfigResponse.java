package com.kitehub.subscription.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Response DTO for public platform configuration.
 * Exposes non-sensitive business config to frontend clients,
 * so they do not need to hard-code trial days, retention policies, etc.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
public class PublicConfigResponse {

    /**
     * Trial period duration in days (default 14).
     */
    private int trialDays;

    /**
     * Maximum number of trial instances allowed per owner.
     */
    private int trialMaxPerOwner;

    /**
     * Grace period (days) after subscription expires before suspension.
     */
    private int gracePeriodDays;

    /**
     * Data retention days per tier after suspension.
     * Keys: "trial", "free", "basic", "premium", "enterprise".
     */
    private Map<String, Integer> retentionDays;
}
