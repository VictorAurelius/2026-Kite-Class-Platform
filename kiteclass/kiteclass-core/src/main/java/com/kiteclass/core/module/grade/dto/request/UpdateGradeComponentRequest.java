package com.kiteclass.core.module.grade.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating a grade component.
 * All fields are optional to support partial updates.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGradeComponentRequest {

    @Size(max = 255, message = "Component name must not exceed 255 characters")
    private String componentName;

    @DecimalMin(value = "0.00", message = "Score must be at least 0")
    @DecimalMax(value = "999.99", message = "Score must not exceed 999.99")
    private BigDecimal score;

    @DecimalMin(value = "0.01", message = "Max score must be greater than 0")
    @DecimalMax(value = "999.99", message = "Max score must not exceed 999.99")
    private BigDecimal maxScore;

    @DecimalMin(value = "0.00", message = "Weight percent must be at least 0")
    @DecimalMax(value = "100.00", message = "Weight percent must not exceed 100")
    private BigDecimal weightPercent;
}
