package com.kitehub.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for email aggregate statistics.
 * Used by admin dashboard to monitor email health.
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailStatsResponse {

    private long totalSentToday;
    private long totalSentThisWeek;
    private long failedToday;

    /**
     * Count of emails sent today grouped by email type.
     * Example: {"trial-warning": 15, "renewal-reminder": 8}
     */
    private Map<String, Long> countByType;
}
