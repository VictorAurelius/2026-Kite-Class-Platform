package com.kiteclass.core.module.assignment.mapper;

import com.kiteclass.core.module.assignment.dto.request.CreateAssignmentRequest;
import com.kiteclass.core.module.assignment.dto.request.UpdateAssignmentRequest;
import com.kiteclass.core.module.assignment.dto.response.AssignmentResponse;
import com.kiteclass.core.module.assignment.dto.response.SubmissionResponse;
import com.kiteclass.core.module.assignment.entity.Assignment;
import com.kiteclass.core.module.assignment.entity.Submission;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
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
    @Mapping(target = "createdBy", ignore = true)
    Assignment toEntity(CreateAssignmentRequest request);

    /**
     * Update Assignment entity from UpdateAssignmentRequest.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "classId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
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
     */
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(submission.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(submission.getUpdatedAt()))")
    @Mapping(target = "isLate",
            expression = "java(submission.getSubmissionDate() != null "
                    + "&& dueDate != null && submission.isLate(dueDate))")
    @Mapping(target = "penaltyApplied", expression = "java(calculatePenalty(submission))")
    SubmissionResponse toSubmissionResponse(Submission submission, @Context LocalDateTime dueDate);

    /**
     * Map list of Submission entities to list of SubmissionResponse.
     */
    List<SubmissionResponse> toSubmissionResponseList(List<Submission> submissions, @Context LocalDateTime dueDate);

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

    /**
     * Calculate penalty applied to submission.
     */
    default BigDecimal calculatePenalty(Submission submission) {
        if (submission.getScore() == null || submission.getAdjustedScore() == null) {
            return BigDecimal.ZERO;
        }
        return submission.getScore().subtract(submission.getAdjustedScore());
    }
}
