package com.kiteclass.core.module.gamification.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.gamification.entity.StudentPoint;
import com.kiteclass.core.module.gamification.repository.StudentPointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PointServiceImpl}.
 *
 * <p>Verifies attendance point awarding, updating, and total point retrieval
 * using Mockito mocks for repository and tenant context.
 *
 * @author KiteClass Team
 * @since 2026-03-24
 */
@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Long STUDENT_ID = 100L;
    private static final Long ATTENDANCE_ID = 200L;
    private static final Integer POINTS = 10;
    private static final String DESCRIPTION = "Present in class";
    private static final String ATTENDANCE_REFERENCE_TYPE = "ATTENDANCE";

    @Mock
    private StudentPointRepository studentPointRepository;

    @InjectMocks
    private PointServiceImpl pointService;

    @Captor
    private ArgumentCaptor<StudentPoint> studentPointCaptor;

    @Test
    @DisplayName("awardAttendancePoints - saves a StudentPoint with correct fields")
    void awardAttendancePoints_savesPointWithCorrectFields() {
        try (MockedStatic<TenantContext> mockedStatic = mockStatic(TenantContext.class)) {
            mockedStatic.when(TenantContext::getCurrentTenant).thenReturn(TENANT_ID);

            pointService.awardAttendancePoints(STUDENT_ID, ATTENDANCE_ID, POINTS, DESCRIPTION);

            verify(studentPointRepository).save(studentPointCaptor.capture());
            StudentPoint saved = studentPointCaptor.getValue();

            assertThat(saved.getInstanceId()).isEqualTo(TENANT_ID);
            assertThat(saved.getStudentId()).isEqualTo(STUDENT_ID);
            assertThat(saved.getPoints()).isEqualTo(POINTS);
            assertThat(saved.getReferenceType()).isEqualTo(ATTENDANCE_REFERENCE_TYPE);
            assertThat(saved.getReferenceId()).isEqualTo(ATTENDANCE_ID);
            assertThat(saved.getDescription()).isEqualTo(DESCRIPTION);
            assertThat(saved.getEarnedAt()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
        }
    }

    @Test
    @DisplayName("updateAttendancePoints - deletes old record and creates new one")
    void updateAttendancePoints_deletesOldAndCreatesNew() {
        try (MockedStatic<TenantContext> mockedStatic = mockStatic(TenantContext.class)) {
            mockedStatic.when(TenantContext::getCurrentTenant).thenReturn(TENANT_ID);

            StudentPoint oldPoint = StudentPoint.builder()
                    .id(1L)
                    .instanceId(TENANT_ID)
                    .studentId(STUDENT_ID)
                    .points(5)
                    .referenceType(ATTENDANCE_REFERENCE_TYPE)
                    .referenceId(ATTENDANCE_ID)
                    .description("Old description")
                    .build();

            when(studentPointRepository.findByReferenceTypeAndReferenceId(
                    eq(ATTENDANCE_REFERENCE_TYPE), eq(ATTENDANCE_ID)))
                    .thenReturn(Optional.of(oldPoint));

            Integer newPoints = 15;
            String newDescription = "Updated attendance";

            pointService.updateAttendancePoints(STUDENT_ID, ATTENDANCE_ID, newPoints, newDescription);

            verify(studentPointRepository).delete(oldPoint);
            verify(studentPointRepository).save(studentPointCaptor.capture());
            StudentPoint saved = studentPointCaptor.getValue();

            assertThat(saved.getInstanceId()).isEqualTo(TENANT_ID);
            assertThat(saved.getStudentId()).isEqualTo(STUDENT_ID);
            assertThat(saved.getPoints()).isEqualTo(newPoints);
            assertThat(saved.getReferenceType()).isEqualTo(ATTENDANCE_REFERENCE_TYPE);
            assertThat(saved.getReferenceId()).isEqualTo(ATTENDANCE_ID);
            assertThat(saved.getDescription()).isEqualTo(newDescription);
        }
    }

    @Test
    @DisplayName("updateAttendancePoints - no old record exists, still creates new point")
    void updateAttendancePoints_noOldRecord_stillCreatesNew() {
        try (MockedStatic<TenantContext> mockedStatic = mockStatic(TenantContext.class)) {
            mockedStatic.when(TenantContext::getCurrentTenant).thenReturn(TENANT_ID);

            when(studentPointRepository.findByReferenceTypeAndReferenceId(
                    eq(ATTENDANCE_REFERENCE_TYPE), eq(ATTENDANCE_ID)))
                    .thenReturn(Optional.empty());

            pointService.updateAttendancePoints(STUDENT_ID, ATTENDANCE_ID, POINTS, DESCRIPTION);

            verify(studentPointRepository, never()).delete(any(StudentPoint.class));
            verify(studentPointRepository).save(studentPointCaptor.capture());
            StudentPoint saved = studentPointCaptor.getValue();

            assertThat(saved.getInstanceId()).isEqualTo(TENANT_ID);
            assertThat(saved.getStudentId()).isEqualTo(STUDENT_ID);
            assertThat(saved.getPoints()).isEqualTo(POINTS);
            assertThat(saved.getReferenceType()).isEqualTo(ATTENDANCE_REFERENCE_TYPE);
            assertThat(saved.getReferenceId()).isEqualTo(ATTENDANCE_ID);
            assertThat(saved.getDescription()).isEqualTo(DESCRIPTION);
        }
    }

    @Test
    @DisplayName("getTotalPoints - returns value from repository")
    void getTotalPoints_returnsValueFromRepository() {
        Integer expectedTotal = 42;
        when(studentPointRepository.getTotalPointsByStudentId(STUDENT_ID)).thenReturn(expectedTotal);

        Integer result = pointService.getTotalPoints(STUDENT_ID);

        assertThat(result).isEqualTo(expectedTotal);
        verify(studentPointRepository, times(1)).getTotalPointsByStudentId(STUDENT_ID);
    }
}
