package com.kitehub.subscription.beta.dto;

/**
 * Error response cho {@code POST /api/v1/auth/beta-signup} khi token invalid /
 * expired / already-used. Mirror pattern của {@link BetaTokenValidationResponse}
 * để FE phân biệt được "application-layer 404" (token invalid) với
 * "infrastructure 404" (route not found, gateway down).
 *
 * <p>Bug class addressed (GAP-611, Wave beta-readiness-5 Bucket D): trước fix,
 * controller trả {@code ResponseEntity.status(HttpStatus.NOT_FOUND).build()}
 * (empty body) → browser console + curl probe không phân biệt được giữa "token
 * invalid" và "endpoint không tồn tại" → confusion + retry waste. Sau fix,
 * response có JSON body với {@code errorCode} cho FE/observability decode.</p>
 *
 * @param errorCode {@code INVALID_TOKEN} / {@code TOKEN_EXPIRED} / {@code WRONG_STATE}
 * @param message Vietnamese narrative cho dev/observability debug
 *
 * @since Wave beta-readiness-5 — GAP-611
 */
public record BetaSignupErrorResponse(
        String errorCode,
        String message
) {
    public static BetaSignupErrorResponse invalidToken() {
        return new BetaSignupErrorResponse(
                "INVALID_TOKEN",
                "Token không hợp lệ hoặc đã được sử dụng. Vui lòng yêu cầu invite mới."
        );
    }

    public static BetaSignupErrorResponse tokenExpired() {
        return new BetaSignupErrorResponse(
                "TOKEN_EXPIRED",
                "Token đã hết hạn. Vui lòng yêu cầu invite mới."
        );
    }

    public static BetaSignupErrorResponse wrongState(String currentState) {
        return new BetaSignupErrorResponse(
                "WRONG_STATE",
                "Token không thể dùng để đăng ký (trạng thái hiện tại: " + currentState + ")."
        );
    }
}
