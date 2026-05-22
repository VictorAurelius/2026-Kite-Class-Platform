package com.kitehub.subscription.beta.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public submit payload for {@code POST /api/v1/auth/request-beta-access}.
 *
 * <p>The {@code honeypot} field MUST be empty (anti-bot trap) — non-empty value
 * indicates an automated submission and is silently rejected by the controller.</p>
 *
 * <p>The {@code consentGiven} field MUST be {@code true} per PDPL 2023 Art 11
 * (explicit consent). See {@code documents/01-business/kitehub/beta-access/rules.md}
 * BR-BETA-001 for the 5-attribute compliance block + Wave 35 GAP-385.</p>
 *
 * <p><b>Wave 105 Bucket A (failure-mode matrix A4 — stored XSS hardening):</b>
 * {@code name}, {@code orgName}, {@code referralSource} reject HTML structural
 * characters ({@code <}, {@code >}, {@code &}) at input — defense-in-depth on
 * top of React JSX auto-escape in the admin panel. VN tenant names +
 * organization names + referral sources do NOT legitimately contain these
 * characters; rejection up-front prevents stored-XSS payloads from reaching
 * the outbox event payload, email templates, or any future non-React render
 * path. See {@code documents/01-business/kitehub/beta-access/rules.md} BR-BETA-005.</p>
 *
 * @param email required, validated as RFC-5321 length ≤320
 * @param name required, ≤200 chars, no HTML structural chars
 * @param orgName required, ≤200 chars, no HTML structural chars
 * @param persona enumerated string: P1_SOLO_TEACHER / P2_CENTER_OWNER
 * @param referralSource optional, ≤500 chars, no HTML structural chars
 * @param honeypot anti-bot trap; MUST be empty
 * @param consentGiven PDPL 2023 Art 11 explicit consent; MUST be {@code true}
 *
 * @since Wave 33 — GAP-372
 */
public record BetaRequestDto(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 200)
        @Pattern(regexp = BetaRequestDto.NO_HTML_CHARS_REGEX,
                message = "name must not contain HTML structural characters")
        String name,
        @NotBlank @Size(max = 200)
        @Pattern(regexp = BetaRequestDto.NO_HTML_CHARS_REGEX,
                message = "orgName must not contain HTML structural characters")
        String orgName,
        @NotBlank @Pattern(regexp = "P1_SOLO_TEACHER|P2_CENTER_OWNER",
                message = "persona must be P1_SOLO_TEACHER or P2_CENTER_OWNER") String persona,
        @Size(max = 500)
        @Pattern(regexp = BetaRequestDto.NO_HTML_CHARS_REGEX_NULLABLE,
                message = "referralSource must not contain HTML structural characters")
        String referralSource,
        @Size(max = 0, message = "honeypot must be empty") String honeypot,
        @NotNull(message = "BETA_CONSENT_REQUIRED") Boolean consentGiven
) {

    /**
     * Wave 105 Bucket A (failure-mode A4) — XSS input sanitization regex.
     * Bans {@code < > &} explicitly. Rationale:
     * <ul>
     *   <li>{@code <} {@code >} — HTML tag delimiters (script injection vector)</li>
     *   <li>{@code &} — HTML entity prefix (encoded-payload bypass)</li>
     * </ul>
     * Accepts everything else including VN diacritics, spaces, apostrophes,
     * hyphens, dots, parentheses (legitimate in "Trung tâm Sky Education (Q.1)").
     */
    static final String NO_HTML_CHARS_REGEX = "^[^<>&]+$";

    /** Nullable variant — matches empty/null OR safe chars. */
    static final String NO_HTML_CHARS_REGEX_NULLABLE = "^$|^[^<>&]+$";

    /**
     * PDPL 2023 Art 11 — consent MUST be explicit {@code true}.
     * {@code @AssertTrue} guards against {@code false} once {@code @NotNull}
     * has guarded {@code null}/missing. Validation message is the error code
     * surfaced to clients (per api-contract.md error table).
     */
    @AssertTrue(message = "BETA_CONSENT_REQUIRED")
    @JsonIgnore
    public boolean isConsentAccepted() {
        return Boolean.TRUE.equals(consentGiven);
    }
}
