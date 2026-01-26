package com.kiteclass.gateway.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for business logic errors.
 *
 * <p>Uses message codes for i18n support. Messages are resolved
 * via MessageService in GlobalExceptionHandler.
 *
 * <p>Used for:
 * <ul>
 *   <li>Validation failures</li>
 *   <li>Business rule violations</li>
 *   <li>Authentication/Authorization errors</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final Object[] args;

    /**
     * Creates a business exception with code and status.
     *
     * @param code   message code for i18n (e.g., "error.entity.not_found")
     * @param status HTTP status to return
     */
    public BusinessException(String code, HttpStatus status) {
        super(code);
        this.code = code;
        this.status = status;
        this.args = null;
    }

    /**
     * Creates a business exception with code, status, and message args.
     *
     * @param code   message code for i18n (e.g., "error.entity.not_found")
     * @param status HTTP status to return
     * @param args   arguments for message formatting
     */
    public BusinessException(String code, HttpStatus status, Object... args) {
        super(code);
        this.code = code;
        this.status = status;
        this.args = args;
    }

    /**
     * Creates a business exception with code, status, cause, and message args.
     *
     * @param code   message code for i18n
     * @param status HTTP status to return
     * @param cause  underlying cause
     * @param args   arguments for message formatting
     */
    public BusinessException(String code, HttpStatus status, Throwable cause, Object... args) {
        super(code, cause);
        this.code = code;
        this.status = status;
        this.args = args;
    }
}
