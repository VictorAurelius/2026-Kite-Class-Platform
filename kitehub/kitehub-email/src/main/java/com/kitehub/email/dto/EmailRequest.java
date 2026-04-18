package com.kitehub.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String to;

    /**
     * Email subject.
     */
    @NotBlank(message = "Subject is required")
    @Size(max = 500, message = "Subject must not exceed 500 characters")
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

    /**
     * Tenant instance ID (optional). When provided, branding is fetched from
     * kiteclass-core and injected into the Thymeleaf context as {@code branding}.
     *
     * @since Wave 4 (GAP-021)
     */
    private Long instanceId;

    /**
     * Tenant identifier header value (optional). Forwarded to the branding
     * service as {@code X-Tenant-Id} for multi-tenant isolation.
     *
     * @since Wave 4 (GAP-021)
     */
    private String tenantId;
}
