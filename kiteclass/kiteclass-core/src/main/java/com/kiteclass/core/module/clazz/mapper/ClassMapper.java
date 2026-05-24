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
 * <p>Note: BaseEntity fields (id, instanceId, createdAt, updatedAt, createdBy,
 * updatedBy, deleted, version) are NOT listed in @Mapping(ignore=true) because
 * they are not exposed via the Lombok @Builder and MapStruct cannot target them.
 * These fields are set by EntityPersistenceListener and JPA auditing.
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
@Mapper(componentModel = "spring")
public interface ClassMapper {

    /**
     * Maps CreateClassRequest to Class entity.
     *
     * <p>Only Class-own fields are mapped; BaseEntity fields are excluded from
     * the builder by Lombok and handled by EntityPersistenceListener / JPA.
     *
     * @param request the creation request
     * @return Class entity (not yet persisted)
     */
    @Mapping(target = "courseId", ignore = true)
    @Mapping(target = "teacherId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentEnrolled", ignore = true)
    @Mapping(target = "classCode", ignore = true)
    @Mapping(target = "codeExpiresAt", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "recurrenceRule", ignore = true)
    // Wave br-4 Bucket D V68 reschedule audit columns — set by ClassService.reschedule(), not via create mapper
    @Mapping(target = "rescheduledByUserId", ignore = true)
    @Mapping(target = "rescheduledAt", ignore = true)
    @Mapping(target = "previousStartDate", ignore = true)
    @Mapping(target = "previousEndDate", ignore = true)
    @Mapping(target = "rescheduleReasonCategory", ignore = true)
    @Mapping(target = "rescheduleReasonNotes", ignore = true)
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
