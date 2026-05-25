package com.kiteclass.core.module.course.mapper;

import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.course.dto.CourseResponse;
import com.kiteclass.core.module.course.dto.PrerequisiteCourseDTO;
import com.kiteclass.core.module.course.dto.UpdateCourseRequest;
import com.kiteclass.core.module.course.entity.Course;
import org.mapstruct.*;

import java.util.Comparator;
import java.util.List;

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
     * <p>Custom implementation to:
     * <ul>
     *   <li>Convert status enum to string</li>
     *   <li>Map prerequisite courses to PrerequisiteCourseDTO list (sorted by name)</li>
     * </ul>
     *
     * @param course the course entity
     * @return CourseResponse DTO
     */
    default CourseResponse toResponse(Course course) {
        if (course == null) {
            return null;
        }

        // Map prerequisite courses to DTO list, sorted by name
        List<PrerequisiteCourseDTO> prerequisiteCourses = course.getPrerequisiteCourses().stream()
            .map(prereq -> new PrerequisiteCourseDTO(
                prereq.getId(),
                prereq.getName(),
                prereq.getCode()
            ))
            .sorted(Comparator.comparing(PrerequisiteCourseDTO::name))
            .toList();

        return new CourseResponse(
            course.getId(),
            course.getName(),
            course.getCode(),
            course.getDescription(),
            course.getSyllabus(),
            course.getObjectives(),
            course.getPrerequisites(),
            prerequisiteCourses,
            course.getTargetAudience(),
            course.getTeacherId(),
            course.getDurationWeeks(),
            course.getTotalSessions(),
            course.getPrice(),
            course.getPricingModel() != null ? course.getPricingModel().name() : null,
            course.getUnitPrice(),
            course.getStatus() != null ? course.getStatus().name() : null,
            course.getCoverImageUrl(),
            course.getLevel(),
            course.getCategory(),
            course.getCreatedAt(),
            course.getUpdatedAt()
        );
    }

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
    @Mapping(target = "prerequisiteCourses", ignore = true)
    @Mapping(target = "dependentCourses", ignore = true)
    // Pricing fields (Wave br-4 / ADR-035) set via entity defaults (PER_HOUR + ZERO)
    // and mutated via separate pricing endpoint per BR-COURSE-PRICING-001..004.
    @Mapping(target = "pricingModel", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    Course toEntity(CreateCourseRequest request);

    /**
     * Updates existing Course entity with UpdateCourseRequest DTO.
     *
     * <p>Only updates non-null fields from request (partial update).
     * ID, audit fields, and status are not updated via this method.
     * Status changes are handled via separate service methods (publish, archive).
     *
     * <p>Note: Code and teacherId can be updated for DRAFT courses only.
     * Service layer validates update restrictions based on course status.
     *
     * @param course the course entity to update
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
    @Mapping(target = "prerequisiteCourses", ignore = true)
    @Mapping(target = "dependentCourses", ignore = true)
    // Pricing fields (Wave br-4 / ADR-035) mutated via separate pricing endpoint
    // per BR-COURSE-PRICING-001..004 — not exposed in UpdateCourseRequest.
    @Mapping(target = "pricingModel", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Course course, UpdateCourseRequest request);
}
