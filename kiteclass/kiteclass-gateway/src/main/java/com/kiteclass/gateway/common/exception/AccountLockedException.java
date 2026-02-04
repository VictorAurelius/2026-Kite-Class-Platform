package com.kiteclass.gateway.common.exception;

import com.kiteclass.gateway.common.constant.MessageCodes;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an account is locked due to failed login attempts.
 *
 * <p>Uses i18n message code: {@code error.auth.account_locked}
 * - "Tài khoản đã bị khóa do {0} lần đăng nhập thất bại. Vui lòng thử lại sau {1} phút."
 *
 * <p>Account lockout mechanism:
 * <ul>
 *   <li>Locks account after 5 failed login attempts</li>
 *   <li>Automatically unlocks after 15 minutes</li>
 *   <li>Resets counter on successful login</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.1.0
 */
public class AccountLockedException extends BusinessException {

    /**
     * Creates exception for locked account.
     *
     * @param message descriptive message about the lockout
     */
    public AccountLockedException(String message) {
        super(MessageCodes.AUTH_ACCOUNT_LOCKED, HttpStatus.FORBIDDEN, 5, 15);
    }
}
