package com.kiteclass.core.module.enrollment.service;

import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.module.enrollment.dto.EnrollmentResponse;
import com.kiteclass.core.module.enrollment.dto.UpdateEnrollmentStatusRequest;
import com.kiteclass.core.module.enrollment.entity.Enrollment;
import com.kiteclass.core.module.enrollment.mapper.EnrollmentMapper;
import com.kiteclass.core.module.enrollment.repository.EnrollmentRepository;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EnrollmentServiceImpl}.
 *
 * @author KiteClass Team
 * @since 2.6.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentService Tests")
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private EnrollmentMapper enrollmentMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private Student testStudent;
    private Class testClass;
    private CreateEnrollmentRequest createRequest;
    private Enrollment testEnrollment;
    private EnrollmentResponse testResponse;

    @BeforeEach
    void setUp() {
        UUID tenantId = UUID.randomUUID();

        testStudent = Student.builder()
                .name("Test Student")
                .email("student@test.com")
                .build();
        testStudent.setId(1L);
        testStudent.setInstanceId(tenantId);

        testClass = Class.builder()
                .courseId(1L)
                .name("Test Class")
                .maxStudents(30)
                .currentEnrolled(0)
                .build();
        testClass.setId(1L);

        createRequest = CreateEnrollmentRequest.builder()
                .studentId(1L)
                .classId(1L)
                .tuitionAmount(new BigDecimal("1000.00"))
                .discountPercent(new BigDecimal("10.00"))
                .build();

        testEnrollment = Enrollment.builder()
                .studentId(1L)
                .classId(1L)
                .tuitionAmount(new BigDecimal("1000.00"))
                .discountPercent(new BigDecimal("10.00"))
                .finalAmount(new BigDecimal("900.00"))
                .status(EnrollmentStatus.PENDING_PAYMENT)
                .build();
        testEnrollment.setId(1L);
        testEnrollment.setInstanceId(tenantId);

        testResponse = EnrollmentResponse.builder()
                .id(1L)
                .studentId(1L)
                .classId(1L)
                .tuitionAmount(new BigDecimal("1000.00"))
                .discountPercent(new BigDecimal("10.00"))
                .finalAmount(new BigDecimal("900.00"))
                .status(EnrollmentStatus.PENDING_PAYMENT)
                .build();
    }

    @Test
    @DisplayName("Should enroll student successfully")
    void shouldEnrollStudentSuccessfully() {
        // Arrange
        when(studentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testStudent));
        when(classRepository.findByIdForEnrollmentWithLock(1L))
                .thenReturn(Optional.of(testClass));
        when(enrollmentRepository.findByStudentIdAndClassIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.empty());
        // testClass.currentEnrolled=0, maxStudents=30 → capacity check passes
        when(enrollmentMapper.toEntity(createRequest))
                .thenReturn(testEnrollment);
        when(enrollmentRepository.save(any(Enrollment.class)))
                .thenReturn(testEnrollment);
        when(classRepository.save(any(Class.class)))
                .thenReturn(testClass);
        when(enrollmentMapper.toResponse(testEnrollment))
                .thenReturn(testResponse);

        // Act
        EnrollmentResponse result = enrollmentService.enrollStudent(createRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStudentId()).isEqualTo(1L);
        assertThat(result.getClassId()).isEqualTo(1L);
        assertThat(result.getFinalAmount()).isEqualByComparingTo(new BigDecimal("900.00"));

        verify(enrollmentRepository).save(any(Enrollment.class));
        // Verify currentEnrolled counter was incremented (classRepository.save called)
        verify(classRepository).save(any(Class.class));
    }

    @Test
    @DisplayName("Should throw exception when student not found")
    void shouldThrowExceptionWhenStudentNotFound() {
        // Arrange
        when(studentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.enrollStudent(createRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("STUDENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should throw exception when class not found")
    void shouldThrowExceptionWhenClassNotFound() {
        // Arrange
        when(studentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testStudent));
        when(classRepository.findByIdForEnrollmentWithLock(1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.enrollStudent(createRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("CLASS_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should throw exception when student already enrolled")
    void shouldThrowExceptionWhenStudentAlreadyEnrolled() {
        // Arrange
        when(studentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testStudent));
        when(classRepository.findByIdForEnrollmentWithLock(1L))
                .thenReturn(Optional.of(testClass));
        when(enrollmentRepository.findByStudentIdAndClassIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(new Enrollment()));

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.enrollStudent(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("ENROLLMENT_DUPLICATE"));
    }

    @Test
    @DisplayName("Should throw exception when class is full")
    void shouldThrowExceptionWhenClassIsFull() {
        // Arrange — set currentEnrolled == maxStudents so capacity check fails
        testClass.setCurrentEnrolled(testClass.getMaxStudents()); // 30 == 30

        when(studentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testStudent));
        when(classRepository.findByIdForEnrollmentWithLock(1L))
                .thenReturn(Optional.of(testClass));
        when(enrollmentRepository.findByStudentIdAndClassIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.enrollStudent(createRequest))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("CLASS_FULL"));
    }

    @Test
    @DisplayName("Should throw exception when enrolling into a COMPLETED class (GAP-989)")
    void shouldThrowExceptionWhenClassCompleted() {
        // Arrange — class in a terminal lifecycle state must reject enrollment.
        testClass.setStatus(com.kiteclass.core.common.constant.ClassStatus.COMPLETED);

        when(studentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testStudent));
        when(classRepository.findByIdForEnrollmentWithLock(1L))
                .thenReturn(Optional.of(testClass));

        // Act & Assert — 400 CLASS_NOT_ENROLLABLE; no enrollment persisted.
        assertThatThrownBy(() -> enrollmentService.enrollStudent(createRequest))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("CLASS_NOT_ENROLLABLE"));
    }

    @Test
    @DisplayName("Should throw exception when enrolling into a CANCELLED class (GAP-989)")
    void shouldThrowExceptionWhenClassCancelled() {
        // Arrange
        testClass.setStatus(com.kiteclass.core.common.constant.ClassStatus.CANCELLED);

        when(studentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testStudent));
        when(classRepository.findByIdForEnrollmentWithLock(1L))
                .thenReturn(Optional.of(testClass));

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.enrollStudent(createRequest))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("CLASS_NOT_ENROLLABLE"));
    }

    @Test
    @DisplayName("Should enroll into an IN_PROGRESS class (GAP-989 — active state allowed)")
    void shouldEnrollIntoInProgressClass() {
        // Arrange — IN_PROGRESS is an enrollable (active) lifecycle state.
        testClass.setStatus(com.kiteclass.core.common.constant.ClassStatus.IN_PROGRESS);

        when(studentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testStudent));
        when(classRepository.findByIdForEnrollmentWithLock(1L))
                .thenReturn(Optional.of(testClass));
        when(enrollmentRepository.findByStudentIdAndClassIdAndDeletedFalse(1L, 1L))
                .thenReturn(Optional.empty());
        when(enrollmentMapper.toEntity(createRequest))
                .thenReturn(testEnrollment);
        when(enrollmentRepository.save(any(Enrollment.class)))
                .thenReturn(testEnrollment);
        when(classRepository.save(any(Class.class)))
                .thenReturn(testClass);
        when(enrollmentMapper.toResponse(testEnrollment))
                .thenReturn(testResponse);

        // Act
        EnrollmentResponse result = enrollmentService.enrollStudent(createRequest);

        // Assert — succeeds; enrollment persisted.
        assertThat(result).isNotNull();
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Should update enrollment status successfully")
    void shouldUpdateEnrollmentStatusSuccessfully() {
        // Arrange
        UpdateEnrollmentStatusRequest updateRequest = UpdateEnrollmentStatusRequest.builder()
                .status(EnrollmentStatus.ACTIVE)
                .build();

        when(enrollmentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class)))
                .thenReturn(testEnrollment);
        when(enrollmentMapper.toResponse(any(Enrollment.class)))
                .thenReturn(testResponse);

        // Act
        EnrollmentResponse result = enrollmentService.updateEnrollmentStatus(1L, updateRequest);

        // Assert
        assertThat(result).isNotNull();
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Should withdraw student successfully")
    void shouldWithdrawStudentSuccessfully() {
        // Arrange
        testEnrollment.setStatus(EnrollmentStatus.ACTIVE);
        testClass.setCurrentEnrolled(1); // non-zero so decrement fires

        when(enrollmentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class)))
                .thenReturn(testEnrollment);
        // Withdraw path decrements currentEnrolled on the Class row
        when(classRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testClass));
        when(classRepository.save(any(Class.class)))
                .thenReturn(testClass);
        when(enrollmentMapper.toResponse(any(Enrollment.class)))
                .thenReturn(testResponse);

        // Act
        EnrollmentResponse result = enrollmentService.withdrawStudent(1L);

        // Assert
        assertThat(result).isNotNull();
        verify(enrollmentRepository).save(argThat(enrollment ->
                enrollment.getStatus() == EnrollmentStatus.WITHDRAWN
        ));
        // Verify counter was decremented
        verify(classRepository).save(argThat(clazz -> clazz.getCurrentEnrolled() == 0));
    }

    @Test
    @DisplayName("Should throw exception when withdrawing already withdrawn enrollment")
    void shouldThrowExceptionWhenWithdrawingAlreadyWithdrawn() {
        // Arrange
        testEnrollment.setStatus(EnrollmentStatus.WITHDRAWN);

        when(enrollmentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(testEnrollment));

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.withdrawStudent(1L))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(e.getMessage())
                        .containsIgnoringCase("ENROLLMENT_ALREADY_WITHDRAWN"));
    }
}
