package com.kiteclass.core.module.grade.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Transcript.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptResponse {

    private Long id;
    private Long studentId;
    private String semester;
    private Integer academicYear;
    private BigDecimal totalCredits;
    private BigDecimal semesterGpa;
    private BigDecimal cumulativeGpa;
    private Integer totalCourses;
    private Integer passedCourses;
    private Integer failedCourses;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Related data
    private List<GradingSummaryResponse> grades;

    // Student info (optional)
    private String studentName;
    private String studentEmail;
}
