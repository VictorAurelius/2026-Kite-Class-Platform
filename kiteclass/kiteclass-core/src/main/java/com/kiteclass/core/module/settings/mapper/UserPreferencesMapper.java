package com.kiteclass.core.module.settings.mapper;

import com.kiteclass.core.module.settings.dto.request.UpdateUserPreferencesRequest;
import com.kiteclass.core.module.settings.dto.response.UserPreferencesResponse;
import com.kiteclass.core.module.settings.entity.UserPreferences;
import com.kiteclass.core.module.settings.enums.Language;
import com.kiteclass.core.module.settings.enums.Theme;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for UserPreferences entity.
 *
 * @since 2.9
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserPreferencesMapper {

    /**
     * Map UserPreferences entity to UserPreferencesResponse DTO.
     *
     * @param userPreferences entity
     * @return response DTO
     */
    @Mapping(target = "language", source = "language", qualifiedByName = "languageToString")
    @Mapping(target = "theme", source = "theme", qualifiedByName = "themeToString")
    UserPreferencesResponse toResponse(UserPreferences userPreferences);

    /**
     * Update UserPreferences entity from UpdateUserPreferencesRequest DTO.
     * Null fields in request are ignored (PATCH semantics).
     *
     * @param request update request DTO
     * @param userPreferences target entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "instanceId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "language", source = "language", qualifiedByName = "stringToLanguage")
    @Mapping(target = "theme", source = "theme", qualifiedByName = "stringToTheme")
    void updateFromRequest(UpdateUserPreferencesRequest request, @MappingTarget UserPreferences userPreferences);

    /**
     * Convert Language enum to String code.
     *
     * @param language enum
     * @return language code
     */
    @Named("languageToString")
    default String languageToString(Language language) {
        return language != null ? language.getCode() : null;
    }

    /**
     * Convert Theme enum to String code.
     *
     * @param theme enum
     * @return theme code
     */
    @Named("themeToString")
    default String themeToString(Theme theme) {
        return theme != null ? theme.getCode() : null;
    }

    /**
     * Convert String code to Language enum.
     *
     * @param code language code
     * @return Language enum
     */
    @Named("stringToLanguage")
    default Language stringToLanguage(String code) {
        return code != null ? Language.fromCode(code) : null;
    }

    /**
     * Convert String code to Theme enum.
     *
     * @param code theme code
     * @return Theme enum
     */
    @Named("stringToTheme")
    default Theme stringToTheme(String code) {
        return code != null ? Theme.fromCode(code) : null;
    }
}
