package com.kiteclass.core.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for business logic errors.
 *
 * <p>Used for:
 * <ul>
 *   <li>Validation failures</li>
 *   <li>Business rule violations</li>
 *   <li>Resource not found errors</li>
 *   <li>Duplicate resource errors</li>
 * </ul>
 *
 * <p>All business exceptions include:
 * <ul>
 *   <li>Error code for programmatic handling</li>
 *   <li>HTTP status to return</li>
 *   <li>Optional message arguments for formatting</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final Object[] args;

    /**
     * Creates a business exception with code and status.
     *
     * @param code   error code
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
     * @param code   error code
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
     * Creates a business exception with message.
     *
     * @param message error message
     * @param status  HTTP status to return
     */
    public BusinessException(String message, HttpStatus status, String code) {
        super(message);
        this.code = code;
        this.status = status;
        this.args = null;
    }

    /**
     * Creates a business exception with code, status, cause, and message args.
     *
     * @param code   error code
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
