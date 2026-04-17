package com.kiteclass.core.module.marketing.mapper;

import com.kiteclass.core.module.marketing.dto.request.CreateContactMessageRequest;
import com.kiteclass.core.module.marketing.dto.request.UpdateContactMessageRequest;
import com.kiteclass.core.module.marketing.dto.response.ContactMessageResponse;
import com.kiteclass.core.module.marketing.entity.ContactMessage;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for ContactMessage entity and DTOs.
 *
 * <p>Provides mappings between:
 * <ul>
 *   <li>ContactMessage entity → ContactMessageResponse DTO</li>
 *   <li>CreateContactMessageRequest DTO → ContactMessage entity</li>
 *   <li>UpdateContactMessageRequest DTO → ContactMessage entity (partial update)</li>
 * </ul>
 *
 * @since 2.10
 */
@Mapper(componentModel = "spring")
public interface ContactMessageMapper {

    /**
     * Maps ContactMessage entity to ContactMessageResponse DTO.
     *
     * @param contactMessage the contact message entity
     * @return ContactMessageResponse DTO
     */
    ContactMessageResponse toResponse(ContactMessage contactMessage);

    /**
     * Maps CreateContactMessageRequest DTO to ContactMessage entity.
     *
     * <p>isRead defaults to false per ContactMessage.Builder.Default annotation.
     * Deleted defaults to false per BaseEntity.
     *
     * @param request the create request DTO
     * @return ContactMessage entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "instanceId", ignore = true)
    @Mapping(target = "isRead", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    @Mapping(target = "replied", ignore = true)
    @Mapping(target = "repliedAt", ignore = true)
    @Mapping(target = "replyMessage", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    ContactMessage toEntity(CreateContactMessageRequest request);

    /**
     * Updates existing ContactMessage entity with UpdateContactMessageRequest DTO.
     *
     * <p>Only updates non-null fields from request (partial update).
     * ID, read status, and audit fields are not updated by MapStruct.
     * Read status is updated via separate markAsRead method.
     *
     * @param contactMessage the contact message entity to update
     * @param request        the update request DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "instanceId", ignore = true)
    @Mapping(target = "isRead", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "replied", ignore = true)
    @Mapping(target = "repliedAt", ignore = true)
    @Mapping(target = "replyMessage", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget ContactMessage contactMessage, UpdateContactMessageRequest request);
}
