package com.kitehub.subscription.controller;

import com.kitehub.subscription.config.DataRetentionConfig;
import com.kitehub.subscription.config.SubscriptionConfig;
import com.kitehub.subscription.config.TrialConfig;
import com.kitehub.subscription.dto.PublicConfigResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller exposing public (non-sensitive) platform configuration.
 * No authentication required — frontend reads this on startup so it never
 * has to hard-code business values like trial days or retention periods.
 *
 * Endpoint:
 *   GET /api/platform/config/public
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/platform/config")
@RequiredArgsConstructor
@Tag(name = "Public Config", description = "Public configuration for frontend clients")
public class PublicConfigController {

    private final TrialConfig trialConfig;
    private final SubscriptionConfig subscriptionConfig;
    private final DataRetentionConfig dataRetentionConfig;

    /**
     * Return public platform configuration values.
     * This endpoint is unauthenticated and cacheable.
     *
     * @return PublicConfigResponse containing trial, subscription and retention settings
     */
    @Operation(summary = "Get public platform configuration",
               description = "Returns non-sensitive business configuration for frontend clients. No auth required.")
    @GetMapping("/public")
    public ResponseEntity<PublicConfigResponse> getPublicConfig() {
        return ResponseEntity.ok(PublicConfigResponse.builder()
            .trialDays(trialConfig.getDurationDays())
            .trialMaxPerOwner(trialConfig.getMaxPerOwner())
            .gracePeriodDays(subscriptionConfig.getGracePeriodDays())
            .retentionDays(Map.of(
                "trial", dataRetentionConfig.getTrial(),
                "free", dataRetentionConfig.getFree(),
                "basic", dataRetentionConfig.getBasic(),
                "premium", dataRetentionConfig.getPremium(),
                "enterprise", dataRetentionConfig.getEnterprise()
            ))
            .build());
    }
}
