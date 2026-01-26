package com.kiteclass.gateway.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested entity is not found.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
public class EntityNotFoundException extends BusinessException {

    private static final String CODE = "ENTITY_NOT_FOUND";

    /**
     * Creates exception for entity not found by ID.
     *
     * @param entityName name of the entity type
     * @param id         the ID that was not found
     */
    public EntityNotFoundException(String entityName, Long id) {
        super(CODE, String.format("%s với ID %d không tồn tại", entityName, id), HttpStatus.NOT_FOUND);
    }

    /**
     * Creates exception for entity not found by field.
     *
     * @param entityName name of the entity type
     * @param field      the field used for lookup
     * @param value      the value that was not found
     */
    public EntityNotFoundException(String entityName, String field, String value) {
        super(CODE, String.format("%s với %s '%s' không tồn tại", entityName, field, value), HttpStatus.NOT_FOUND);
    }
}
