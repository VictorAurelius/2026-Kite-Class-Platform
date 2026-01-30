package com.kiteclass.core.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to create a duplicate resource.
 *
 * <p>Returns HTTP 409 CONFLICT status.
 *
 * <p>Usage examples:
 * <ul>
 *   <li>Student with email already exists</li>
 *   <li>Class with code already exists</li>
 *   <li>Duplicate enrollment detected</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
public class DuplicateResourceException extends BusinessException {

    private static final String ERROR_CODE = "DUPLICATE_RESOURCE";

    /**
     * Creates exception for duplicate resource.
     *
     * @param field the field that has a duplicate value (e.g., "email", "code")
     * @param value the duplicate value
     * @deprecated Use {@link #DuplicateResourceException(String, Object...)} with specific error code
     */
    @Deprecated
    public DuplicateResourceException(String field, String value) {
        super(String.format("%s '%s' already exists", field, value), HttpStatus.CONFLICT, ERROR_CODE);
    }

    /**
     * Creates exception for duplicate resource with entity name.
     *
     * @param entityName name of the entity type
     * @param field      the field that has a duplicate value
     * @param value      the duplicate value
     * @deprecated Use {@link #DuplicateResourceException(String, Object...)} with specific error code
     */
    @Deprecated
    public DuplicateResourceException(String entityName, String field, String value) {
        super(String.format("%s with %s '%s' already exists", entityName, field, value), HttpStatus.CONFLICT, ERROR_CODE);
    }

    /**
     * Creates exception with custom message.
     *
     * @param message custom error message
     * @deprecated Use {@link #DuplicateResourceException(String, Object...)} with specific error code
     */
    @Deprecated
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, ERROR_CODE);
    }

    /**
     * Creates exception with specific error code and arguments.
     *
     * <p>This is the recommended constructor for MessageSource integration.
     * Error messages will be resolved from messages.properties files.
     *
     * <p>Example usage:
     * <pre>
     * throw new DuplicateResourceException("STUDENT_EMAIL_EXISTS", "john@example.com");
     * throw new DuplicateResourceException("COURSE_CODE_EXISTS", "MATH101");
     * </pre>
     *
     * @param errorCode specific error code (e.g., "STUDENT_EMAIL_EXISTS", "COURSE_CODE_EXISTS")
     * @param args      arguments for message formatting ({0}, {1}, etc.)
     */
    public DuplicateResourceException(String errorCode, Object... args) {
        super(errorCode, HttpStatus.CONFLICT, args);
    }
}
