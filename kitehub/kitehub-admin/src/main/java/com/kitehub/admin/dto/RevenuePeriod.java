package com.kitehub.admin.dto;

/**
 * Revenue report aggregation period.
 *
 * <p>Typed replacement for the free-form {@code String period} param previously accepted by
 * admin revenue endpoints (GAP-654). Binding a {@code RevenuePeriod} enum on the controller
 * lets Spring reject invalid values with a 400 (instead of silently passing arbitrary strings
 * to {@code AnalyticsService}), and lets springdoc-openapi auto-document the allowed values.</p>
 *
 * @since 1.0
 */
public enum RevenuePeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY
}
