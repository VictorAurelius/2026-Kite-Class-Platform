package com.kiteclass.gateway.common.exception;

import com.kiteclass.gateway.common.constant.MessageCodes;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a JWT token has expired.
 *
 * <p>Uses i18n message code: {@code error.auth.token_expired} - "Phiên đăng nhập đã hết hạn"
 *
 * <p>Used in JWT validation to reject expired access tokens.
 *
 * @author KiteClass Team
 * @since 1.1.0
 */
public class TokenExpiredException extends BusinessException {

    /**
     * Creates exception for expired token.
     *
     * @param message descriptive message about the expiration
     */
    public TokenExpiredException(String message) {
        super(MessageCodes.AUTH_TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED);
    }
}
