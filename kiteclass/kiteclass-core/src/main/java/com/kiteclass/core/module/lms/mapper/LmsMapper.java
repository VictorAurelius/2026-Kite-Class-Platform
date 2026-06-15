package com.kiteclass.core.module.lms.mapper;

import com.kiteclass.core.module.lms.dto.request.CreateCourseModuleRequest;
import com.kiteclass.core.module.lms.dto.request.CreateLearningResourceRequest;
import com.kiteclass.core.module.lms.dto.request.CreateLessonRequest;
import com.kiteclass.core.module.lms.dto.request.UpdateCourseModuleRequest;
import com.kiteclass.core.module.lms.dto.request.UpdateLessonRequest;
import com.kiteclass.core.module.lms.dto.response.CourseModuleResponse;
import com.kiteclass.core.module.lms.dto.response.LearningResourceResponse;
import com.kiteclass.core.module.lms.dto.response.LessonProgressResponse;
import com.kiteclass.core.module.lms.dto.response.LessonResponse;
import com.kiteclass.core.module.lms.entity.CourseModule;
import com.kiteclass.core.module.lms.entity.LearningResource;
import com.kiteclass.core.module.lms.entity.Lesson;
import com.kiteclass.core.module.lms.entity.LessonProgress;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * MapStruct mapper for LMS entities and DTOs.
 *
 * <p>Provides mappings between:
 * <ul>
 *   <li>CourseModule entity ↔ CourseModule DTOs</li>
 *   <li>Lesson entity ↔ Lesson DTOs</li>
 *   <li>LearningResource entity ↔ LearningResource DTOs</li>
 *   <li>LessonProgress entity ↔ LessonProgress DTOs</li>
 * </ul>
 *
 * <p>MapStruct generates implementation at compile time for type-safe mapping.
 *
 * @author KiteClass Team
 * @since 2.9.0
 */
@Mapper(componentModel = "spring")
public interface LmsMapper {

    // ==================== CourseModule Mappings ====================

    /**
     * Maps CourseModule entity to CourseModuleResponse DTO.
     *
     * @param entity the course module entity
     * @return CourseModuleResponse DTO
     */
    CourseModuleResponse toModuleResponse(CourseModule entity);

    /**
     * Maps list of CourseModule entities to list of CourseModuleResponse DTOs.
     *
     * @param entities list of course module entities
     * @return list of CourseModuleResponse DTOs
     */
    List<CourseModuleResponse> toModuleResponseList(List<CourseModule> entities);

    /**
     * Maps CreateCourseModuleRequest DTO to CourseModule entity.
     *
     * @param request the create request DTO
     * @return CourseModule entity
     */
    @Mapping(target = "courseId", ignore = true)  // Set by service layer
    CourseModule toModuleEntity(CreateCourseModuleRequest request);

    /**
     * Updates existing CourseModule entity with UpdateCourseModuleRequest DTO.
     * Only updates non-null fields from request (partial update).
     *
     * @param entity the course module entity to update
     * @param request the update request DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "courseId", ignore = true)
    @Mapping(target = "instanceId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateModuleEntity(@MappingTarget CourseModule entity, UpdateCourseModuleRequest request);

    // ==================== Lesson Mappings ====================

    /**
     * Maps Lesson entity to LessonResponse DTO.
     *
     * @param entity the lesson entity
     * @return LessonResponse DTO
     */
    LessonResponse toLessonResponse(Lesson entity);

    /**
     * Maps list of Lesson entities to list of LessonResponse DTOs.
     *
     * @param entities list of lesson entities
     * @return list of LessonResponse DTOs
     */
    List<LessonResponse> toLessonResponseList(List<Lesson> entities);

    /**
     * Maps CreateLessonRequest DTO to Lesson entity.
     *
     * @param request the create request DTO
     * @return Lesson entity
     */
    @Mapping(target = "moduleId", ignore = true)  // Set by service layer
    @Mapping(target = "isTrial", defaultValue = "false")  // Default to false if not provided
    Lesson toLessonEntity(CreateLessonRequest request);

    /**
     * Updates existing Lesson entity with UpdateLessonRequest DTO.
     * Only updates non-null fields from request (partial update).
     *
     * @param entity the lesson entity to update
     * @param request the update request DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "moduleId", ignore = true)
    @Mapping(target = "instanceId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateLessonEntity(@MappingTarget Lesson entity, UpdateLessonRequest request);

    // ==================== LearningResource Mappings ====================

    /**
     * Maps LearningResource entity to LearningResourceResponse DTO.
     *
     * @param entity the learning resource entity
     * @return LearningResourceResponse DTO
     */
    LearningResourceResponse toResourceResponse(LearningResource entity);

    /**
     * Maps list of LearningResource entities to list of LearningResourceResponse DTOs.
     *
     * @param entities list of learning resource entities
     * @return list of LearningResourceResponse DTOs
     */
    List<LearningResourceResponse> toResourceResponseList(List<LearningResource> entities);

    /**
     * Maps CreateLearningResourceRequest DTO to LearningResource entity.
     *
     * @param request the create request DTO
     * @return LearningResource entity
     */
    @Mapping(target = "lessonId", ignore = true)        // Set by service layer
    @Mapping(target = "uploadedFileId", ignore = true)  // Set by service layer on upload (GAP-1405)
    LearningResource toResourceEntity(CreateLearningResourceRequest request);

    // ==================== LessonProgress Mappings ====================

    /**
     * Maps LessonProgress entity to LessonProgressResponse DTO.
     *
     * @param entity the lesson progress entity
     * @return LessonProgressResponse DTO
     */
    LessonProgressResponse toProgressResponse(LessonProgress entity);

    /**
     * Maps list of LessonProgress entities to list of LessonProgressResponse DTOs.
     *
     * @param entities list of lesson progress entities
     * @return list of LessonProgressResponse DTOs
     */
    List<LessonProgressResponse> toProgressResponseList(List<LessonProgress> entities);

    // ==================== Helper Methods ====================

    /**
     * Converts Instant to LocalDateTime for DTO mapping.
     * Uses system default timezone for conversion.
     *
     * @param instant the Instant value to convert
     * @return LocalDateTime in system default timezone, or null if input is null
     */
    default LocalDateTime map(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
