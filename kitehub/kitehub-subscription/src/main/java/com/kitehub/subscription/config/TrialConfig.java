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
     * Days before trial expiration to send warning notifications.
     */
    private List<Integer> warningDays = List.of(3, 1);

    /**
     * Midpoint day of the trial period (for engagement emails).
     */
    private int midpointDay = 7;
}
