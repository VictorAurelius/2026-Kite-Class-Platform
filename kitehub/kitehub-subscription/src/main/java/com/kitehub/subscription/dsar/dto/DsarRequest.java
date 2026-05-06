package com.kitehub.subscription.dsar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kitehub.subscription.dsar.entity.DsarRightType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for {@code POST /api/v1/dsar/request}.
 *
 * <p>Public, unauthenticated endpoint — DSAR submitters are not logged-in users
 * by definition. Identity verification happens out-of-band via the
 * {@code nationalIdLast4} + DPO callback.</p>
 *
 * @since Wave 26 Bucket A — GAP-353c
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DsarRequest {

    @NotNull
    @JsonProperty("rightType")
    private DsarRightType rightType;

    @NotBlank
    @Email
    @Size(max = 320)
    @JsonProperty("requesterEmail")
    private String requesterEmail;

    @NotBlank
    @Size(max = 200)
    @JsonProperty("requesterName")
    private String requesterName;

    /** Last 4 digits of CMND/CCCD — exactly 4 numeric characters per BR-PDPL-DSAR-003. */
    @NotBlank
    @Pattern(regexp = "^[0-9]{4}$", message = "national_id_last4 must be 4 digits")
    @JsonProperty("nationalIdLast4")
    private String nationalIdLast4;

    @Size(max = 4000)
    @JsonProperty("scope")
    private String scope;

    @Size(max = 4000)
    @JsonProperty("reason")
    private String reason;

    /** Optional contact-method preference — free text, validated server-side only. */
    @Size(max = 50)
    @JsonProperty("contactPreference")
    private String contactPreference;

    /**
     * Honeypot anti-spam field. Front-end keeps it hidden from users; bots that
     * blindly fill all fields populate it. Server rejects when non-empty per
     * BR-PDPL-DSAR-005.
     */
    @Size(max = 500)
    @JsonProperty("companyWebsite")
    private String companyWebsite;
}
