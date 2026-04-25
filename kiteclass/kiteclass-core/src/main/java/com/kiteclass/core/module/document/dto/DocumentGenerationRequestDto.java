package com.kiteclass.core.module.document.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * HTTP body for {@code POST /api/v1/documents/{format}/preview|download}.
 *
 * <p>Callers supply the template id and template-specific data map. Tenant and branding are
 * resolved server-side from the authenticated session, never from the client body.
 */
public record DocumentGenerationRequestDto(
        @NotBlank String templateId,
        Map<String, Object> data) {
}
