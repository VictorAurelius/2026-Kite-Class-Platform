package com.kiteclass.core.module.grade.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for finalizing a grade.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalizeGradeRequest {

    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    @Size(max = 2000, message = "Comments must not exceed 2000 characters")
    private String comments;
}
