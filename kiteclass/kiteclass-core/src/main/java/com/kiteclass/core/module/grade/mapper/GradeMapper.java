package com.kiteclass.core.module.grade.mapper;

import com.kiteclass.core.module.grade.dto.request.CreateGradeComponentRequest;
import com.kiteclass.core.module.grade.dto.request.UpdateGradeComponentRequest;
import com.kiteclass.core.module.grade.dto.response.GradeComponentResponse;
import com.kiteclass.core.module.grade.dto.response.GradeResponse;
import com.kiteclass.core.module.grade.dto.response.GradingSummaryResponse;
import com.kiteclass.core.module.grade.dto.response.TranscriptResponse;
import com.kiteclass.core.module.grade.entity.Grade;
import com.kiteclass.core.module.grade.entity.GradeComponent;
import com.kiteclass.core.module.grade.entity.Transcript;
import org.mapstruct.BeanMapping;
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
 * Mapper for Grade, GradeComponent, and Transcript entities.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Mapper(componentModel = "spring")
public interface GradeMapper {

    // ==================== GradeComponent Mappings ====================

    /**
     * Map CreateGradeComponentRequest to GradeComponent entity.
     */
    @Mapping(target = "grade", ignore = true)
    @Mapping(target = "weightedScore", expression = "java(calculateWeightedScore(request))")
    GradeComponent toEntity(CreateGradeComponentRequest request);

    /**
     * Update GradeComponent entity from UpdateGradeComponentRequest.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "grade", ignore = true)
    @Mapping(target = "componentType", ignore = true)
    @Mapping(target = "componentRefId", ignore = true)
    @Mapping(target = "instanceId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "weightedScore", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateComponentEntity(UpdateGradeComponentRequest request, @MappingTarget GradeComponent component);

    /**
     * Map GradeComponent entity to GradeComponentResponse.
     */
    @Mapping(target = "gradeId", source = "grade.id")
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(component.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(component.getUpdatedAt()))")
    @Mapping(target = "percentage", expression = "java(component.getPercentage())")
    GradeComponentResponse toComponentResponse(GradeComponent component);

    /**
     * Map list of GradeComponent entities to list of GradeComponentResponse.
     */
    List<GradeComponentResponse> toComponentResponseList(List<GradeComponent> components);

    // ==================== Grade Mappings ====================

    /**
     * Map Grade entity to GradeResponse.
     */
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(grade.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(grade.getUpdatedAt()))")
    @Mapping(target = "components", source = "components")
    @Mapping(target = "isFinalized", expression = "java(grade.isFinalized())")
    @Mapping(target = "isPassed", expression = "java(grade.isPassed())")
    @Mapping(target = "isFailed", expression = "java(grade.isFailed())")
    @Mapping(target = "totalWeight", expression = "java(calculateTotalWeight(grade))")
    @Mapping(target = "isWeightValid", expression = "java(grade.isWeightsSumValid())")
    GradeResponse toResponse(Grade grade);

    /**
     * Map list of Grade entities to list of GradeResponse.
     */
    List<GradeResponse> toResponseList(List<Grade> grades);

    /**
     * Map Grade entity to GradingSummaryResponse (lightweight).
     */
    @Mapping(target = "isPassed", expression = "java(grade.isPassed())")
    @Mapping(target = "componentCount", expression = "java(grade.getComponents().size())")
    GradingSummaryResponse toSummaryResponse(Grade grade);

    /**
     * Map list of Grade entities to list of GradingSummaryResponse.
     */
    List<GradingSummaryResponse> toSummaryResponseList(List<Grade> grades);

    // ==================== Transcript Mappings ====================

    /**
     * Map Transcript entity to TranscriptResponse.
     */
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(transcript.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(transcript.getUpdatedAt()))")
    @Mapping(target = "grades", ignore = true)
    @Mapping(target = "studentName", ignore = true)
    @Mapping(target = "studentEmail", ignore = true)
    TranscriptResponse toTranscriptResponse(Transcript transcript);

    /**
     * Map list of Transcript entities to list of TranscriptResponse.
     */
    List<TranscriptResponse> toTranscriptResponseList(List<Transcript> transcripts);

    // ==================== Helper Methods ====================

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
     * Calculate weighted score from request data.
     *
     * @param request create component request
     * @return weighted score
     */
    default BigDecimal calculateWeightedScore(CreateGradeComponentRequest request) {
        if (request.getScore() == null || request.getMaxScore() == null ||
                request.getWeightPercent() == null) {
            return BigDecimal.ZERO;
        }

        if (request.getMaxScore().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Calculate percentage: (score / max_score) * 100
        BigDecimal percentage = request.getScore()
                .divide(request.getMaxScore(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Apply weight: percentage * (weight_percent / 100)
        return percentage
                .multiply(request.getWeightPercent())
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate total weight of all components in a grade.
     *
     * @param grade the grade entity
     * @return total weight percentage
     */
    default BigDecimal calculateTotalWeight(Grade grade) {
        if (grade.getComponents() == null || grade.getComponents().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return grade.getComponents().stream()
                .map(GradeComponent::getWeightPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
