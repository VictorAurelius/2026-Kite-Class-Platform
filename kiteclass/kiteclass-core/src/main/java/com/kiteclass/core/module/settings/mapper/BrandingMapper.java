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
    void updateFromRequest(UpdateBrandingRequest request, @MappingTarget Branding branding);
}
