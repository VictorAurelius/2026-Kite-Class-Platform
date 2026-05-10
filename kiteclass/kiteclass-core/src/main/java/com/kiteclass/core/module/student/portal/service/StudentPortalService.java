package com.kiteclass.core.module.student.portal.service;

import com.kiteclass.core.module.student.portal.dto.StudentGradeDetailResponse;
import com.kiteclass.core.module.student.portal.dto.StudentGradeOverview;
import com.kiteclass.core.module.student.portal.dto.StudentNotificationFeedResponse;
import com.kiteclass.core.module.student.portal.dto.StudentPaymentSummary;
import com.kiteclass.core.module.student.portal.dto.StudentTodayResponse;

import java.util.List;

/**
 * Read-only service contract for the student-portal surface (GAP-269b).
 *
 * <p>Authorization: callers pass {@code studentReferenceId} taken from the
 * Gateway-injected {@code X-User-Reference-Id} header; implementations MUST
 * scope every read to that student. Phase 1 v1 returns empty payloads
 * (foundation pattern matching {@code ParentNotificationsFacetController});
 * full join logic against ClassSchedule / Assignment / SubjectGrade /
 * Invoice / notification feed is deferred to a follow-up that lands when
 * the FE swap-to-real-data PR is filed.
 *
 * @since GAP-269b (Wave 51 Bucket B)
 */
public interface StudentPortalService {

    /**
     * Today's schedule + assignments due-today for the calling student.
     *
     * @param studentReferenceId student users.reference_id from Gateway
     */
    StudentTodayResponse getToday(Long studentReferenceId);

    /**
     * Grades summary across every subject the student is enrolled in.
     */
    List<StudentGradeOverview> getGradesOverview(Long studentReferenceId);

    /**
     * Detailed grade rows for one subject the student is enrolled in.
     *
     * @throws com.kiteclass.core.common.exception.BusinessException
     *         {@code STUDENT_PORTAL_SUBJECT_NOT_FOUND} if the student is not
     *         enrolled in {@code subjectId}.
     */
    StudentGradeDetailResponse getSubjectDetail(Long studentReferenceId, Long subjectId);

    /**
     * Invoice summary for the calling student (own invoices only).
     */
    List<StudentPaymentSummary> getPayments(Long studentReferenceId);

    /**
     * Notification feed for the calling student (cursor-paginated).
     *
     * @param cursor opaque cursor or {@code null} for first page
     * @param limit page size (clamped to 1..100)
     */
    StudentNotificationFeedResponse getNotifications(
            Long studentReferenceId, String cursor, Integer limit);
}
