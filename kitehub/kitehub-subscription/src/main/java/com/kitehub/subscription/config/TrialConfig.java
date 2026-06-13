package com.kitehub.subscription.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for trial period settings.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "kitehub.trial")
@Data
public class TrialConfig {

    /**
     * Duration of the trial period in days.
     */
    private int durationDays = 14;

    /**
     * Maximum number of trial instances per owner.
     */
    private int maxPerOwner = 1;

    /**
     * Days before trial expiration to send warning notifications (TR-03 / TR-08).
     *
     * <p>Widened from {@code [3, 1]} to {@code [10, 5, 3, 1]} per GAP-1270 to raise
     * conversion touch-points toward the industry 5-7 cadence. On a 14-day trial these
     * land on trial days 4 / 9 / 11 / 13; combined with the midpoint engagement email
     * (day {@code midpointDay}) + the welcome email (day 0) → ~6 touch-points. None of
     * these values equal {@code midpointDay} (7), so no same-day double-send.</p>
     */
    private List<Integer> warningDays = List.of(10, 5, 3, 1);

    /**
     * Midpoint day of the trial period (for engagement emails).
     */
    private int midpointDay = 7;

    /**
     * Trial extension/rescue length in days (TR-08, GAP-1270). Applied by
     * {@code TrialExpirationChecker.grantTrialExtension(...)} on an admin/auto rescue.
     */
    private int extensionDays = 7;

    /**
     * When true, an expiring trial is auto-granted ONE extension (of {@code extensionDays})
     * instead of being suspended (TR-08 auto-rescue). Default false — preserves the
     * suspend-on-expiry behavior; ops opts in. Bounded to a single auto-extension per trial
     * (derived from {@code trialStartedAt + durationDays}; no per-trial counter column).
     */
    private boolean autoExtendOnExpiry = false;
}
