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
     * @deprecated Use {@link #ValidationException(String, Object...)} with specific error code
     */
    @Deprecated
    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, ERROR_CODE);
    }

    /**
     * Creates exception for invalid field value.
     *
     * @param field the field that failed validation
     * @param value the invalid value
     * @param reason why the value is invalid
     * @deprecated Use {@link #ValidationException(String, Object...)} with specific error code
     */
    @Deprecated
    public ValidationException(String field, String value, String reason) {
        super(String.format("Invalid %s '%s': %s", field, value, reason), HttpStatus.BAD_REQUEST, ERROR_CODE);
    }

    /**
     * Creates exception with cause.
     *
     * @param message validation error message
     * @param cause   underlying cause
     * @deprecated Use {@link #ValidationException(String, Object...)} with specific error code
     */
    @Deprecated
    public ValidationException(String message, Throwable cause) {
        super(ERROR_CODE, HttpStatus.BAD_REQUEST, cause);
    }

    /**
     * Creates exception with specific error code and arguments.
     *
     * <p>This is the recommended constructor for MessageSource integration.
     * Error messages will be resolved from messages.properties files.
     *
     * <p>Example usage:
     * <pre>
     * throw new ValidationException("COURSE_CANNOT_DELETE_STATUS", "PUBLISHED");
     * throw new ValidationException("COURSE_INVALID_UPDATE_ARCHIVED");
     * throw new ValidationException("COURSE_MISSING_REQUIRED_FIELDS", "name, description");
     * </pre>
     *
     * @param errorCode specific error code (e.g., "COURSE_CANNOT_DELETE_STATUS")
     * @param args      arguments for message formatting ({0}, {1}, etc.)
     */
    public ValidationException(String errorCode, Object... args) {
        super(errorCode, HttpStatus.BAD_REQUEST, args);
    }
}
