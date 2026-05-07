package com.kitehub.subscription.beta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Claim-code → invite_token exchange payload (GAP-388 Wave 36 Bucket A 2FA).
 *
 * <p>The signup page submits the 6-digit claim code received by email. The
 * server resolves the corresponding {@code invite_token} UUID and returns it
 * (along with the pre-fill fields). This adds a 2FA gate on top of email
 * delivery — possessing the email alone is not enough.</p>
 *
 * @param claimCode 6-digit numeric code emailed to invitee at approve time
 *
 * @since Wave 36 — GAP-388 Bucket A
 */
public record BetaClaimCodeExchangeCommand(
        @NotBlank
        @Pattern(regexp = "[0-9]{6}", message = "claimCode must be exactly 6 digits")
        String claimCode
) {}
