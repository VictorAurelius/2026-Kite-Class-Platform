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

    /**
     * TTL in days for a {@code Payment} stuck in PENDING (SUB-23). Past this age the
     * payment is auto-FAILED (timeout) and the subscription's pendingPaymentId released
     * so a fresh renewal/upgrade attempt is possible.
     */
    private int pendingPaymentTtlDays = 7;

    /**
     * TTL in days for an un-activated PENDING subscription (GAP-1080 AC#2). Past this age
     * an orphan PENDING subscription (instance never activated) is soft-deleted by the
     * cleanup sweep.
     */
    private int orphanPendingSubscriptionTtlDays = 7;
}
