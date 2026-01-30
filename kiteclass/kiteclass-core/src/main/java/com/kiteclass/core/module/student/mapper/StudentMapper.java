package com.kiteclass.core.module.student.mapper;

import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.dto.StudentResponse;
import com.kiteclass.core.module.student.dto.UpdateStudentRequest;
import com.kiteclass.core.module.student.entity.Student;
import org.mapstruct.*;

/**
 * MapStruct mapper for Student entity and DTOs.
 *
 * <p>Provides mappings between:
 * <ul>
 *   <li>Student entity → StudentResponse DTO</li>
 *   <li>CreateStudentRequest DTO → Student entity</li>
 *   <li>UpdateStudentRequest DTO → Student entity (partial update)</li>
 * </ul>
 *
 * <p>MapStruct generates implementation at compile time for type-safe mapping.
 *
 * @author KiteClass Team
 * @since 2.3.0
 */
@Mapper(componentModel = "spring")
public interface StudentMapper {

    /**
     * Maps Student entity to StudentResponse DTO.
     *
     * @param student the student entity
     * @return StudentResponse DTO
     */
    StudentResponse toResponse(Student student);

    /**
     * Maps CreateStudentRequest DTO to Student entity.
     *
     * <p>Status defaults to ACTIVE per Student.Builder.Default annotation.
     * Deleted defaults to false per BaseEntity.
     * AvatarUrl is not set during creation (set to null, updated later via separate endpoint).
     *
     * @param request the create request DTO
     * @return Student entity
     */
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "status", ignore = true)
    Student toEntity(CreateStudentRequest request);

    /**
     * Updates existing Student entity with UpdateStudentRequest DTO.
     *
     * <p>Only updates non-null fields from request (partial update).
     * ID and audit fields are not updated by MapStruct.
     * AvatarUrl is not updated via this endpoint (separate endpoint for avatar upload).
     *
     * @param student the student entity to update
     * @param request the update request DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Student student, UpdateStudentRequest request);
}
