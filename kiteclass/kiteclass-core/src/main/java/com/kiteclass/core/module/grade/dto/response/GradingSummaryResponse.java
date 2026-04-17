package com.kiteclass.core.module.grade.dto.response;

import com.kiteclass.core.common.constant.GradeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight response DTO for Grade summary (without components).
 * Used for listings and reports.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingSummaryResponse {

    private Long id;
    private Long studentId;
    private Long classId;
    private BigDecimal finalScore;
    private String letterGrade;
    private BigDecimal gpa;
    private GradeStatus status;

    // Computed fields
    private Boolean isPassed;
    private Integer componentCount;
}
