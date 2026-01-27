package com.kiteclass.core.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when validation fails.
 *
 * <p>Returns HTTP 400 BAD_REQUEST status.
 *
 * <p>Usage examples:
 * <ul>
 *   <li>Invalid input data</li>
 *   <li>Business rule validation failures</li>
 *   <li>Constraint violations</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
public class ValidationException extends BusinessException {

    private static final String ERROR_CODE = "VALIDATION_ERROR";

    /**
     * Creates exception with validation error message.
     *
     * @param message validation error message
     */
    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, ERROR_CODE);
    }

    /**
     * Creates exception for invalid field value.
     *
     * @param field the field that failed validation
     * @param value the invalid value
     * @param reason why the value is invalid
     */
    public ValidationException(String field, String value, String reason) {
        super(String.format("Invalid %s '%s': %s", field, value, reason), HttpStatus.BAD_REQUEST, ERROR_CODE);
    }

    /**
     * Creates exception with cause.
     *
     * @param message validation error message
     * @param cause   underlying cause
     */
    public ValidationException(String message, Throwable cause) {
        super(ERROR_CODE, HttpStatus.BAD_REQUEST, cause);
    }
}
