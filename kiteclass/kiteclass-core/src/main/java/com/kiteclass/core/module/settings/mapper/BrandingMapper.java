package com.kiteclass.core.module.settings.mapper;

import com.kiteclass.core.module.settings.dto.request.UpdateBrandingRequest;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import com.kiteclass.core.module.settings.entity.Branding;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Branding entity.
 *
 * @since 2.9
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BrandingMapper {

    /**
     * Map Branding entity to BrandingResponse DTO.
     *
     * @param branding entity
     * @return response DTO
     */
    BrandingResponse toResponse(Branding branding);

    /**
     * Update Branding entity from UpdateBrandingRequest DTO.
     * Null fields in request are ignored (PATCH semantics).
     *
     * @param request update request DTO
     * @param branding target entity to update
     */
    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "instanceId", ignore = true)
    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    @org.mapstruct.Mapping(target = "updatedAt", ignore = true)
    @org.mapstruct.Mapping(target = "createdBy", ignore = true)
    @org.mapstruct.Mapping(target = "updatedBy", ignore = true)
    @org.mapstruct.Mapping(target = "deleted", ignore = true)
    @org.mapstruct.Mapping(target = "version", ignore = true)
    @org.mapstruct.Mapping(target = "faviconUrl", ignore = true)
    @org.mapstruct.Mapping(target = "logoUrl", ignore = true)
    void updateFromRequest(UpdateBrandingRequest request, @MappingTarget Branding branding);
}
