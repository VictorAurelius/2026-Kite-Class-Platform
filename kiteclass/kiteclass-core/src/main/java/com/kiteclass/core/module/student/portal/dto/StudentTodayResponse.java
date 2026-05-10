package com.kiteclass.core.module.student.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Today screen payload for the kc-student portal (GAP-269b).
 *
 * <p>Combines the student's class schedule for today plus assignments due
 * today across all enrolled subjects. Both lists are returned as empty in
 * the Phase 1 v1 stub; FE renders a stable empty state until the join
 * services land in a follow-up.
 *
 * @since GAP-269b (Wave 51 Bucket B)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentTodayResponse {

    /** ISO-8601 date the response was computed for (server-side "today"). */
    private LocalDate date;

    /** Schedule periods for today, ordered by {@code periodNo} ascending. */
    private List<SchedulePeriod> schedulePeriods;

    /** Assignments whose due-date equals today, oldest-first by created. */
    private List<AssignmentDueToday> assignmentsDueToday;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchedulePeriod {
        private Integer periodNo;
        private String subjectName;
        private String teacherName;
        private LocalTime startTime;
        private LocalTime endTime;
        private String room;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentDueToday {
        private Long assignmentId;
        private String title;
        private String subjectName;
        private String dueAt; // ISO-8601 datetime
        private String status; // PENDING / SUBMITTED / GRADED
    }
}
