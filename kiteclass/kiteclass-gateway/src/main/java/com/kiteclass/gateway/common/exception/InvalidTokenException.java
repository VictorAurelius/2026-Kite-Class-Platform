package com.kiteclass.gateway.common.exception;

import com.kiteclass.gateway.common.constant.MessageCodes;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a JWT token is invalid or malformed.
 *
 * <p>Uses i18n message code: {@code error.auth.token_invalid} - "Token không hợp lệ"
 *
 * <p>Used in JWT validation for:
 * <ul>
 *   <li>Invalid signature</li>
 *   <li>Malformed token structure</li>
 *   <li>Missing required claims</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.1.0
 */
public class InvalidTokenException extends BusinessException {

    /**
     * Creates exception for invalid token.
     *
     * @param message descriptive message about the validation failure
     */
    public InvalidTokenException(String message) {
        super(MessageCodes.AUTH_TOKEN_INVALID, HttpStatus.UNAUTHORIZED);
    }
}
