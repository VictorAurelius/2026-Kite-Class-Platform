package com.kiteclass.core.module.clazz.mapper;

import com.kiteclass.core.module.clazz.dto.ClassResponse;
import com.kiteclass.core.module.clazz.dto.ClassSessionResponse;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.entity.ClassSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Class and ClassSession entities.
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
@Mapper(componentModel = "spring")
public interface ClassMapper {

    /**
     * Maps CreateClassRequest to Class entity.
     * courseId and status are set by the service layer.
     *
     * @param request the creation request
     * @return Class entity (not yet persisted)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "courseId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentEnrolled", ignore = true)
    @Mapping(target = "classCode", ignore = true)
    @Mapping(target = "codeExpiresAt", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "instanceId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    Class toEntity(CreateClassRequest request);

    /**
     * Maps Class entity to ClassResponse DTO.
     *
     * @param entity the class entity
     * @return ClassResponse DTO
     */
    ClassResponse toResponse(Class entity);

    /**
     * Maps ClassSession entity to ClassSessionResponse DTO.
     *
     * @param entity the session entity
     * @return ClassSessionResponse DTO
     */
    ClassSessionResponse toSessionResponse(ClassSession entity);
}
