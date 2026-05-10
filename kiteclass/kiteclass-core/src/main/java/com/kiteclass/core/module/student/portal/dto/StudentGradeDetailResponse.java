package com.kiteclass.core.module.student.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Per-subject grade detail payload (GAP-269b).
 *
 * <p>Returned by {@code GET /api/v1/students/me/grades/{subjectId}}. Carries
 * the subject summary plus an ordered entry list (newest-first) covering
 * every published grade row visible to the student for that subject.
 *
 * @since GAP-269b (Wave 51 Bucket B)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGradeDetailResponse {

    private Long subjectId;
    private String subjectName;
    private BigDecimal average;
    private List<GradeEntry> entries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeEntry {
        private Long entryId;
        private String type; // TX / GK / CK / OTHER per TT 22/2021
        private String label;
        private BigDecimal score;
        private BigDecimal maxScore;
        private LocalDate gradedAt;
        private String comment;
    }
}
