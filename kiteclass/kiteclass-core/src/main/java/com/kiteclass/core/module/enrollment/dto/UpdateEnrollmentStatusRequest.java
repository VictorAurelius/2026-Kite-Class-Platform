package com.kiteclass.core.module.enrollment.dto;

import com.kiteclass.core.common.constant.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating enrollment status.
 *
 * @author KiteClass Team
 * @since 2.6.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEnrollmentStatusRequest {

    /**
     * New enrollment status.
     * Required.
     */
    @NotNull(message = "Status is required")
    private EnrollmentStatus status;

    /**
     * Optional note about status change.
     */
    private String notes;
}
