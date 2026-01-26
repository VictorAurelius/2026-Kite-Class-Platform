package com.kiteclass.gateway.common.exception;

import com.kiteclass.gateway.common.constant.MessageCodes;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to create a duplicate resource.
 *
 * <p>Uses i18n message code: {@code error.entity.duplicate} - "{0} '{1}' đã tồn tại"
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
public class DuplicateResourceException extends BusinessException {

    /**
     * Creates exception for duplicate resource.
     *
     * @param field the field that has a duplicate value
     * @param value the duplicate value
     */
    public DuplicateResourceException(String field, String value) {
        super(MessageCodes.ENTITY_DUPLICATE, HttpStatus.CONFLICT, field, value);
    }
}
