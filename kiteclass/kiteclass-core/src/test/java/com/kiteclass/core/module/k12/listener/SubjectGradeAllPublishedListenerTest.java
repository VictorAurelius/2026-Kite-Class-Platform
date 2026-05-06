package com.kiteclass.core.module.k12.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.outbox.OutboxEventWriter;
import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.entity.Semester;
import com.kiteclass.core.module.k12.entity.SubjectGrade;
import com.kiteclass.core.module.k12.enums.SubjectGradeStatus;
import com.kiteclass.core.module.k12.event.SubjectGradeAllPublishedEvent;
import com.kiteclass.core.module.k12.repository.SubjectGradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SubjectGradeAllPublishedListener} — §360.5 học bạ
 * trigger contract.
 *
 * <ul>
 *   <li>NOT all grades published → no event</li>
 *   <li>All grades published → Outbox event emitted with correct routing key
 *       + aggregate id format</li>
 *   <li>Null grade or null student → safe no-op</li>
 *   <li>Missing semester / academicYear → safe no-op</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class SubjectGradeAllPublishedListenerTest {

    @Mock
    private SubjectGradeRepository repository;

    @Mock
    private OutboxEventWriter outbox;

    @InjectMocks
    private SubjectGradeAllPublishedListener listener;

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private static final Long STUDENT_ID = 900L;
    private static final Long ACADEMIC_YEAR_ID = 2026L;

    private SubjectGrade grade;

    @BeforeEach
    void setUp() {
        // Inject real ObjectMapper (record serialization is straightforward).
        listener = new SubjectGradeAllPublishedListener(repository, outbox, mapper);

        AcademicYear ay = new AcademicYear();
        ay.setId(ACADEMIC_YEAR_ID);
        Semester semester = new Semester();
        semester.setAcademicYear(ay);
        grade = SubjectGrade.builder()
                .studentId(STUDENT_ID)
                .semester(semester)
                .status(SubjectGradeStatus.PUBLISHED)
                .build();
        grade.setId(500L);
    }

    @Test
    void onPublish_pendingGradesRemain_skipsEvent() {
        when(repository.countNotInStatusForStudentAndAcademicYear(
                STUDENT_ID, ACADEMIC_YEAR_ID, SubjectGradeStatus.PUBLISHED)).thenReturn(2L);

        listener.onPublish(grade);

        verifyNoInteractions(outbox);
    }

    @Test
    void onPublish_allPublished_emitsOutboxEvent() {
        when(repository.countNotInStatusForStudentAndAcademicYear(
                STUDENT_ID, ACADEMIC_YEAR_ID, SubjectGradeStatus.PUBLISHED)).thenReturn(0L);

        listener.onPublish(grade);

        verify(outbox, times(1)).enqueue(
                eq(SubjectGradeAllPublishedEvent.ROUTING_KEY),
                eq(SubjectGradeAllPublishedEvent.AGGREGATE_TYPE),
                eq(STUDENT_ID + ":" + ACADEMIC_YEAR_ID),
                contains("\"studentId\":" + STUDENT_ID));
    }

    @Test
    void onPublish_nullGrade_safeNoop() {
        listener.onPublish(null);
        verifyNoInteractions(outbox, repository);
    }

    @Test
    void onPublish_nullStudent_safeNoop() {
        SubjectGrade noStudent = SubjectGrade.builder().build();
        listener.onPublish(noStudent);
        verifyNoInteractions(outbox, repository);
    }

    @Test
    void onPublish_missingAcademicYear_safeNoop() {
        SubjectGrade noSemester = SubjectGrade.builder()
                .studentId(STUDENT_ID)
                .build();
        listener.onPublish(noSemester);
        verify(outbox, never()).enqueue(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }
}
