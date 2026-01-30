package com.kiteclass.core.module.teacher.mapper;

import com.kiteclass.core.module.teacher.dto.CreateTeacherRequest;
import com.kiteclass.core.module.teacher.dto.TeacherResponse;
import com.kiteclass.core.module.teacher.dto.UpdateTeacherRequest;
import com.kiteclass.core.module.teacher.entity.Teacher;
import org.mapstruct.*;

/**
 * MapStruct mapper for Teacher entity and DTOs.
 *
 * <p>Provides mappings between:
 * <ul>
 *   <li>Teacher entity → TeacherResponse DTO</li>
 *   <li>CreateTeacherRequest DTO → Teacher entity</li>
 *   <li>UpdateTeacherRequest DTO → Teacher entity (partial update)</li>
 * </ul>
 *
 * <p>MapStruct generates implementation at compile time for type-safe mapping.
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@Mapper(componentModel = "spring")
public interface TeacherMapper {

    /**
     * Maps Teacher entity to TeacherResponse DTO.
     *
     * @param teacher the teacher entity
     * @return TeacherResponse DTO
     */
    @Mapping(target = "status", expression = "java(teacher.getStatus().name())")
    TeacherResponse toResponse(Teacher teacher);

    /**
     * Maps CreateTeacherRequest DTO to Teacher entity.
     *
     * <p>Status defaults to ACTIVE per Teacher.Builder.Default annotation.
     * Deleted defaults to false per BaseEntity.
     * AvatarUrl is not set during creation (set to null, updated later via separate endpoint).
     *
     * @param request the create request DTO
     * @return Teacher entity
     */
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "status", ignore = true)
    Teacher toEntity(CreateTeacherRequest request);

    /**
     * Updates existing Teacher entity with UpdateTeacherRequest DTO.
     *
     * <p>Only updates non-null fields from request (partial update).
     * ID and audit fields are not updated by MapStruct.
     * AvatarUrl is not updated via this endpoint (separate endpoint for avatar upload).
     *
     * @param teacher the teacher entity to update
     * @param request the update request DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Teacher teacher, UpdateTeacherRequest request);
}
