package com.kiteclass.core.module.assignment.dto.response;

import com.kiteclass.core.common.constant.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Submission.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponse {

    private Long id;
    private Long assignmentId;
    private Long studentId;
    private LocalDateTime submissionDate;
    private String contentUrl;
    private String notes;
    private BigDecimal score;
    private BigDecimal adjustedScore;
    private SubmissionStatus status;
    private Long gradedBy;
    private LocalDateTime gradedAt;
    private String feedback;
    private LocalDateTime createdAt;

    // Additional computed fields
    private Boolean isLate;
    private BigDecimal penaltyApplied;
}
