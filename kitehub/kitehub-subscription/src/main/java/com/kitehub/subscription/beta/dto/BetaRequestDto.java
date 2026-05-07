package com.kitehub.subscription.beta.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public submit payload for {@code POST /api/v1/auth/request-beta-access}.
 *
 * <p>The {@code honeypot} field MUST be empty (anti-bot trap) — non-empty value
 * indicates an automated submission and is silently rejected by the controller.</p>
 *
 * @param email required, validated as RFC-5321 length ≤320
 * @param name required, ≤200 chars
 * @param orgName required, ≤200 chars
 * @param persona enumerated string: P1_SOLO_TEACHER / P2_CENTER_OWNER
 * @param referralSource optional, ≤500 chars
 * @param honeypot anti-bot trap; MUST be empty
 *
 * @since Wave 33 — GAP-372
 */
public record BetaRequestDto(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 200) String orgName,
        @NotBlank @Pattern(regexp = "P1_SOLO_TEACHER|P2_CENTER_OWNER",
                message = "persona must be P1_SOLO_TEACHER or P2_CENTER_OWNER") String persona,
        @Size(max = 500) String referralSource,
        @Size(max = 0, message = "honeypot must be empty") String honeypot
) {}
