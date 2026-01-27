package com.kiteclass.core.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested entity is not found.
 *
 * <p>Returns HTTP 404 NOT_FOUND status.
 *
 * <p>Usage examples:
 * <ul>
 *   <li>Student with ID not found</li>
 *   <li>Class with ID not found</li>
 *   <li>Entity with specific field value not found</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
public class EntityNotFoundException extends BusinessException {

    private static final String ERROR_CODE = "ENTITY_NOT_FOUND";

    /**
     * Creates exception for entity not found by ID.
     *
     * @param entityName name of the entity type (e.g., "Student", "Class")
     * @param id         the ID that was not found
     */
    public EntityNotFoundException(String entityName, Long id) {
        super(String.format("%s with ID %d not found", entityName, id), HttpStatus.NOT_FOUND, ERROR_CODE);
    }

    /**
     * Creates exception for entity not found by field.
     *
     * @param entityName name of the entity type
     * @param field      the field used for lookup (e.g., "email", "code")
     * @param value      the value that was not found
     */
    public EntityNotFoundException(String entityName, String field, String value) {
        super(String.format("%s with %s '%s' not found", entityName, field, value), HttpStatus.NOT_FOUND, ERROR_CODE);
    }

    /**
     * Creates exception with custom message.
     *
     * @param message custom error message
     */
    public EntityNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, ERROR_CODE);
    }
}
