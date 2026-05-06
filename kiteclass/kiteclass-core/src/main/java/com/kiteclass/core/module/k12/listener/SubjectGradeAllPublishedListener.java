package com.kiteclass.core.module.k12.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.outbox.OutboxEventWriter;
import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.entity.Semester;
import com.kiteclass.core.module.k12.entity.SubjectGrade;
import com.kiteclass.core.module.k12.enums.SubjectGradeStatus;
import com.kiteclass.core.module.k12.event.SubjectGradeAllPublishedEvent;
import com.kiteclass.core.module.k12.repository.SubjectGradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Học bạ generation trigger — invoked from
 * {@link com.kiteclass.core.module.k12.service.SubjectGradeServiceImpl#publish}
 * after the publish commit. When the count of "not-yet-PUBLISHED" subject grades
 * for the (student, academicYear) reaches zero, fires
 * {@link SubjectGradeAllPublishedEvent} via the Outbox so downstream consumers
 * (GAP-055 học bạ generator) can materialise the transcript reliably.
 *
 * <p>This is NOT a Spring {@code @EventListener}; it is invoked synchronously
 * inside the publish transaction. Reason: Outbox writes need {@code Propagation.MANDATORY}
 * (caller's transaction) — async event listeners run on a fresh transaction.
 *
 * <p>Reference: BR-GRADEBOOK-006 in
 * {@code documents/01-business/kiteclass/multi-subject-gradebook/rules.md}
 * and UC-GRADEBOOK-PUBLISH-COMPLETE in {@code use-cases.md}.
 *
 * @since 5.x (Wave 24 Bucket B — GAP-360 §360.5)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubjectGradeAllPublishedListener {

    private final SubjectGradeRepository subjectGradeRepository;
    private final OutboxEventWriter outbox;
    private final ObjectMapper objectMapper;

    /**
     * Check whether all subject grades for {@code grade.studentId} in
     * {@code grade.semester.academicYear} are now {@code PUBLISHED}; if so,
     * emit the all-published event via Outbox.
     *
     * <p>Caller MUST hold an open transaction (the publish transaction) so the
     * Outbox row commits atomically with the final PUBLISHED status update.
     *
     * @param grade the grade just transitioned to PUBLISHED (caller's reference)
     */
    public void onPublish(SubjectGrade grade) {
        if (grade == null || grade.getStudentId() == null) {
            log.debug("onPublish called with null grade or studentId — skipping");
            return;
        }

        Long academicYearId = resolveAcademicYearId(grade);
        if (academicYearId == null) {
            log.debug("Cannot resolve academicYearId for grade {} — skipping all-published check",
                    grade.getId());
            return;
        }

        long pendingCount = subjectGradeRepository
                .countNotInStatusForStudentAndAcademicYear(
                        grade.getStudentId(), academicYearId, SubjectGradeStatus.PUBLISHED);

        if (pendingCount > 0) {
            log.debug("Student {} academic year {} still has {} non-PUBLISHED grades — skipping event",
                    grade.getStudentId(), academicYearId, pendingCount);
            return;
        }

        SubjectGradeAllPublishedEvent event = new SubjectGradeAllPublishedEvent(
                grade.getStudentId(),
                academicYearId,
                Instant.now());

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize SubjectGradeAllPublishedEvent for student {} year {}: {}",
                    grade.getStudentId(), academicYearId, ex.getMessage());
            return;
        }

        outbox.enqueue(
                SubjectGradeAllPublishedEvent.ROUTING_KEY,
                SubjectGradeAllPublishedEvent.AGGREGATE_TYPE,
                grade.getStudentId() + ":" + academicYearId,
                payload);

        log.info("Emitted {} for student {} year {}",
                SubjectGradeAllPublishedEvent.ROUTING_KEY, grade.getStudentId(), academicYearId);
    }

    private Long resolveAcademicYearId(SubjectGrade grade) {
        Semester semester = grade.getSemester();
        if (semester == null) {
            return null;
        }
        AcademicYear ay = semester.getAcademicYear();
        return ay == null ? null : ay.getId();
    }
}
