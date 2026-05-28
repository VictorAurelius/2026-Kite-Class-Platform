package com.kiteclass.core.module.assignment.mapper;

import com.kiteclass.core.module.assignment.dto.request.CreateAssignmentRequest;
import com.kiteclass.core.module.assignment.dto.request.UpdateAssignmentRequest;
import com.kiteclass.core.module.assignment.dto.response.AssignmentResponse;
import com.kiteclass.core.module.assignment.dto.response.SubmissionResponse;
import com.kiteclass.core.module.assignment.entity.Assignment;
import com.kiteclass.core.module.assignment.entity.Submission;
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
 * Mapper for Assignment and Submission entities.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
@Mapper(componentModel = "spring")
public interface AssignmentMapper {

    /**
     * Map CreateAssignmentRequest to Assignment entity.
     */
    @Mapping(target = "status", constant = "DRAFT")
    Assignment toEntity(CreateAssignmentRequest request);

    /**
     * Update Assignment entity from UpdateAssignmentRequest.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "classId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "instanceId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdateAssignmentRequest request, @MappingTarget Assignment assignment);

    /**
     * Map Assignment entity to AssignmentResponse.
     */
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(assignment.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(assignment.getUpdatedAt()))")
    @Mapping(target = "isOverdue", expression = "java(isOverdue(assignment))")
    @Mapping(target = "isAcceptingSubmissions", expression = "java(assignment.isAcceptingSubmissions())")
    AssignmentResponse toResponse(Assignment assignment);

    /**
     * Map list of Assignment entities to list of AssignmentResponse.
     */
    List<AssignmentResponse> toResponseList(List<Assignment> assignments);

    /**
     * Map Submission entity to SubmissionResponse.
     * Note: isLate and penaltyApplied are computed in service layer.
     */
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(submission.getCreatedAt()))")
    @Mapping(target = "isLate", ignore = true)
    @Mapping(target = "penaltyApplied", ignore = true)
    SubmissionResponse toSubmissionResponse(Submission submission);

    /**
     * Map list of Submission entities to list of SubmissionResponse.
     */
    List<SubmissionResponse> toSubmissionResponseList(List<Submission> submissions);

    /**
     * Convert Instant to LocalDateTime.
     *
     * @param instant the instant to convert
     * @return LocalDateTime in system default timezone
     */
    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    /**
     * Check if assignment is overdue.
     */
    default boolean isOverdue(Assignment assignment) {
        return assignment.getDueDate() != null &&
               LocalDateTime.now().isAfter(assignment.getDueDate());
    }
}
