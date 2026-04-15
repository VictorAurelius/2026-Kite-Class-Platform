package com.kiteclass.core.module.legal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /public/dmca} — DMCA takedown notice intake (ADR-012).
 *
 * <p>Public endpoint; validated at ingress. The Gateway-level {@code RateLimitingFilter} still
 * applies (no new filter configured for this route).
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.3, GAP-042)
 */
public record DmcaTakedownDto(

        @NotBlank
        @Email
        @Size(max = 255)
        String reporterEmail,

        @NotBlank
        @Size(max = 255)
        String reporterName,

        @NotBlank
        @Size(max = 2000)
        String allegedInfringingUrl,

        @NotBlank
        @Size(max = 4000)
        String copyrightedWorkDescription
) {
}
