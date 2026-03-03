package com.kiteclass.core.module.assignment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for submitting an assignment.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAssignmentRequest {

    @NotNull(message = "Assignment ID is required")
    private Long assignmentId;

    @Size(max = 500, message = "Content URL must not exceed 500 characters")
    private String contentUrl;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;
}
