package com.kiteclass.core.module.grade.dto.response;

import com.kiteclass.core.common.constant.GradeComponentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for GradeComponent.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeComponentResponse {

    private Long id;
    private Long gradeId;
    private GradeComponentType componentType;
    private String componentName;
    private Long componentRefId;
    private BigDecimal score;
    private BigDecimal maxScore;
    private BigDecimal weightPercent;
    private BigDecimal weightedScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed field
    private BigDecimal percentage;
}
