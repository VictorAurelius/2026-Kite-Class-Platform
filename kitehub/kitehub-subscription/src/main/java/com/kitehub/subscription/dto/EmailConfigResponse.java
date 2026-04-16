package com.kitehub.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for email configuration state.
 * Shows which email types are enabled/disabled.
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailConfigResponse {

    /**
     * Whether the RabbitMQ queue mode is enabled (vs direct HTTP).
     */
    private boolean queueEnabled;

    /**
     * Per email-type toggle states.
     * Example: {"trial-expiration-warning": true, "suspension-notification": false}
     */
    private Map<String, Boolean> emailTypeToggles;
}
