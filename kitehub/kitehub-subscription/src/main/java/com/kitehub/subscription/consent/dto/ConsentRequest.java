package com.kitehub.subscription.consent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload for {@code POST /api/v1/consent/record}.
 *
 * <p>Idempotent by {@code visitorId}: re-posting with the same id updates the
 * existing record in place rather than inserting a duplicate.</p>
 *
 * @since Wave 25 Bucket A — GAP-353b
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRequest {

    /** Pseudonymous visitor identifier — UUID v4 generated client-side. */
    @NotNull
    @JsonProperty("visitorId")
    private UUID visitorId;

    /** Optional — populated after login. */
    @JsonProperty("userId")
    private Long userId;

    /** Optional — marketing-surface visitors pre-tenant. */
    @JsonProperty("tenantId")
    private UUID tenantId;

    /**
     * Always coerced server-side to {@code true} (essential cookies are
     * non-optional per BR-PDPL-CONSENT-001 — banner UX shows them locked-on).
     * Field present so future schema changes (essential split) need no breaking
     * migration.
     */
    @JsonProperty("essentialConsented")
    private Boolean essentialConsented;

    @NotNull
    @JsonProperty("analyticsConsented")
    private Boolean analyticsConsented;

    @NotNull
    @JsonProperty("marketingConsented")
    private Boolean marketingConsented;

    /** Schema version per {@code BR-PDPL-CONSENT-003}. Defaults to 1. */
    @JsonProperty("consentVersion")
    private Integer consentVersion;

    /** Optional — populated by Spring controller from {@code X-Forwarded-For} when present. */
    @Size(max = 45)
    @JsonProperty("ipAddress")
    private String ipAddress;

    /** Optional — populated by Spring controller from {@code User-Agent} header. */
    @Size(max = 4096)
    @JsonProperty("userAgent")
    private String userAgent;
}
