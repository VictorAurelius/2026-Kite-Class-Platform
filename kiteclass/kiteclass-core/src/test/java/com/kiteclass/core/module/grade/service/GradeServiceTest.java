package com.kiteclass.core.module.grade.service;

import com.kiteclass.core.common.constant.GradeStatus;
import com.kiteclass.core.common.constant.TeacherClassRole;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.grade.dto.request.CreateGradeComponentRequest;
import com.kiteclass.core.module.grade.dto.request.FinalizeGradeRequest;
import com.kiteclass.core.module.grade.dto.response.GradeResponse;
import com.kiteclass.core.module.grade.entity.Grade;
import com.kiteclass.core.module.grade.entity.GradeComponent;
import com.kiteclass.core.module.grade.entity.GradingScale;
import com.kiteclass.core.module.grade.mapper.GradeMapper;
import com.kiteclass.core.module.grade.repository.GradeComponentRepository;
import com.kiteclass.core.module.grade.repository.GradeRepository;
import com.kiteclass.core.module.grade.repository.GradingScaleRepository;
import com.kiteclass.core.module.grade.repository.TranscriptRepository;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import com.kiteclass.core.module.teacher.entity.TeacherClass;
import com.kiteclass.core.module.teacher.repository.TeacherClassRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.kiteclass.core.common.security.AuthorizationBean;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GradeServiceImpl}.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GradeService Tests")
class GradeServiceTest {

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private GradeComponentRepository gradeComponentRepository;

    @Mock
    private GradingScaleRepository gradingScaleRepository;

    @Mock
    private TranscriptRepository transcriptRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherClassRepository teacherClassRepository;

    @Mock
    private GradeMapper gradeMapper;

    // GAP-1000/GAP-1301: GradeServiceImpl.validateTeacherPermission now delegates the admin/owner
    // bypass to AuthorizationBean.isAdmin() (OWNER-inclusive) instead of an inline SecurityContext
    // check. Unstubbed mock → isAdmin() returns false, so these tests exercise the non-admin
    // (MAIN_TEACHER-required) path as before.
    @Mock
    private AuthorizationBean authz;

    @InjectMocks
    private GradeServiceImpl gradeService;

    private UUID tenantId;
    private Long studentId;
    private Long classId;
    private Long teacherId;
    private Student testStudent;
    private Class testClass;
    private Grade testGrade;
    private GradeComponent testComponent;
    private GradingScale testGradingScale;
    private TeacherClass mainTeacherClass;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        studentId = 100L;
        classId = 200L;
        teacherId = 300L;

        // Set tenant context
        TenantContext.setCurrentTenant(tenantId);

        // GAP-1000: finalizeGrade now derives the acting teacher from the authenticated
        // principal (UserContext.getCurrentReferenceId()), NOT request.getTeacherId().
        // Seed the reference id as the MAIN_TEACHER so the MAIN_TEACHER permission check
        // resolves against mainTeacherClass in the finalize tests.
        UserContext.setCurrentReferenceId(teacherId);

        // Setup test student
        testStudent = Student.builder()
                .email("student@test.com")
                .name("John Doe")
                .build();
        testStudent.setId(studentId);
        testStudent.setInstanceId(tenantId);

        // Setup test class
        testClass = Class.builder()
                .name("Math 101")
                .build();
        testClass.setId(classId);
        testClass.setInstanceId(tenantId);

        // Setup test grade
        testGrade = Grade.builder()
                .studentId(studentId)
                .classId(classId)
                .status(GradeStatus.IN_PROGRESS)
                .passThreshold(BigDecimal.valueOf(50.0))
                .components(new ArrayList<>())
                .build();
        testGrade.setId(1L);
        testGrade.setInstanceId(tenantId);

        // Setup test component
        testComponent = GradeComponent.builder()
                .grade(testGrade)
                .componentName("Midterm Exam")
                .score(BigDecimal.valueOf(85))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(30))
                .build();
        testComponent.setId(1L);
        testComponent.calculateWeightedScore();

        // Setup grading scale
        testGradingScale = GradingScale.builder()
                .letterGrade("B+")
                .gpaValue(BigDecimal.valueOf(3.3))
                .minScore(BigDecimal.valueOf(87))
                .maxScore(BigDecimal.valueOf(89.99))
                .build();
        testGradingScale.setId(1L);
        testGradingScale.setInstanceId(tenantId);

        // Setup teacher-class relationship
        mainTeacherClass = TeacherClass.builder()
                .teacherId(teacherId)
                .classId(classId)
                .role(TeacherClassRole.MAIN_TEACHER)
                .build();
        mainTeacherClass.setId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.clear();
    }

    // ==================== Initialize Grade Tests ====================

    @Test
    @DisplayName("Should initialize grade successfully")
    void shouldInitializeGrade_whenValidInput() {
        // Arrange
        when(studentRepository.findByIdAndDeletedFalse(studentId)).thenReturn(Optional.of(testStudent));
        when(classRepository.findByIdAndDeletedFalse(classId)).thenReturn(Optional.of(testClass));
        when(gradeRepository.findByStudentIdAndClassIdAndDeletedFalse(studentId, classId))
                .thenReturn(Optional.empty());
        when(gradeRepository.save(any(Grade.class))).thenReturn(testGrade);
        when(gradeMapper.toResponse(any(Grade.class))).thenReturn(new GradeResponse());

        // Act
        GradeResponse response = gradeService.initializeGrade(studentId, classId);

        // Assert
        assertThat(response).isNotNull();
        verify(gradeRepository).save(any(Grade.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when student not found")
    void shouldThrowException_whenStudentNotFound() {
        // Arrange
        when(studentRepository.findByIdAndDeletedFalse(studentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> gradeService.initializeGrade(studentId, classId))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("STUDENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when class not found")
    void shouldThrowException_whenClassNotFound() {
        // Arrange
        when(studentRepository.findByIdAndDeletedFalse(studentId)).thenReturn(Optional.of(testStudent));
        when(classRepository.findByIdAndDeletedFalse(classId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> gradeService.initializeGrade(studentId, classId))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("CLASS_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should return existing grade when already exists")
    void shouldReturnExisting_whenGradeAlreadyExists() {
        // Arrange
        when(studentRepository.findByIdAndDeletedFalse(studentId)).thenReturn(Optional.of(testStudent));
        when(classRepository.findByIdAndDeletedFalse(classId)).thenReturn(Optional.of(testClass));
        when(gradeRepository.findByStudentIdAndClassIdAndDeletedFalse(studentId, classId))
                .thenReturn(Optional.of(testGrade));
        when(gradeMapper.toResponse(any(Grade.class))).thenReturn(new GradeResponse());

        // Act
        GradeResponse response = gradeService.initializeGrade(studentId, classId);

        // Assert
        assertThat(response).isNotNull();
        verify(gradeRepository).findByStudentIdAndClassIdAndDeletedFalse(studentId, classId);
    }

    // ==================== Get Grade Tests ====================

    @Test
    @DisplayName("Should get grade by ID successfully")
    void shouldGetGradeById_whenExists() {
        // Arrange
        when(gradeRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(testGrade));
        when(gradeMapper.toResponse(any(Grade.class))).thenReturn(new GradeResponse());

        // Act
        GradeResponse response = gradeService.getGradeById(1L);

        // Assert
        assertThat(response).isNotNull();
        verify(gradeRepository).findByIdAndDeletedFalse(1L);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when grade not found")
    void shouldThrowException_whenGradeNotFound() {
        // Arrange
        when(gradeRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> gradeService.getGradeById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("GRADE_NOT_FOUND"));
    }

    // ==================== Add Component Tests ====================

    @Test
    @DisplayName("Should add component successfully")
    void shouldAddComponent_whenValidInput() {
        // Arrange
        CreateGradeComponentRequest request = CreateGradeComponentRequest.builder()
                .gradeId(1L)
                .componentName("Midterm")
                .score(BigDecimal.valueOf(85))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(30))
                .build();

        when(gradeRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(testGrade));
        when(gradeComponentRepository.findByGradeIdAndComponentTypeAndComponentRefId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(gradeMapper.toEntity(any())).thenReturn(testComponent);
        when(gradeComponentRepository.save(any(GradeComponent.class))).thenReturn(testComponent);
        when(gradeMapper.toComponentResponse(any())).thenReturn(null);

        // Act
        gradeService.addOrUpdateComponent(request);

        // Assert
        verify(gradeComponentRepository).save(any(GradeComponent.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when grade is finalized")
    void shouldThrowException_whenGradeIsFinalized() {
        // Arrange
        testGrade.setStatus(GradeStatus.PASSED);
        testGrade.setFinalizedAt(java.time.LocalDateTime.now());

        CreateGradeComponentRequest request = CreateGradeComponentRequest.builder()
                .gradeId(1L)
                .componentName("Midterm")
                .score(BigDecimal.valueOf(85))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(30))
                .build();

        when(gradeRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(testGrade));

        // Act & Assert
        assertThatThrownBy(() -> gradeService.addOrUpdateComponent(request))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("CANNOT_MODIFY_FINALIZED_GRADE"));
    }

    // ==================== Calculate Final Score Tests ====================

    @Test
    @DisplayName("Should calculate final score successfully")
    void shouldCalculateFinalScore_whenComponentsExist() {
        // Arrange
        testGrade.getComponents().add(testComponent);

        when(gradeRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(testGrade));
        when(gradingScaleRepository.findByInstanceIdAndScoreRange(any(), any()))
                .thenReturn(Optional.of(testGradingScale));
        when(gradeRepository.save(any(Grade.class))).thenReturn(testGrade);
        when(gradeMapper.toResponse(any(Grade.class))).thenReturn(new GradeResponse());

        // Act
        GradeResponse response = gradeService.calculateFinalScore(1L);

        // Assert
        assertThat(response).isNotNull();
        verify(gradeRepository).save(any(Grade.class));
    }

    // ==================== Finalize Grade Tests ====================

    @Test
    @DisplayName("Should finalize grade successfully when weights sum to 100")
    void shouldFinalizeGrade_whenWeightsValid() {
        // Arrange
        GradeComponent component1 = GradeComponent.builder()
                .grade(testGrade)
                .componentName("Midterm")
                .score(BigDecimal.valueOf(85))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(50))
                .build();
        component1.calculateWeightedScore();

        GradeComponent component2 = GradeComponent.builder()
                .grade(testGrade)
                .componentName("Final")
                .score(BigDecimal.valueOf(90))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(50))
                .build();
        component2.calculateWeightedScore();

        testGrade.getComponents().add(component1);
        testGrade.getComponents().add(component2);

        FinalizeGradeRequest request = FinalizeGradeRequest.builder()
                .teacherId(teacherId)
                .comments("Good work!")
                .build();

        when(gradeRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(testGrade));
        when(teacherClassRepository.findByTeacherIdAndClassId(teacherId, classId))
                .thenReturn(Optional.of(mainTeacherClass));
        when(gradingScaleRepository.findByInstanceIdAndScoreRange(any(), any()))
                .thenReturn(Optional.of(testGradingScale));
        when(gradeRepository.save(any(Grade.class))).thenReturn(testGrade);
        when(gradeMapper.toResponse(any(Grade.class))).thenReturn(new GradeResponse());

        // Act
        GradeResponse response = gradeService.finalizeGrade(1L, request);

        // Assert
        assertThat(response).isNotNull();
        verify(gradeRepository).save(any(Grade.class));
    }

    @Test
    @DisplayName("GAP-1000/GAP-1301 — ADMIN/OWNER bypasses MAIN_TEACHER check (OWNER-inclusive isAdmin)")
    void shouldBypassMainTeacherCheck_whenAdmin() {
        // Given — ADMIN/OWNER carries no numeric reference id and has no TeacherClass row;
        // AuthorizationBean.isAdmin() (now OWNER-inclusive — GAP-1301) lets them bypass the
        // per-class MAIN_TEACHER check in validateTeacherPermission.
        GradeComponent component1 = GradeComponent.builder()
                .grade(testGrade)
                .componentName("Midterm")
                .score(BigDecimal.valueOf(85))
                .maxScore(BigDecimal.valueOf(100))
                .weightPercent(BigDecimal.valueOf(100))
                .build();
        component1.calculateWeightedScore();
        testGrade.getComponents().add(component1);

        FinalizeGradeRequest request = FinalizeGradeRequest.builder()
                .comments("Finalized by tenant admin")
                .build();

        when(gradeRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(testGrade));
        when(authz.isAdmin()).thenReturn(true);
        when(gradingScaleRepository.findByInstanceIdAndScoreRange(any(), any()))
                .thenReturn(Optional.of(testGradingScale));
        when(gradeRepository.save(any(Grade.class))).thenReturn(testGrade);
        when(gradeMapper.toResponse(any(Grade.class))).thenReturn(new GradeResponse());

        // When — admin acts with a null teacher reference id
        GradeResponse response = gradeService.finalizeGrade(1L, request);

        // Then — finalized without consulting TeacherClass (the MAIN_TEACHER check was bypassed)
        assertThat(response).isNotNull();
        verify(gradeRepository).save(any(Grade.class));
        verifyNoInteractions(teacherClassRepository);
    }

    @Test
    @DisplayName("Should throw ValidationException when weights do not sum to 100")
    void shouldThrowException_whenWeightsInvalid() {
        // Arrange
        testGrade.getComponents().add(testComponent); // Only 30% weight

        FinalizeGradeRequest request = FinalizeGradeRequest.builder()
                .teacherId(teacherId)
                .build();

        when(gradeRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(testGrade));
        when(teacherClassRepository.findByTeacherIdAndClassId(teacherId, classId))
                .thenReturn(Optional.of(mainTeacherClass));

        // Act & Assert
        assertThatThrownBy(() -> gradeService.finalizeGrade(1L, request))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("GRADE_WEIGHTS_MUST_SUM_TO_100"));
    }

    @Test
    @DisplayName("Should throw PermissionDeniedException when not main teacher")
    void shouldThrowException_whenNotMainTeacher() {
        // Arrange
        TeacherClass assistantTeacher = TeacherClass.builder()
                .teacherId(teacherId)
                .classId(classId)
                .role(TeacherClassRole.ASSISTANT)
                .build();

        FinalizeGradeRequest request = FinalizeGradeRequest.builder()
                .teacherId(teacherId)
                .build();

        when(gradeRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(testGrade));
        when(teacherClassRepository.findByTeacherIdAndClassId(teacherId, classId))
                .thenReturn(Optional.of(assistantTeacher));

        // Act & Assert
        assertThatThrownBy(() -> gradeService.finalizeGrade(1L, request))
                .isInstanceOf(PermissionDeniedException.class)
                .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("ONLY_MAIN_TEACHER"));
    }

    @Test
    @DisplayName("Should throw ValidationException when grade already finalized")
    void shouldThrowException_whenGradeAlreadyFinalized() {
        // Arrange
        testGrade.setStatus(GradeStatus.PASSED);
        testGrade.setFinalizedAt(java.time.LocalDateTime.now());

        FinalizeGradeRequest request = FinalizeGradeRequest.builder()
                .teacherId(teacherId)
                .build();

        when(gradeRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(testGrade));
        when(teacherClassRepository.findByTeacherIdAndClassId(teacherId, classId))
                .thenReturn(Optional.of(mainTeacherClass));

        // Act & Assert
        assertThatThrownBy(() -> gradeService.finalizeGrade(1L, request))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("GRADE_ALREADY_FINALIZED"));
    }

    // ==================== Unfinalize Grade Tests ====================

    @Test
    @DisplayName("Should unfinalize grade successfully")
    void shouldUnfinalizeGrade_whenFinalized() {
        // Arrange
        testGrade.setStatus(GradeStatus.PASSED);
        testGrade.setFinalizedAt(java.time.LocalDateTime.now());

        when(gradeRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(testGrade));
        when(gradeRepository.save(any(Grade.class))).thenReturn(testGrade);
        when(gradeMapper.toResponse(any(Grade.class))).thenReturn(new GradeResponse());

        // Act
        GradeResponse response = gradeService.unfinalizeGrade(1L);

        // Assert
        assertThat(response).isNotNull();
        verify(gradeRepository).save(any(Grade.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when grade not finalized")
    void shouldThrowException_whenGradeNotFinalized() {
        // Arrange
        when(gradeRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(testGrade));

        // Act & Assert
        assertThatThrownBy(() -> gradeService.unfinalizeGrade(1L))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("GRADE_NOT_FINALIZED"));
    }

    // ==================== Get Grades Tests ====================

    @Test
    @DisplayName("Should get grades by student")
    void shouldGetGradesByStudent() {
        // Arrange
        List<Grade> grades = List.of(testGrade);
        when(gradeRepository.findByStudentIdAndDeletedFalseOrderByCalculatedAtDesc(studentId))
                .thenReturn(grades);
        when(gradeMapper.toResponseList(any())).thenReturn(List.of());

        // Act
        List<GradeResponse> responses = gradeService.getGradesByStudent(studentId);

        // Assert
        assertThat(responses).isNotNull();
        verify(gradeRepository).findByStudentIdAndDeletedFalseOrderByCalculatedAtDesc(studentId);
    }

    @Test
    @DisplayName("Should get grades by class")
    void shouldGetGradesByClass() {
        // Arrange
        List<Grade> grades = List.of(testGrade);
        when(gradeRepository.findByClassIdAndDeletedFalseOrderByFinalScoreDesc(classId))
                .thenReturn(grades);
        when(gradeMapper.toSummaryResponseList(any())).thenReturn(List.of());

        // Act
        gradeService.getGradesByClass(classId);

        // Assert
        verify(gradeRepository).findByClassIdAndDeletedFalseOrderByFinalScoreDesc(classId);
    }
}
