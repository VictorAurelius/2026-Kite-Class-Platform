package com.kiteclass.core.module.tenantsettings.mapper;

import com.kiteclass.core.module.tenantsettings.dto.request.UpdateTenantSettingsRequest;
import com.kiteclass.core.module.tenantsettings.dto.response.TenantSettingsResponse;
import com.kiteclass.core.module.tenantsettings.entity.SchoolType;
import com.kiteclass.core.module.tenantsettings.entity.TenantSettings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for {@link TenantSettings}.
 *
 * @since Wave provisioning-1 (GAP-947)
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TenantSettingsMapper {

    /**
     * Map entity to response DTO.
     *
     * @param settings entity
     * @return response DTO
     */
    @Mapping(target = "schoolType", source = "schoolType", qualifiedByName = "schoolTypeToString")
    TenantSettingsResponse toResponse(TenantSettings settings);

    /**
     * Update entity from request DTO (PATCH semantics — null fields ignored).
     *
     * @param request update request
     * @param settings target entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "instanceId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "schoolType", source = "schoolType", qualifiedByName = "stringToSchoolType")
    void updateFromRequest(UpdateTenantSettingsRequest request, @MappingTarget TenantSettings settings);

    /**
     * Convert SchoolType enum to its string name.
     *
     * @param schoolType enum
     * @return enum name or null
     */
    @Named("schoolTypeToString")
    default String schoolTypeToString(SchoolType schoolType) {
        return schoolType != null ? schoolType.name() : null;
    }

    /**
     * Convert string to SchoolType enum (null-safe — null leaves existing value via IGNORE strategy).
     *
     * @param value enum name
     * @return SchoolType enum or null
     */
    @Named("stringToSchoolType")
    default SchoolType stringToSchoolType(String value) {
        return value != null ? SchoolType.valueOf(value) : null;
    }
}
