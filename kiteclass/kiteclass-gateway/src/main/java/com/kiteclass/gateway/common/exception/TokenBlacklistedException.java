package com.kiteclass.gateway.common.exception;

import com.kiteclass.gateway.common.constant.MessageCodes;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a JWT token has been blacklisted.
 *
 * <p>Uses i18n message code: {@code error.auth.token_blacklisted} - "Token đã bị vô hiệu hóa. Vui lòng đăng nhập lại."
 *
 * <p>Tokens are blacklisted when:
 * <ul>
 *   <li>User logs out</li>
 *   <li>Password is changed</li>
 *   <li>Account is suspended</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.1.0
 */
public class TokenBlacklistedException extends BusinessException {

    /**
     * Creates exception for blacklisted token.
     *
     * @param message descriptive message about why token was blacklisted
     */
    public TokenBlacklistedException(String message) {
        super(MessageCodes.AUTH_TOKEN_BLACKLISTED, HttpStatus.UNAUTHORIZED);
    }
}
