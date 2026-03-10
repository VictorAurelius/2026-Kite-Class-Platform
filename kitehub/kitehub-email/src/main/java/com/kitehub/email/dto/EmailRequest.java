package com.kitehub.email.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Email send request.
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    /**
     * Recipient email address.
     */
    private String to;

    /**
     * Email subject.
     */
    private String subject;

    /**
     * Template name (e.g., "welcome", "trial-ending").
     */
    private String templateName;

    /**
     * Template variables for Thymeleaf.
     */
    private Map<String, Object> variables;

    /**
     * Plain HTML body (if not using template).
     */
    private String htmlBody;
}
