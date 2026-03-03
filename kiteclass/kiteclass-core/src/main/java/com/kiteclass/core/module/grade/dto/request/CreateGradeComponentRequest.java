package com.kiteclass.core.module.grade.dto.request;

import com.kiteclass.core.common.constant.GradeComponentType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a grade component.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGradeComponentRequest {

    @NotNull(message = "Grade ID is required")
    private Long gradeId;

    @NotNull(message = "Component type is required")
    private GradeComponentType componentType;

    @NotBlank(message = "Component name is required")
    @Size(max = 255, message = "Component name must not exceed 255 characters")
    private String componentName;

    private Long componentRefId;

    @NotNull(message = "Score is required")
    @DecimalMin(value = "0.00", message = "Score must be at least 0")
    @DecimalMax(value = "999.99", message = "Score must not exceed 999.99")
    private BigDecimal score;

    @NotNull(message = "Max score is required")
    @DecimalMin(value = "0.01", message = "Max score must be greater than 0")
    @DecimalMax(value = "999.99", message = "Max score must not exceed 999.99")
    private BigDecimal maxScore;

    @NotNull(message = "Weight percent is required")
    @DecimalMin(value = "0.00", message = "Weight percent must be at least 0")
    @DecimalMax(value = "100.00", message = "Weight percent must not exceed 100")
    private BigDecimal weightPercent;
}
