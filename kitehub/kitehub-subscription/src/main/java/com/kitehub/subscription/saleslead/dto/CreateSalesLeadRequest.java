package com.kitehub.subscription.saleslead.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public submit payload for {@code POST /api/platform/sales-leads} (GAP-1101).
 *
 * <p>KiteHub PLATFORM sales lead — a prospective center owner contacting KiteHub
 * sales about the Enterprise SaaS plan. The {@code honeypot} field MUST be empty
 * (anti-bot trap) — Bean-Validation {@code @Size(max = 0)} rejects non-empty with
 * HTTP 400 via the global handler.</p>
 *
 * <p>Stored XSS hardening (per {@code vn-localization-audit-checklist.md} §5 +
 * {@code BetaRequestDto} precedent): {@code fullName}, {@code organizationName},
 * {@code message} reject HTML structural characters ({@code <}, {@code >},
 * {@code &}) at input. The {@code [^<>&]} regex preserves Vietnamese diacritics
 * (â/ê/ô/ữ...) — defense-in-depth without the {@code HtmlUtils.htmlEscape}
 * single-arg corruption class (GAP-764).</p>
 *
 * @param fullName         required, ≤200 chars, no HTML structural chars
 * @param email            required, RFC-5321 length ≤320
 * @param phone            required, ≤20 chars, digits + common phone punctuation
 * @param organizationName required, ≤200 chars, no HTML structural chars
 * @param message          optional, ≤2000 chars, no HTML structural chars
 * @param planInterest     optional enumerated string; defaults to ENTERPRISE
 * @param honeypot         anti-bot trap; MUST be empty
 *
 * @since GAP-1101
 */
public record CreateSalesLeadRequest(
        @NotBlank @Size(max = 200)
        @Pattern(regexp = CreateSalesLeadRequest.NO_HTML_CHARS_REGEX,
                message = "fullName must not contain HTML structural characters")
        String fullName,

        @NotBlank @Email @Size(max = 320) String email,

        @NotBlank @Size(max = 20)
        @Pattern(regexp = CreateSalesLeadRequest.PHONE_REGEX,
                message = "phone must be 8-20 digits/symbols")
        String phone,

        @NotBlank @Size(max = 200)
        @Pattern(regexp = CreateSalesLeadRequest.NO_HTML_CHARS_REGEX,
                message = "organizationName must not contain HTML structural characters")
        String organizationName,

        @Size(max = 2000)
        @Pattern(regexp = CreateSalesLeadRequest.NO_HTML_CHARS_REGEX_NULLABLE,
                message = "message must not contain HTML structural characters")
        String message,

        @Pattern(regexp = "FREE|BASIC|PREMIUM|ENTERPRISE",
                message = "planInterest must be FREE/BASIC/PREMIUM/ENTERPRISE")
        String planInterest,

        @Size(max = 0, message = "honeypot must be empty") String honeypot
) {

    /**
     * XSS input sanitization regex — bans {@code < > &} only, preserves VN
     * diacritics + spaces + apostrophes + hyphens + dots + parentheses
     * (legitimate in "Trung tâm Sky Education (Q.1)"). Mirrors
     * {@code BetaRequestDto.NO_HTML_CHARS_REGEX}.
     */
    static final String NO_HTML_CHARS_REGEX = "^[^<>&]+$";

    /** Nullable variant — matches empty/null OR safe chars. */
    static final String NO_HTML_CHARS_REGEX_NULLABLE = "^$|^[^<>&]+$";

    /**
     * Lenient VN phone regex — accepts digits + common separators
     * (space, dot, dash, parentheses, leading +). 8-20 chars covers
     * {@code 0901234567}, {@code 0901 234 567}, {@code +84 90 123 4567},
     * {@code (024) 3826 0000}.
     */
    static final String PHONE_REGEX = "^[0-9+().\\s-]{8,20}$";
}
