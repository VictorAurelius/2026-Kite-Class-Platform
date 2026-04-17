package com.kiteclass.gateway.common.exception;

import com.kiteclass.gateway.common.constant.MessageCodes;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when login credentials are invalid.
 *
 * <p>Uses i18n message code: {@code error.auth.invalid_credentials} - "Email hoặc mật khẩu không đúng"
 *
 * <p>Used for login failures:
 * <ul>
 *   <li>Wrong password</li>
 *   <li>Non-existent email</li>
 *   <li>Inactive account (to prevent user enumeration)</li>
 * </ul>
 *
 * <p>Security note: Error message is intentionally generic to prevent
 * user enumeration attacks (don't reveal if email exists).
 *
 * @author KiteClass Team
 * @since 1.1.0
 */
public class InvalidCredentialsException extends BusinessException {

    /**
     * Creates exception for invalid credentials.
     *
     * @param message descriptive message (not exposed to user)
     */
    public InvalidCredentialsException(String message) {
        super(MessageCodes.AUTH_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
    }
}
