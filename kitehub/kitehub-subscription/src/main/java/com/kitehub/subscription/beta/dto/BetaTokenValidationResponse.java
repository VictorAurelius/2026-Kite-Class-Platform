package com.kitehub.subscription.beta.dto;

/**
 * Response for {@code GET /api/v1/auth/beta-signup/validate?token=XXX} —
 * used by the FE to pre-fill the signup form with the approved request's
 * email + name + persona before tenant-owner password collection.
 *
 * @param valid {@code true} if token matches an APPROVED row that has not expired
 * @param email pre-fill (only present when {@code valid})
 * @param name pre-fill
 * @param orgName pre-fill
 * @param persona pre-fill
 * @param errorCode {@code TOKEN_NOT_FOUND} / {@code TOKEN_EXPIRED} / {@code ALREADY_USED} when {@code !valid}
 *
 * @since Wave 33 — GAP-372
 */
public record BetaTokenValidationResponse(
        boolean valid,
        String email,
        String name,
        String orgName,
        String persona,
        String errorCode
) {
    public static BetaTokenValidationResponse invalid(String errorCode) {
        return new BetaTokenValidationResponse(false, null, null, null, null, errorCode);
    }

    public static BetaTokenValidationResponse ok(String email, String name, String orgName, String persona) {
        return new BetaTokenValidationResponse(true, email, name, orgName, persona, null);
    }
}
