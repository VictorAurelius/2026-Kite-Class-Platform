package com.kiteclass.core.module.marketing.mapper;

import com.kiteclass.core.module.marketing.dto.request.CreateLeadRequest;
import com.kiteclass.core.module.marketing.dto.request.UpdateLeadRequest;
import com.kiteclass.core.module.marketing.dto.response.LeadResponse;
import com.kiteclass.core.module.marketing.entity.Lead;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Lead entity and DTOs.
 *
 * <p>Provides mappings between:
 * <ul>
 *   <li>Lead entity → LeadResponse DTO</li>
 *   <li>CreateLeadRequest DTO → Lead entity</li>
 *   <li>UpdateLeadRequest DTO → Lead entity (partial update)</li>
 * </ul>
 *
 * @since 2.10
 */
@Mapper(componentModel = "spring")
public interface LeadMapper {

    /**
     * Maps Lead entity to LeadResponse DTO.
     *
     * @param lead the lead entity
     * @return LeadResponse DTO
     */
    LeadResponse toResponse(Lead lead);

    /**
     * Maps CreateLeadRequest DTO to Lead entity.
     *
     * <p>Status defaults to NEW per Lead.Builder.Default annotation.
     * Deleted defaults to false per BaseEntity.
     *
     * @param request the create request DTO
     * @return Lead entity
     */
    @Mapping(target = "status", ignore = true)
    Lead toEntity(CreateLeadRequest request);

    /**
     * Updates existing Lead entity with UpdateLeadRequest DTO.
     *
     * <p>Only updates non-null fields from request (partial update).
     * ID, status, and audit fields are not updated by MapStruct.
     * Status is updated via separate updateLeadStatus method.
     *
     * @param lead    the lead entity to update
     * @param request the update request DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "instanceId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Lead lead, UpdateLeadRequest request);
}
