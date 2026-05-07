package com.kitehub.subscription.beta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Beta signup completion payload — submitted by the invitee after clicking
 * the email invite link.
 *
 * @param token invite UUID emitted at approve time (matches {@code beta_access_request.invite_token})
 * @param ownerPassword tenant-owner password (forwarded to the standard registration flow)
 * @param subdomain requested tenant subdomain (forwarded to the standard registration flow)
 *
 * @since Wave 33 — GAP-372
 */
public record BetaSignupCommand(
        @NotNull UUID token,
        @NotBlank @Size(min = 8, max = 200) String ownerPassword,
        @NotBlank @Size(max = 100) String subdomain
) {}
