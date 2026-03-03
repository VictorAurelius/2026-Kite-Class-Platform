package com.kiteclass.core.module.assignment.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for grading a submission.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeSubmissionRequest {

    @NotNull(message = "Score is required")
    @DecimalMin(value = "0.00", message = "Score must be at least 0")
    private BigDecimal score;

    @Size(max = 5000, message = "Feedback must not exceed 5000 characters")
    private String feedback;
}
