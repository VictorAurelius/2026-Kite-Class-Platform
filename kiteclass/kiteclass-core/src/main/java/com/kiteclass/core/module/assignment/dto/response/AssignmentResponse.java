package com.kiteclass.core.module.assignment.dto.response;

import com.kiteclass.core.common.constant.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Assignment.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponse {

    private Long id;
    private Long classId;
    private String title;
    private String description;
    private String instructions;
    private LocalDateTime dueDate;
    private BigDecimal maxScore;
    private BigDecimal weightPercent;
    private Boolean allowLateSubmission;
    private BigDecimal latePenaltyPercent;
    private AssignmentStatus status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional computed fields
    private Boolean isOverdue;
    private Boolean isAcceptingSubmissions;
}
