package com.kiteclass.core.module.enrollment.dto;

import com.kiteclass.core.common.constant.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Response DTO for enrollment data.
 *
 * @author KiteClass Team
 * @since 2.6.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {

    /**
     * Enrollment ID.
     */
    private Long id;

    /**
     * Student ID.
     */
    private Long studentId;

    /**
     * Class ID.
     */
    private Long classId;

    /**
     * Enrollment date.
     */
    private LocalDateTime enrollmentDate;

    /**
     * Current enrollment status.
     */
    private EnrollmentStatus status;

    /**
     * Original tuition amount.
     */
    private BigDecimal tuitionAmount;

    /**
     * Discount percentage applied.
     */
    private BigDecimal discountPercent;

    /**
     * Final amount after discount.
     */
    private BigDecimal finalAmount;

    /**
     * Additional notes.
     */
    private String notes;

    /**
     * Creation timestamp.
     */
    private Instant createdAt;

    /**
     * Last update timestamp.
     */
    private Instant updatedAt;
}
