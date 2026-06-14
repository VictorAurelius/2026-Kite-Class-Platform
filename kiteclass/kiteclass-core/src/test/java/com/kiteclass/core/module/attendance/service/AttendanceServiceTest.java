package com.kiteclass.core.module.attendance.service;

import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.common.constant.TeacherClassRole;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.attendance.dto.AttendanceResponse;
import com.kiteclass.core.module.attendance.dto.AttendanceStatsResponse;
import com.kiteclass.core.module.attendance.dto.CreateAttendanceRequest;
import com.kiteclass.core.module.attendance.dto.UpdateAttendanceStatusRequest;
import com.kiteclass.core.module.attendance.entity.Attendance;
import com.kiteclass.core.module.attendance.mapper.AttendanceMapper;
import com.kiteclass.core.module.attendance.repository.AttendanceRepository;
import com.kiteclass.core.common.constant.SessionStatus;
import com.kiteclass.core.module.clazz.entity.ClassSession;
import com.kiteclass.core.module.clazz.repository.ClassSessionRepository;
import com.kiteclass.core.module.enrollment.entity.Enrollment;
import com.kiteclass.core.module.enrollment.repository.EnrollmentRepository;
import com.kiteclass.core.module.gamification.service.PointService;
import com.kiteclass.core.module.teacher.entity.TeacherClass;
import com.kiteclass.core.module.teacher.repository.TeacherClassRepository;
import com.kiteclass.core.testutil.AttendanceTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.kiteclass.core.common.security.AuthorizationBean;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AttendanceServiceImpl}.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceService Tests")
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private TeacherClassRepository teacherClassRepository;

    @Mock
    private AttendanceMapper attendanceMapper;

    @Mock
    private PointService pointService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    // GAP-1300/GAP-1301: AttendanceServiceImpl now consults AuthorizationBean.isAdmin() to let
    // ADMIN/OWNER bypass the per-class MAIN_TEACHER check. Unstubbed mock → isAdmin() returns
    // false, so these tests exercise the non-admin (MAIN_TEACHER-required) path as before.
    @Mock
    private AuthorizationBean authz;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private Enrollment testEnrollment;
    private CreateAttendanceRequest createRequest;
    private Attendance testAttendance;
    private AttendanceResponse testResponse;
    private ClassSession testSession;
    private TeacherClass testTeacherClass;

    @BeforeEach
    void setUp() {
        UUID tenantId = UUID.randomUUID();

        testEnrollment = Enrollment.builder()
                .studentId(1L)
                .classId(1L)
                .status(EnrollmentStatus.ACTIVE)
                .tuitionAmount(new BigDecimal("1000.00"))
                .discountPercent(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("1000.00"))
                .build();
        testEnrollment.setId(1L);
        testEnrollment.setInstanceId(tenantId);

        createRequest = AttendanceTestDataBuilder.createRequestForEnrollmentAndSession(1L, 1L);

        testAttendance = AttendanceTestDataBuilder.createAttendance(1L, 1L);
        testAttendance.setInstanceId(tenantId);

        testResponse = AttendanceResponse.builder()
                .id(1L)
                .enrollmentId(1L)
                .sessionId(1L)
                .status(AttendanceStatus.PRESENT)
                .pointsAwarded(0)
                .build();

        testSession = ClassSession.builder()
                .classId(1L)
                .sessionNumber(1)
                .attendanceTaken(false)
                .build();
        testSession.setId(1L);

        testTeacherClass = TeacherClass.builder()
                .teacherId(1L)
                .classId(1L)
                .role(TeacherClassRole.MAIN_TEACHER)
                .build();
        testTeacherClass.setId(1L);

        // GAP-992: single-mark now loads session to enforce BR-ATTEND-002 (status guard).
        // lenient — error-path tests throw before reaching the session lookup.
        lenient().when(classSessionRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testSession));
    }

    @Test
    @DisplayName("Should mark attendance successfully")
    void shouldMarkAttendanceSuccessfully() {
        // Arrange
        when(enrollmentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testEnrollment));
        when(attendanceRepository.existsByEnrollmentIdAndSessionIdAndDeletedFalse(1L, 1L))
                .thenReturn(false);
        when(attendanceMapper.toEntity(createRequest))
                .thenReturn(testAttendance);
        when(attendanceRepository.save(any(Attendance.class)))
                .thenReturn(testAttendance);
        when(attendanceMapper.toResponse(testAttendance))
                .thenReturn(testResponse);

        // Act
        AttendanceResponse result = attendanceService.markAttendance(createRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEnrollmentId()).isEqualTo(1L);
        assertThat(result.getSessionId()).isEqualTo(1L);

        verify(attendanceRepository).save(any(Attendance.class));
        verify(pointService).awardAttendancePoints(eq(1L), anyLong(), eq(0), anyString());
    }

    @Test
    @DisplayName("Should throw exception when enrollment not found")
    void shouldThrowExceptionWhenEnrollmentNotFound() {
        // Arrange
        when(enrollmentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> attendanceService.markAttendance(createRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("ENROLLMENT_NOT_FOUND"));

        verify(attendanceRepository, never()).save(any());
        verify(pointService, never()).awardAttendancePoints(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when enrollment not active")
    void shouldThrowExceptionWhenEnrollmentNotActive() {
        // Arrange
        testEnrollment.setStatus(EnrollmentStatus.WITHDRAWN);
        when(enrollmentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testEnrollment));

        // Act & Assert
        assertThatThrownBy(() -> attendanceService.markAttendance(createRequest))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("ENROLLMENT_NOT_ACTIVE"));

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when attendance already marked")
    void shouldThrowExceptionWhenAttendanceAlreadyMarked() {
        // Arrange
        when(enrollmentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testEnrollment));
        when(attendanceRepository.existsByEnrollmentIdAndSessionIdAndDeletedFalse(1L, 1L))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> attendanceService.markAttendance(createRequest))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("ATTENDANCE_ALREADY_MARKED"));

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("GAP-992: Should reject mark for CANCELLED/COMPLETED session")
    void shouldRejectMarkWhenSessionNotMarkable() {
        // Arrange — session is CANCELLED (BR-ATTEND-002)
        ClassSession cancelled = ClassSession.builder()
                .classId(1L)
                .sessionNumber(1)
                .status(SessionStatus.CANCELLED)
                .build();
        cancelled.setId(1L);
        when(enrollmentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testEnrollment));
        when(attendanceRepository.existsByEnrollmentIdAndSessionIdAndDeletedFalse(1L, 1L))
                .thenReturn(false);
        when(classSessionRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(cancelled));

        // Act & Assert
        assertThatThrownBy(() -> attendanceService.markAttendance(createRequest))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("SESSION_NOT_MARKABLE"));

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("GAP-993: Should reject EXCUSED status without a note")
    void shouldRejectExcusedWithoutNote() {
        // Arrange — EXCUSED requires a note (BR-ATT-005)
        CreateAttendanceRequest excusedNoNote = CreateAttendanceRequest.builder()
                .enrollmentId(1L)
                .sessionId(1L)
                .status(AttendanceStatus.EXCUSED)
                .notes(null)
                .build();
        when(enrollmentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testEnrollment));
        when(attendanceRepository.existsByEnrollmentIdAndSessionIdAndDeletedFalse(1L, 1L))
                .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> attendanceService.markAttendance(excusedNoNote))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("EXCUSED_REQUIRES_NOTE"));

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should award negative points for ABSENT status")
    void shouldAwardNegativePointsForAbsent() {
        // Arrange
        CreateAttendanceRequest absentRequest = AttendanceTestDataBuilder.createRequestWithStatus(
                1L, 1L, AttendanceStatus.ABSENT
        );
        Attendance absentAttendance = AttendanceTestDataBuilder.createAbsentAttendance(1L, 1L);

        when(enrollmentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testEnrollment));
        when(attendanceRepository.existsByEnrollmentIdAndSessionIdAndDeletedFalse(1L, 1L))
                .thenReturn(false);
        when(attendanceMapper.toEntity(absentRequest))
                .thenReturn(absentAttendance);
        when(attendanceRepository.save(any(Attendance.class)))
                .thenReturn(absentAttendance);
        when(attendanceMapper.toResponse(absentAttendance))
                .thenReturn(testResponse);

        // Act
        attendanceService.markAttendance(absentRequest);

        // Assert
        verify(pointService).awardAttendancePoints(eq(1L), anyLong(), eq(-10), anyString());
    }

    @Test
    @DisplayName("Should get attendance by ID")
    void shouldGetAttendanceById() {
        // Arrange
        when(attendanceRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testAttendance));
        when(enrollmentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testEnrollment));
        when(attendanceMapper.toResponse(testAttendance))
                .thenReturn(testResponse);

        // Act
        AttendanceResponse result = attendanceService.getAttendanceById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should get attendance by enrollment with pagination")
    void shouldGetAttendanceByEnrollment() {
        // Arrange
        Page<Attendance> attendancePage = new PageImpl<>(List.of(testAttendance));
        when(enrollmentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testEnrollment));
        when(attendanceRepository.findByEnrollmentIdAndDeletedFalse(eq(1L), any(Pageable.class)))
                .thenReturn(attendancePage);
        when(attendanceMapper.toResponse(testAttendance))
                .thenReturn(testResponse);

        // Act
        Page<AttendanceResponse> result = attendanceService.getAttendanceByEnrollment(
                1L, Pageable.unpaged()
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should update attendance status")
    void shouldUpdateAttendanceStatus() {
        // Arrange
        UpdateAttendanceStatusRequest updateRequest =
                AttendanceTestDataBuilder.createUpdateStatusRequest(AttendanceStatus.EXCUSED);

        when(attendanceRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testAttendance));
        when(enrollmentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testEnrollment));
        when(classSessionRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testSession));
        when(teacherClassRepository.findByTeacherIdAndClassId(1L, 1L))
                .thenReturn(Optional.of(testTeacherClass));
        when(attendanceRepository.save(any(Attendance.class)))
                .thenReturn(testAttendance);
        when(attendanceMapper.toResponse(testAttendance))
                .thenReturn(testResponse);

        // Act
        AttendanceResponse result = attendanceService.updateAttendanceStatus(1L, updateRequest, 1L);

        // Assert
        assertThat(result).isNotNull();
        verify(attendanceRepository).save(any(Attendance.class));
        verify(pointService).updateAttendancePoints(eq(1L), eq(1L), eq(0), anyString());
    }

    @Test
    @DisplayName("GAP-1301 — ADMIN/OWNER bypasses MAIN_TEACHER check on status update")
    void shouldUpdateAttendanceStatus_whenAdminBypassesOwnership() {
        // Given — ADMIN/OWNER carries no numeric reference id (teacherId == null) and has no
        // TeacherClass row; AuthorizationBean.isAdmin() == true lets them bypass the per-class
        // MAIN_TEACHER check (GAP-1301).
        UpdateAttendanceStatusRequest updateRequest =
                AttendanceTestDataBuilder.createUpdateStatusRequest(AttendanceStatus.EXCUSED);

        when(attendanceRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testAttendance));
        when(enrollmentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testEnrollment));
        when(classSessionRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testSession));
        when(authz.isAdmin()).thenReturn(true);
        when(attendanceRepository.save(any(Attendance.class)))
                .thenReturn(testAttendance);
        when(attendanceMapper.toResponse(testAttendance))
                .thenReturn(testResponse);

        // When — admin acts with a null reference id
        AttendanceResponse result = attendanceService.updateAttendanceStatus(1L, updateRequest, null);

        // Then — updated without consulting TeacherClass (the MAIN_TEACHER check was bypassed)
        assertThat(result).isNotNull();
        verify(attendanceRepository).save(any(Attendance.class));
        verifyNoInteractions(teacherClassRepository);
    }

    @Test
    @DisplayName("Should throw PermissionDeniedException when teacher is not MAIN_TEACHER")
    void shouldThrowPermissionDeniedWhenNotMainTeacher() {
        // Arrange
        UpdateAttendanceStatusRequest updateRequest =
                AttendanceTestDataBuilder.createUpdateStatusRequest(AttendanceStatus.EXCUSED);

        TeacherClass assistantTeacher = TeacherClass.builder()
                .teacherId(2L)
                .classId(1L)
                .role(TeacherClassRole.ASSISTANT)
                .build();

        when(attendanceRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testAttendance));
        when(enrollmentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testEnrollment));
        when(classSessionRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testSession));
        when(teacherClassRepository.findByTeacherIdAndClassId(2L, 1L))
                .thenReturn(Optional.of(assistantTeacher));

        // Act & Assert
        assertThatThrownBy(() -> attendanceService.updateAttendanceStatus(1L, updateRequest, 2L))
                .isInstanceOf(PermissionDeniedException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("ONLY_MAIN_TEACHER"));

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get student attendance statistics")
    void shouldGetStudentAttendanceStats() {
        // Arrange
        Page<Enrollment> enrollmentPage = new PageImpl<>(List.of(testEnrollment));
        when(enrollmentRepository.findByStudentIdAndDeletedFalse(eq(1L), any(Pageable.class)))
                .thenReturn(enrollmentPage);
        when(attendanceRepository.countByEnrollmentIdAndDeletedFalse(1L))
                .thenReturn(10L);
        when(attendanceRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
                1L, AttendanceStatus.PRESENT))
                .thenReturn(8L);
        when(attendanceRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
                1L, AttendanceStatus.ABSENT))
                .thenReturn(1L);
        when(attendanceRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
                1L, AttendanceStatus.LATE))
                .thenReturn(1L);
        when(attendanceRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
                1L, AttendanceStatus.EXCUSED))
                .thenReturn(0L);
        when(attendanceRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
                1L, AttendanceStatus.MAKEUP))
                .thenReturn(0L);

        // Act
        AttendanceStatsResponse result = attendanceService.getStudentAttendanceStats(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTargetId()).isEqualTo(1L);
        assertThat(result.getTargetType()).isEqualTo("STUDENT");
        assertThat(result.getTotalSessions()).isEqualTo(10L);
        assertThat(result.getPresentCount()).isEqualTo(8L);
        // BR-ATT-008 (GAP-994): rate = (PRESENT 8 + LATE 1) / total 10 = 90%
        assertThat(result.getAttendanceRate()).isEqualTo(90.0);
    }

    @Test
    @DisplayName("Should delete attendance")
    void shouldDeleteAttendance() {
        // Arrange
        when(attendanceRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testAttendance));
        when(attendanceRepository.save(any(Attendance.class)))
                .thenReturn(testAttendance);

        // Act
        attendanceService.deleteAttendance(1L);

        // Assert
        verify(attendanceRepository).save(any(Attendance.class));
    }
}
