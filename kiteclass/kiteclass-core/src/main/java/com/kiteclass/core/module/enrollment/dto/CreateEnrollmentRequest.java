package com.kiteclass.core.module.enrollment.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new enrollment.
 *
 * @author KiteClass Team
 * @since 2.6.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEnrollmentRequest {

    /**
     * ID of the student to enroll.
     * Required.
     */
    @NotNull(message = "Student ID is required")
    @Positive(message = "Student ID must be positive")
    private Long studentId;

    /**
     * ID of the class to enroll in.
     * Required.
     */
    @NotNull(message = "Class ID is required")
    @Positive(message = "Class ID must be positive")
    private Long classId;

    /**
     * Tuition amount for this enrollment.
     * Required, must be non-negative.
     */
    @NotNull(message = "Tuition amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Tuition amount must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Tuition amount must have at most 8 integer digits and 2 decimal places")
    private BigDecimal tuitionAmount;

    /**
     * Discount percentage to apply.
     * Optional, defaults to 0. Range: 0-100.
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "Discount percent must be >= 0")
    @DecimalMax(value = "100.0", inclusive = true, message = "Discount percent must be <= 100")
    @Digits(integer = 3, fraction = 2, message = "Discount percent must have at most 3 integer digits and 2 decimal places")
    private BigDecimal discountPercent;

    /**
     * Additional notes about the enrollment.
     * Optional, max 2000 characters.
     */
    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;
}
