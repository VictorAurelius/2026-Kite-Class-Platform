package com.kiteclass.gateway.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for business logic errors.
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

    /**
     * Creates a business exception with code and status.
     *
     * @param code   error code for client handling
     * @param status HTTP status to return
     */
    public BusinessException(String code, HttpStatus status) {
        super(code);
        this.code = code;
        this.status = status;
    }

    /**
     * Creates a business exception with code, message, and status.
     *
     * @param code    error code for client handling
     * @param message human-readable error message
     * @param status  HTTP status to return
     */
    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    /**
     * Creates a business exception with code, message, status, and cause.
     *
     * @param code    error code for client handling
     * @param message human-readable error message
     * @param status  HTTP status to return
     * @param cause   underlying cause
     */
    public BusinessException(String code, String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }
}
