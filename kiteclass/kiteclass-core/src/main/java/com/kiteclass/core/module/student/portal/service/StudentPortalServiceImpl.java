package com.kiteclass.core.module.student.portal.service;

import com.kiteclass.core.module.student.portal.dto.StudentGradeDetailResponse;
import com.kiteclass.core.module.student.portal.dto.StudentGradeOverview;
import com.kiteclass.core.module.student.portal.dto.StudentNotificationFeedResponse;
import com.kiteclass.core.module.student.portal.dto.StudentPaymentSummary;
import com.kiteclass.core.module.student.portal.dto.StudentTodayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of {@link StudentPortalService} (GAP-269b Phase 1
 * v1 foundation).
 *
 * <p>Returns empty / zero-state payloads so the FE can wire against stable
 * shapes immediately while full join logic against ClassSchedule /
 * Assignment / SubjectGrade / Invoice / notification feed lands in a
 * follow-up. Pattern mirrors {@code ParentNotificationsFacetService} from
 * Wave 18b2 — contract-first, body-later.
 *
 * @since GAP-269b (Wave 51 Bucket B)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentPortalServiceImpl implements StudentPortalService {

    private static final int DEFAULT_NOTIFICATION_LIMIT = 20;
    private static final int MAX_NOTIFICATION_LIMIT = 100;

    @Override
    public StudentTodayResponse getToday(Long studentReferenceId) {
        log.debug("getToday studentReferenceId={}", studentReferenceId);
        return StudentTodayResponse.builder()
                .date(LocalDate.now())
                .schedulePeriods(Collections.emptyList())
                .assignmentsDueToday(Collections.emptyList())
                .build();
    }

    @Override
    public List<StudentGradeOverview> getGradesOverview(Long studentReferenceId) {
        log.debug("getGradesOverview studentReferenceId={}", studentReferenceId);
        return Collections.emptyList();
    }

    @Override
    public StudentGradeDetailResponse getSubjectDetail(
            Long studentReferenceId, Long subjectId) {
        log.debug("getSubjectDetail studentReferenceId={} subjectId={}",
                studentReferenceId, subjectId);
        // Phase 1 v1 returns an empty detail body; subject-existence + enrollment
        // scope check moves here once the join lands. FE renders empty state.
        return StudentGradeDetailResponse.builder()
                .subjectId(subjectId)
                .subjectName(null)
                .average(null)
                .entries(Collections.emptyList())
                .build();
    }

    @Override
    public List<StudentPaymentSummary> getPayments(Long studentReferenceId) {
        log.debug("getPayments studentReferenceId={}", studentReferenceId);
        return Collections.emptyList();
    }

    @Override
    public StudentNotificationFeedResponse getNotifications(
            Long studentReferenceId, String cursor, Integer limit) {
        int clamped = clampLimit(limit);
        log.debug("getNotifications studentReferenceId={} cursor={} limit={}",
                studentReferenceId, cursor, clamped);
        return StudentNotificationFeedResponse.builder()
                .items(Collections.emptyList())
                .nextCursor(null)
                .build();
    }

    private int clampLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_NOTIFICATION_LIMIT;
        }
        return Math.min(limit, MAX_NOTIFICATION_LIMIT);
    }
}
