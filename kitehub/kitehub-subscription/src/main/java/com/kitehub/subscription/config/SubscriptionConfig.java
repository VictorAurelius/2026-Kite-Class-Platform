package com.kitehub.subscription.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for subscription settings.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "kitehub.subscription")
@Data
public class SubscriptionConfig {

    /**
     * Grace period in days after subscription expires before suspension.
     */
    private int gracePeriodDays = 3;

    /**
     * Days before subscription expiration to send warning notifications.
     */
    private List<Integer> warningDays = List.of(7, 3, 1);
}
