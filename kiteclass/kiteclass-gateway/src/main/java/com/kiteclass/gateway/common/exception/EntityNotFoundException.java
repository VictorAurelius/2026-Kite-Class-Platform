package com.kiteclass.gateway.common.exception;

import com.kiteclass.gateway.common.constant.MessageCodes;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested entity is not found.
 *
 * <p>Uses i18n message codes:
 * <ul>
 *   <li>{@code error.entity.not_found} - for ID lookup: "{0} với ID {1} không tồn tại"</li>
 *   <li>{@code error.entity.not_found.field} - for field lookup: "{0} với {1} '{2}' không tồn tại"</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
public class EntityNotFoundException extends BusinessException {

    /**
     * Creates exception for entity not found by ID.
     *
     * @param entityName name of the entity type
     * @param id         the ID that was not found
     */
    public EntityNotFoundException(String entityName, Long id) {
        super(MessageCodes.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND, entityName, id);
    }

    /**
     * Creates exception for entity not found by field.
     *
     * @param entityName name of the entity type
     * @param field      the field used for lookup
     * @param value      the value that was not found
     */
    public EntityNotFoundException(String entityName, String field, String value) {
        super(MessageCodes.ENTITY_NOT_FOUND_FIELD, HttpStatus.NOT_FOUND, entityName, field, value);
    }
}
