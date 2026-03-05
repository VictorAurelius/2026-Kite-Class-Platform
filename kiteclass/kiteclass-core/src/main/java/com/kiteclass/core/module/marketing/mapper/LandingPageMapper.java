package com.kiteclass.core.module.marketing.mapper;

import com.kiteclass.core.module.marketing.dto.request.UpdateLandingPageRequest;
import com.kiteclass.core.module.marketing.dto.response.LandingPageResponse;
import com.kiteclass.core.module.marketing.entity.LandingPage;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for LandingPage entity and DTOs.
 *
 * <p>Provides mappings between:
 * <ul>
 *   <li>LandingPage entity → LandingPageResponse DTO</li>
 *   <li>UpdateLandingPageRequest DTO → LandingPage entity (partial update)</li>
 * </ul>
 *
 * <p>Note: No create mapping needed - landing pages are auto-created with defaults.
 *
 * @since 2.10
 */
@Mapper(componentModel = "spring")
public interface LandingPageMapper {

    /**
     * Maps LandingPage entity to LandingPageResponse DTO.
     *
     * @param landingPage the landing page entity
     * @return LandingPageResponse DTO
     */
    LandingPageResponse toResponse(LandingPage landingPage);

    /**
     * Updates existing LandingPage entity with UpdateLandingPageRequest DTO.
     *
     * <p>Only updates non-null fields from request (partial update).
     * ID and audit fields are not updated by MapStruct.
     *
     * @param landingPage the landing page entity to update
     * @param request     the update request DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "instanceId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget LandingPage landingPage, UpdateLandingPageRequest request);
}
