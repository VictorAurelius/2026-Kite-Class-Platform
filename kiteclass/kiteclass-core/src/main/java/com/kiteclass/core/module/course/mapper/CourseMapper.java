package com.kiteclass.core.module.course.mapper;

import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.course.dto.CourseResponse;
import com.kiteclass.core.module.course.dto.UpdateCourseRequest;
import com.kiteclass.core.module.course.entity.Course;
import org.mapstruct.*;

/**
 * MapStruct mapper for Course entity and DTOs.
 *
 * <p>Provides mappings between:
 * <ul>
 *   <li>Course entity → CourseResponse DTO</li>
 *   <li>CreateCourseRequest DTO → Course entity</li>
 *   <li>UpdateCourseRequest DTO → Course entity (partial update)</li>
 * </ul>
 *
 * <p>MapStruct generates implementation at compile time for type-safe mapping.
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@Mapper(componentModel = "spring")
public interface CourseMapper {

    /**
     * Maps Course entity to CourseResponse DTO.
     *
     * @param course the course entity
     * @return CourseResponse DTO
     */
    @Mapping(target = "status", expression = "java(course.getStatus().name())")
    CourseResponse toResponse(Course course);

    /**
     * Maps CreateCourseRequest DTO to Course entity.
     *
     * <p>Status defaults to DRAFT per Course.Builder.Default annotation.
     * Deleted defaults to false per BaseEntity.
     * CoverImageUrl is not set during creation (set to null, updated later via separate endpoint or update).
     *
     * @param request the create request DTO
     * @return Course entity
     */
    @Mapping(target = "coverImageUrl", ignore = true)
    @Mapping(target = "status", ignore = true)
    Course toEntity(CreateCourseRequest request);

    /**
     * Updates existing Course entity with UpdateCourseRequest DTO.
     *
     * <p>Only updates non-null fields from request (partial update).
     * ID, audit fields, code, teacherId, and status are not updated via this method.
     * Status changes are handled via separate service methods (publish, archive).
     *
     * @param course the course entity to update
     * @param request the update request DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "teacherId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Course course, UpdateCourseRequest request);
}
