package com.kiteclass.gateway.common.exception;

import com.kiteclass.gateway.common.constant.MessageCodes;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a password does not meet security requirements.
 *
 * <p>Uses i18n message code: {@code error.auth.weak_password}
 * - "Mật khẩu phải có ít nhất {0} ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt"
 *
 * <p>Password policy enforces:
 * <ul>
 *   <li>Minimum 8 characters</li>
 *   <li>At least one uppercase letter</li>
 *   <li>At least one lowercase letter</li>
 *   <li>At least one number</li>
 *   <li>At least one special character</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.1.0
 */
public class WeakPasswordException extends BusinessException {

    /**
     * Creates exception for weak password with minimum length requirement.
     *
     * @param message descriptive message about which requirement failed
     */
    public WeakPasswordException(String message) {
        super(MessageCodes.AUTH_WEAK_PASSWORD, HttpStatus.BAD_REQUEST, 8);
    }
}
