package com.kiteclass.core.module.grade.dto.response;

import com.kiteclass.core.common.constant.GradeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Grade with all components.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeResponse {

    private Long id;
    private Long studentId;
    private Long classId;
    private BigDecimal finalScore;
    private String letterGrade;
    private BigDecimal gpa;
    private GradeStatus status;
    private BigDecimal passThreshold;
    private String comments;
    private LocalDateTime calculatedAt;
    private LocalDateTime finalizedAt;
    private Long finalizedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Related data
    private List<GradeComponentResponse> components;

    // Computed fields
    private Boolean isFinalized;
    private Boolean isPassed;
    private Boolean isFailed;
    private BigDecimal totalWeight;
    private Boolean isWeightValid;
}
