package com.kiteclass.core.module.assignment.service;

import com.kiteclass.core.common.constant.AssignmentStatus;
import com.kiteclass.core.common.constant.SubmissionStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.assignment.dto.request.CreateAssignmentRequest;
import com.kiteclass.core.module.assignment.dto.request.GradeSubmissionRequest;
import com.kiteclass.core.module.assignment.dto.request.SubmitAssignmentRequest;
import com.kiteclass.core.module.assignment.dto.response.AssignmentResponse;
import com.kiteclass.core.module.assignment.dto.response.SubmissionResponse;
import com.kiteclass.core.module.assignment.entity.Assignment;
import com.kiteclass.core.module.assignment.entity.Submission;
import com.kiteclass.core.module.assignment.event.AssignmentGradedEvent;
import com.kiteclass.core.module.assignment.mapper.AssignmentMapper;
import com.kiteclass.core.module.assignment.repository.AssignmentRepository;
import com.kiteclass.core.module.assignment.repository.SubmissionRepository;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.teacher.entity.TeacherClass;
import com.kiteclass.core.module.teacher.repository.TeacherClassRepository;
import com.kiteclass.core.common.constant.TeacherClassRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AssignmentServiceImpl}.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentService Tests")
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private TeacherClassRepository teacherClassRepository;

    @Mock
    private AssignmentMapper assignmentMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    private Class testClass;
    private Assignment testAssignment;
    private Submission testSubmission;
    private TeacherClass mainTeacherClass;
    private UUID tenantId;
    private Long mainTeacherId;
    private Long studentId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        mainTeacherId = 100L;
        studentId = 200L;

        // Set tenant context for tests
        TenantContext.setCurrentTenant(tenantId);

        testClass = Class.builder()
                .name("Math 101")
                .build();
        testClass.setId(1L);
        testClass.setInstanceId(tenantId);

        testAssignment = Assignment.builder()
                .classId(1L)
                .title("Homework 1")
                .description("Math homework")
                .dueDate(LocalDateTime.now().plusDays(7))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(20))
                .allowLateSubmission(true)
                .latePenaltyPercent(BigDecimal.valueOf(10))
                .status(AssignmentStatus.DRAFT)
                .build();
        testAssignment.setId(1L);
        testAssignment.setInstanceId(tenantId);

        testSubmission = Submission.builder()
                .assignmentId(1L)
                .studentId(studentId)
                .submissionDate(LocalDateTime.now())
                .status(SubmissionStatus.PENDING)
                .build();
        testSubmission.setId(1L);
        testSubmission.setInstanceId(tenantId);

        mainTeacherClass = TeacherClass.builder()
                .teacherId(mainTeacherId)
                .classId(1L)
                .role(TeacherClassRole.MAIN_TEACHER)
                .build();
        mainTeacherClass.setId(1L);
    }

    @AfterEach
    void tearDown() {
        // Clear tenant context to avoid interference with other tests
        TenantContext.clear();
    }

    // ==================== Create Assignment Tests ====================

    @Test
    @DisplayName("Should create assignment when teacher is main teacher")
    void shouldCreateAssignment_whenTeacherIsMainTeacher() {
        // Given
        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .classId(1L)
                .title("Homework 1")
                .dueDate(LocalDateTime.now().plusDays(7))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(20))
                .allowLateSubmission(true)
                .build();

        when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testClass));
        when(teacherClassRepository.findByTeacherIdAndClassId(mainTeacherId, 1L))
                .thenReturn(Optional.of(mainTeacherClass));
        when(assignmentMapper.toEntity(request)).thenReturn(testAssignment);
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(testAssignment);
        when(assignmentMapper.toResponse(testAssignment))
                .thenReturn(AssignmentResponse.builder().id(1L).build());

        // When
        AssignmentResponse result = assignmentService.createAssignment(request, mainTeacherId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    @DisplayName("Should throw PermissionDeniedException when teacher is not main teacher")
    void shouldThrowPermissionDeniedException_whenTeacherNotMainTeacher() {
        // Given
        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .classId(1L)
                .title("Homework 1")
                .dueDate(LocalDateTime.now().plusDays(7))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(20))
                .allowLateSubmission(true)
                .build();

        when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testClass));
        when(teacherClassRepository.findByTeacherIdAndClassId(999L, 1L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> assignmentService.createAssignment(request, 999L))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("TEACHER_NOT_IN_CLASS");
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when class not found")
    void shouldThrowEntityNotFoundException_whenClassNotFound() {
        // Given
        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .classId(999L)
                .title("Homework 1")
                .dueDate(LocalDateTime.now().plusDays(7))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(20))
                .allowLateSubmission(true)
                .build();

        when(classRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> assignmentService.createAssignment(request, mainTeacherId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("CLASS_NOT_FOUND");
    }

    // ==================== Publish Assignment Tests ====================

    @Test
    @DisplayName("Should publish assignment when status is DRAFT")
    void shouldPublishAssignment_whenStatusIsDraft() {
        // Given
        when(assignmentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testAssignment));
        when(teacherClassRepository.findByTeacherIdAndClassId(mainTeacherId, 1L))
                .thenReturn(Optional.of(mainTeacherClass));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(testAssignment);
        when(assignmentMapper.toResponse(testAssignment))
                .thenReturn(AssignmentResponse.builder().id(1L).build());

        // When
        AssignmentResponse result = assignmentService.publishAssignment(1L, mainTeacherId);

        // Then
        assertThat(result).isNotNull();
        verify(assignmentRepository).save(argThat(assignment ->
                assignment.getStatus() == AssignmentStatus.PUBLISHED));
    }

    @Test
    @DisplayName("Should throw ValidationException when assignment already published")
    void shouldThrowValidationException_whenAssignmentAlreadyPublished() {
        // Given
        testAssignment.setStatus(AssignmentStatus.PUBLISHED);
        when(assignmentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testAssignment));
        when(teacherClassRepository.findByTeacherIdAndClassId(mainTeacherId, 1L))
                .thenReturn(Optional.of(mainTeacherClass));

        // When & Then
        assertThatThrownBy(() -> assignmentService.publishAssignment(1L, mainTeacherId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("ASSIGNMENT_ALREADY_PUBLISHED");
    }

    // ==================== Submit Assignment Tests ====================

    @Test
    @DisplayName("Should submit assignment when assignment accepts submissions")
    void shouldSubmitAssignment_whenAssignmentAcceptsSubmissions() {
        // Given
        testAssignment.setStatus(AssignmentStatus.PUBLISHED);
        SubmitAssignmentRequest request = SubmitAssignmentRequest.builder()
                .assignmentId(1L)
                .contentUrl("https://s3.amazonaws.com/file.pdf")
                .notes("My submission")
                .build();

        when(assignmentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testAssignment));
        when(submissionRepository.findByAssignmentIdAndStudentIdAndDeletedFalse(1L, studentId))
                .thenReturn(Optional.empty());
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);
        when(assignmentMapper.toSubmissionResponse(any(Submission.class)))
                .thenReturn(SubmissionResponse.builder().id(1L).build());

        // When
        SubmissionResponse result = assignmentService.submitAssignment(request, studentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(submissionRepository).save(any(Submission.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when student already submitted")
    void shouldThrowValidationException_whenStudentAlreadySubmitted() {
        // Given
        testAssignment.setStatus(AssignmentStatus.PUBLISHED);
        SubmitAssignmentRequest request = SubmitAssignmentRequest.builder()
                .assignmentId(1L)
                .contentUrl("https://s3.amazonaws.com/file.pdf")
                .build();

        when(assignmentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testAssignment));
        when(submissionRepository.findByAssignmentIdAndStudentIdAndDeletedFalse(1L, studentId))
                .thenReturn(Optional.of(testSubmission));

        // When & Then
        assertThatThrownBy(() -> assignmentService.submitAssignment(request, studentId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("STUDENT_ALREADY_SUBMITTED");
    }

    @Test
    @DisplayName("Should throw ValidationException when assignment not accepting submissions")
    void shouldThrowValidationException_whenAssignmentNotAcceptingSubmissions() {
        // Given
        testAssignment.setStatus(AssignmentStatus.CLOSED);
        SubmitAssignmentRequest request = SubmitAssignmentRequest.builder()
                .assignmentId(1L)
                .contentUrl("https://s3.amazonaws.com/file.pdf")
                .build();

        when(assignmentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testAssignment));

        // When & Then
        assertThatThrownBy(() -> assignmentService.submitAssignment(request, studentId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("ASSIGNMENT_NOT_ACCEPTING_SUBMISSIONS");
    }

    // ==================== Grade Submission Tests ====================

    @Test
    @DisplayName("Should grade submission with late penalty")
    void shouldGradeSubmission_withLatePenalty() {
        // Given
        testAssignment.setDueDate(LocalDateTime.now().minusDays(1)); // Overdue
        testSubmission.setSubmissionDate(LocalDateTime.now()); // Late by 1 day

        GradeSubmissionRequest request = GradeSubmissionRequest.builder()
                .score(BigDecimal.valueOf(90))
                .feedback("Good work")
                .build();

        when(submissionRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testSubmission));
        when(assignmentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testAssignment));
        when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testClass));
        when(teacherClassRepository.findByTeacherIdAndClassId(mainTeacherId, 1L))
                .thenReturn(Optional.of(mainTeacherClass));
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);
        when(assignmentMapper.toSubmissionResponse(any(Submission.class)))
                .thenReturn(SubmissionResponse.builder().id(1L).build());

        // When
        SubmissionResponse result = assignmentService.gradeSubmission(1L, request, mainTeacherId);

        // Then
        assertThat(result).isNotNull();
        verify(submissionRepository).save(any(Submission.class));
        verify(eventPublisher).publishEvent(any(AssignmentGradedEvent.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when score exceeds max score")
    void shouldThrowValidationException_whenScoreExceedsMaxScore() {
        // Given
        GradeSubmissionRequest request = GradeSubmissionRequest.builder()
                .score(BigDecimal.valueOf(150)) // Exceeds max_score 100
                .feedback("Good work")
                .build();

        when(submissionRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testSubmission));
        when(assignmentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testAssignment));
        when(teacherClassRepository.findByTeacherIdAndClassId(mainTeacherId, 1L))
                .thenReturn(Optional.of(mainTeacherClass));

        // When & Then
        assertThatThrownBy(() -> assignmentService.gradeSubmission(1L, request, mainTeacherId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("SCORE_EXCEEDS_MAX_SCORE");
    }

    // ==================== Delete Assignment Tests ====================

    @Test
    @DisplayName("Should throw ValidationException when deleting assignment with submissions")
    void shouldThrowValidationException_whenDeletingAssignmentWithSubmissions() {
        // Given
        when(assignmentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testAssignment));
        when(teacherClassRepository.findByTeacherIdAndClassId(mainTeacherId, 1L))
                .thenReturn(Optional.of(mainTeacherClass));
        when(submissionRepository.countByAssignmentIdAndDeletedFalse(1L)).thenReturn(5L);

        // When & Then
        assertThatThrownBy(() -> assignmentService.deleteAssignment(1L, mainTeacherId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("CANNOT_DELETE_ASSIGNMENT_WITH_SUBMISSIONS");
    }

    @Test
    @DisplayName("Should delete assignment when no submissions")
    void shouldDeleteAssignment_whenNoSubmissions() {
        // Given
        when(assignmentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testAssignment));
        when(teacherClassRepository.findByTeacherIdAndClassId(mainTeacherId, 1L))
                .thenReturn(Optional.of(mainTeacherClass));
        when(submissionRepository.countByAssignmentIdAndDeletedFalse(1L)).thenReturn(0L);
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(testAssignment);

        // When
        assignmentService.deleteAssignment(1L, mainTeacherId);

        // Then
        verify(assignmentRepository).save(argThat(assignment -> assignment.getDeleted()));
    }

    // ==================== Get Methods Tests ====================

    @Test
    @DisplayName("Should get assignment by ID")
    void shouldGetAssignmentById() {
        // Given
        when(assignmentRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testAssignment));
        when(assignmentMapper.toResponse(testAssignment))
                .thenReturn(AssignmentResponse.builder().id(1L).build());

        // When
        AssignmentResponse result = assignmentService.getAssignmentById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should get assignments by class")
    void shouldGetAssignmentsByClass() {
        // Given
        when(assignmentRepository.findByClassIdAndDeletedFalseOrderByDueDateDesc(1L))
                .thenReturn(List.of(testAssignment));
        when(assignmentMapper.toResponseList(anyList()))
                .thenReturn(List.of(AssignmentResponse.builder().id(1L).build()));

        // When
        List<AssignmentResponse> results = assignmentService.getAssignmentsByClass(1L);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should get published assignments by class")
    void shouldGetPublishedAssignmentsByClass() {
        // Given
        testAssignment.setStatus(AssignmentStatus.PUBLISHED);
        when(assignmentRepository.findByClassIdAndStatusAndDeletedFalseOrderByDueDateDesc(
                1L, AssignmentStatus.PUBLISHED))
                .thenReturn(List.of(testAssignment));
        when(assignmentMapper.toResponseList(anyList()))
                .thenReturn(List.of(AssignmentResponse.builder().id(1L).build()));

        // When
        List<AssignmentResponse> results = assignmentService.getPublishedAssignmentsByClass(1L);

        // Then
        assertThat(results).hasSize(1);
    }
}
