package com.kitehub.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for email events published to RabbitMQ queue.
 * <p>
 * Serialized as JSON via Jackson2JsonMessageConverter.
 * Consumer reads this and sends the actual HTTP request to email service.
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Instance ID for tracking (nullable for system-level emails).
     */
    private UUID instanceId;

    /**
     * Recipient email address.
     */
    private String to;

    /**
     * Email subject line.
     */
    private String subject;

    /**
     * Template name (e.g., "trial-expiration-warning", "welcome").
     */
    private String templateName;

    /**
     * Template variables for rendering.
     */
    private Map<String, Object> variables;

    /**
     * Email type for idempotency tracking (e.g., "trial-warning", "welcome").
     */
    private String emailType;
}
