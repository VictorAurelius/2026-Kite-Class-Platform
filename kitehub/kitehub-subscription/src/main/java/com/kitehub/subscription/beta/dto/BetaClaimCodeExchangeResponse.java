package com.kitehub.subscription.beta.dto;

import java.util.UUID;

/**
 * Response from {@code POST /api/v1/auth/beta-signup/exchange-claim-code}.
 *
 * <p>On success: returns the resolved {@code inviteToken} UUID + pre-fill data
 * (mirrors {@link BetaTokenValidationResponse}). On failure: {@code valid=false}
 * with an {@code errorCode} (CODE_NOT_FOUND / CODE_EXPIRED / ALREADY_USED).</p>
 *
 * @since Wave 36 — GAP-388 Bucket A
 */
public record BetaClaimCodeExchangeResponse(
        boolean valid,
        UUID inviteToken,
        String email,
        String name,
        String orgName,
        String persona,
        String errorCode
) {
    public static BetaClaimCodeExchangeResponse ok(UUID token, String email, String name,
                                                    String orgName, String persona) {
        return new BetaClaimCodeExchangeResponse(true, token, email, name, orgName, persona, null);
    }

    public static BetaClaimCodeExchangeResponse invalid(String errorCode) {
        return new BetaClaimCodeExchangeResponse(false, null, null, null, null, null, errorCode);
    }
}
