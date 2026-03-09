package com.kiteclass.gateway.common.exception;

import com.kiteclass.gateway.common.constant.MessageCodes;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to reuse a refresh token.
 *
 * <p>Uses i18n message code: {@code error.auth.refresh_token_used}
 * - "Refresh token đã được sử dụng. Vui lòng đăng nhập lại."
 *
 * <p>Implements refresh token rotation security:
 * <ul>
 *   <li>Each refresh token can only be used once</li>
 *   <li>After use, a new refresh token is issued</li>
 *   <li>Reuse detection indicates potential token theft</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.1.0
 */
public class RefreshTokenUsedException extends BusinessException {

    /**
     * Creates exception for already-used refresh token.
     *
     * @param message descriptive message about the reuse attempt
     */
    public RefreshTokenUsedException(String message) {
        super(MessageCodes.AUTH_REFRESH_TOKEN_USED, HttpStatus.UNAUTHORIZED);
    }
}
