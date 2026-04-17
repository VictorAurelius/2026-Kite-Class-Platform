package com.kiteclass.core.module.assignment.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for creating an assignment.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssignmentRequest {

    @NotNull(message = "Class ID is required")
    private Long classId;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Size(max = 5000, message = "Instructions must not exceed 5000 characters")
    private String instructions;

    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in the future")
    private LocalDateTime dueDate;

    @NotNull(message = "Max score is required")
    @DecimalMin(value = "0.01", message = "Max score must be greater than 0")
    @DecimalMax(value = "999.99", message = "Max score must not exceed 999.99")
    private BigDecimal maxScore;

    @NotNull(message = "Weight percent is required")
    @DecimalMin(value = "0.00", message = "Weight percent must be at least 0")
    @DecimalMax(value = "100.00", message = "Weight percent must not exceed 100")
    private BigDecimal weightPercent;

    @NotNull(message = "Allow late submission flag is required")
    private Boolean allowLateSubmission;

    @DecimalMin(value = "0.00", message = "Late penalty percent must be at least 0")
    @DecimalMax(value = "100.00", message = "Late penalty percent must not exceed 100")
    private BigDecimal latePenaltyPercent;
}
