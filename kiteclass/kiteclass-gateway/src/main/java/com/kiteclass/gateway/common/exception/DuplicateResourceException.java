package com.kiteclass.gateway.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to create a duplicate resource.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
public class DuplicateResourceException extends BusinessException {

    private static final String CODE = "DUPLICATE_RESOURCE";

    /**
     * Creates exception for duplicate resource.
     *
     * @param field the field that has a duplicate value
     * @param value the duplicate value
     */
    public DuplicateResourceException(String field, String value) {
        super(CODE, String.format("%s '%s' đã tồn tại", field, value), HttpStatus.CONFLICT);
    }

    /**
     * Creates exception with custom message.
     *
     * @param message custom error message
     */
    public DuplicateResourceException(String message) {
        super(CODE, message, HttpStatus.CONFLICT);
    }
}
