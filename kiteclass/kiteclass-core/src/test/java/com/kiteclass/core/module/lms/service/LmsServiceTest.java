package com.kiteclass.core.module.lms.service;

import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.lms.dto.request.CreateCourseModuleRequest;
import com.kiteclass.core.module.lms.dto.request.CreateLessonRequest;
import com.kiteclass.core.module.lms.dto.response.CourseModuleDetailResponse;
import com.kiteclass.core.module.lms.dto.response.CourseModuleResponse;
import com.kiteclass.core.module.lms.dto.response.LessonDetailResponse;
import com.kiteclass.core.module.lms.dto.response.LessonResponse;
import com.kiteclass.core.module.lms.entity.CourseModule;
import com.kiteclass.core.module.lms.entity.Lesson;
import com.kiteclass.core.module.lms.mapper.LmsMapper;
import com.kiteclass.core.module.lms.repository.CourseModuleRepository;
import com.kiteclass.core.module.lms.repository.LearningResourceRepository;
import com.kiteclass.core.module.lms.repository.LessonRepository;
import com.kiteclass.core.common.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LmsServiceImpl}.
 *
 * @author KiteClass Team
 * @since 2.9.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LmsService Tests")
class LmsServiceTest {

    @Mock
    private CourseModuleRepository courseModuleRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LearningResourceRepository learningResourceRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private com.kiteclass.core.module.enrollment.repository.EnrollmentRepository enrollmentRepository;

    @Mock
    private com.kiteclass.core.module.clazz.repository.ClassRepository classRepository;

    @Mock
    private LmsMapper lmsMapper;

    @InjectMocks
    private LmsServiceImpl lmsService;

    private Course testCourse;
    private CourseModule testModule;
    private Lesson testLesson;
    private Lesson trialLesson;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();

        testCourse = Course.builder()
                .name("Test Course")
                .code("TEST-001")
                .teacherId(100L)
                .status(CourseStatus.PUBLISHED)
                .build();
        testCourse.setId(1L);
        testCourse.setInstanceId(tenantId);

        testModule = CourseModule.builder()
                .courseId(1L)
                .title("Module 1")
                .description("Test module")
                .orderNumber(1)
                .build();
        testModule.setId(1L);
        testModule.setInstanceId(tenantId);

        trialLesson = Lesson.builder()
                .moduleId(1L)
                .title("Trial Lesson")
                .content("Trial content")
                .isTrial(true)
                .orderNumber(1)
                .build();
        trialLesson.setId(1L);
        trialLesson.setInstanceId(tenantId);

        testLesson = Lesson.builder()
                .moduleId(1L)
                .title("Paid Lesson")
                .content("Paid content")
                .isTrial(false)
                .orderNumber(2)
                .build();
        testLesson.setId(2L);
        testLesson.setInstanceId(tenantId);
    }

    @AfterEach
    void tearDown() {
        // GAP-1118: ensure the thread-local tenant never leaks across tests.
        TenantContext.clear();
    }

    // ==================== Guest Access Tests ====================

    @Test
    @DisplayName("getCourseStructurePublic - should return trial lessons only")
    void getCourseStructurePublic_shouldReturnTrialLessonsOnly() {
        // Given
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
        when(courseModuleRepository.findByCourseIdAndDeletedFalseOrderByOrderNumber(1L))
                .thenReturn(List.of(testModule));
        when(lessonRepository.findByModuleIdAndIsTrialTrueAndDeletedFalseOrderByOrderNumber(1L))
                .thenReturn(List.of(trialLesson));
        when(lmsMapper.toLessonResponseList(anyList())).thenReturn(List.of());

        // When
        List<CourseModuleDetailResponse> result = lmsService.getCourseStructurePublic(1L);

        // Then
        assertThat(result).isNotNull();
        verify(courseRepository).findById(1L);
        verify(lessonRepository).findByModuleIdAndIsTrialTrueAndDeletedFalseOrderByOrderNumber(1L);
        verify(lessonRepository, never()).findByModuleIdAndDeletedFalseOrderByOrderNumber(anyLong());
    }

    @Test
    @DisplayName("getCourseStructurePublic - should throw ValidationException if course not published")
    void getCourseStructurePublic_shouldThrowValidationException_whenCourseNotPublished() {
        // Given
        testCourse.setStatus(CourseStatus.DRAFT);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

        // When & Then
        assertThatThrownBy(() -> lmsService.getCourseStructurePublic(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("COURSE_NOT_PUBLISHED");
    }

    @Test
    @DisplayName("getLessonPublic - should allow access to trial lesson")
    void getLessonPublic_shouldAllowAccess_whenLessonIsTrial() {
        // Given
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(trialLesson));
        when(lessonRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(trialLesson));
        when(learningResourceRepository.findByLessonIdAndDeletedFalse(1L)).thenReturn(List.of());
        when(lmsMapper.toResourceResponseList(anyList())).thenReturn(List.of());

        // When
        LessonDetailResponse result = lmsService.getLessonPublic(1L);

        // Then
        assertThat(result).isNotNull();
        verify(lessonRepository).findByIdAndDeletedFalse(1L);
    }

    @Test
    @DisplayName("getLessonPublic - should deny access to paid lesson")
    void getLessonPublic_shouldDenyAccess_whenLessonNotTrial() {
        // Given
        when(lessonRepository.findById(2L)).thenReturn(Optional.of(testLesson));
        when(lessonRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(testLesson));

        // When & Then
        assertThatThrownBy(() -> lmsService.getLessonPublic(2L))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("TRIAL_LESSON_REQUIRED");
    }

    @Test
    @DisplayName("getLessonPublic - should NOT leak tenant context onto the pooled thread (GAP-1118)")
    void getLessonPublic_shouldRestoreTenantContext_forGuest() {
        // Given - guest request: no tenant set on the thread before the call
        assertThat(TenantContext.isSet()).isFalse();
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(trialLesson));
        when(lessonRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(trialLesson));
        when(learningResourceRepository.findByLessonIdAndDeletedFalse(1L)).thenReturn(List.of());
        when(lmsMapper.toResourceResponseList(anyList())).thenReturn(List.of());

        // When
        lmsService.getLessonPublic(1L);

        // Then - the guest's resolved instanceId must be cleared, not leaked to the next request
        assertThat(TenantContext.isSet()).isFalse();
    }

    @Test
    @DisplayName("getLessonPublic - should restore a pre-existing tenant after the call (GAP-1118)")
    void getLessonPublic_shouldRestorePreExistingTenant() {
        // Given - a tenant was already active before the call (authenticated context)
        UUID preExisting = UUID.randomUUID();
        TenantContext.setCurrentTenant(preExisting);
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(trialLesson));
        when(lessonRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(trialLesson));
        when(learningResourceRepository.findByLessonIdAndDeletedFalse(1L)).thenReturn(List.of());
        when(lmsMapper.toResourceResponseList(anyList())).thenReturn(List.of());

        // When
        lmsService.getLessonPublic(1L);

        // Then - original tenant preserved
        assertThat(TenantContext.isSet()).isTrue();
        assertThat(TenantContext.getCurrentTenant()).isEqualTo(preExisting);
    }

    // ==================== Student Access Tests ====================

    @Test
    @DisplayName("getCourseStructureForStudent - enrolled student gets full content for paid lessons (GAP-1115)")
    void getCourseStructureForStudent_enrolled_shouldReturnFullContent() {
        // Given - student IS enrolled in a class of the course
        Long userId = 200L;
        Long classId = 1000L;
        com.kiteclass.core.module.clazz.entity.Class testClass =
                com.kiteclass.core.module.clazz.entity.Class.builder().courseId(1L).name("Class").build();
        testClass.setId(classId);

        when(courseModuleRepository.findByCourseIdAndDeletedFalseOrderByOrderNumber(1L))
                .thenReturn(List.of(testModule));
        when(lessonRepository.findByModuleIdAndDeletedFalseOrderByOrderNumber(1L))
                .thenReturn(List.of(trialLesson, testLesson));
        when(classRepository.findByCourseIdAndDeletedFalse(eq(1L), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(testClass)));
        when(enrollmentRepository.existsByStudentIdAndClassIdAndStatusAndDeletedFalse(
                userId, classId, com.kiteclass.core.common.constant.EnrollmentStatus.ACTIVE))
                .thenReturn(true);
        when(lmsMapper.toLessonResponse(trialLesson)).thenReturn(
                LessonResponse.builder().id(1L).title("Trial").isTrial(true).content("Trial body").build());
        when(lmsMapper.toLessonResponse(testLesson)).thenReturn(
                LessonResponse.builder().id(2L).title("Paid").isTrial(false)
                        .content("Paid body").videoUrl("http://video").build());

        // When
        List<CourseModuleDetailResponse> result = lmsService.getCourseStructureForStudent(1L, userId);

        // Then - outline contains both lessons; enrolled student keeps full paid body
        assertThat(result).hasSize(1);
        List<LessonResponse> lessons = result.get(0).lessons();
        assertThat(lessons).hasSize(2);
        LessonResponse paid = lessons.stream().filter(l -> l.id().equals(2L)).findFirst().orElseThrow();
        assertThat(paid.content()).isEqualTo("Paid body");
        assertThat(paid.videoUrl()).isEqualTo("http://video");
    }

    @Test
    @DisplayName("getCourseStructureForStudent - non-enrolled student gets paid content stripped, trial intact (GAP-1115)")
    void getCourseStructureForStudent_notEnrolled_shouldStripPaidContent() {
        // Given - course has NO classes → student is not enrolled
        Long userId = 200L;
        when(courseModuleRepository.findByCourseIdAndDeletedFalseOrderByOrderNumber(1L))
                .thenReturn(List.of(testModule));
        when(lessonRepository.findByModuleIdAndDeletedFalseOrderByOrderNumber(1L))
                .thenReturn(List.of(trialLesson, testLesson));
        when(classRepository.findByCourseIdAndDeletedFalse(eq(1L), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        when(lmsMapper.toLessonResponse(trialLesson)).thenReturn(
                LessonResponse.builder().id(1L).title("Trial").isTrial(true).content("Trial body").build());
        when(lmsMapper.toLessonResponse(testLesson)).thenReturn(
                LessonResponse.builder().id(2L).title("Paid").isTrial(false)
                        .content("Paid body").videoUrl("http://video").estimatedDuration(30).build());

        // When
        List<CourseModuleDetailResponse> result = lmsService.getCourseStructureForStudent(1L, userId);

        // Then - full outline still returned (UX), but paid BODY paywalled
        assertThat(result).hasSize(1);
        List<LessonResponse> lessons = result.get(0).lessons();
        assertThat(lessons).hasSize(2);

        LessonResponse trial = lessons.stream().filter(l -> l.id().equals(1L)).findFirst().orElseThrow();
        assertThat(trial.content()).isEqualTo("Trial body");  // trial preview kept

        LessonResponse paid = lessons.stream().filter(l -> l.id().equals(2L)).findFirst().orElseThrow();
        assertThat(paid.content()).isNull();   // paywalled
        assertThat(paid.videoUrl()).isNull();  // paywalled
        // metadata retained so FE can render the locked outline
        assertThat(paid.title()).isEqualTo("Paid");
        assertThat(paid.isTrial()).isFalse();
        assertThat(paid.estimatedDuration()).isEqualTo(30);
    }

    @Test
    @DisplayName("getLessonForStudent - should allow access when enrolled")
    void getLessonForStudent_enrolled_shouldAllowAccess() {
        // Setup: Student has ACTIVE enrollment in class
        Long userId = 200L;
        Long classId = 1000L;

        com.kiteclass.core.module.clazz.entity.Class testClass = com.kiteclass.core.module.clazz.entity.Class.builder()
                .courseId(1L)
                .name("Test Class")
                .build();
        testClass.setId(classId);

        when(lessonRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(testLesson));
        when(courseModuleRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testModule));
        when(classRepository.findByCourseIdAndDeletedFalse(eq(1L), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(testClass)));
        when(enrollmentRepository.existsByStudentIdAndClassIdAndStatusAndDeletedFalse(
                userId, classId, com.kiteclass.core.common.constant.EnrollmentStatus.ACTIVE))
                .thenReturn(true);
        when(learningResourceRepository.findByLessonIdAndDeletedFalse(2L)).thenReturn(List.of());
        when(lmsMapper.toResourceResponseList(anyList())).thenReturn(List.of());

        // When
        LessonDetailResponse result = lmsService.getLessonForStudent(2L, userId);

        // Then
        assertThat(result).isNotNull();
        verify(enrollmentRepository).existsByStudentIdAndClassIdAndStatusAndDeletedFalse(
                userId, classId, com.kiteclass.core.common.constant.EnrollmentStatus.ACTIVE);
    }

    @Test
    @DisplayName("getLessonForStudent - should deny access when not enrolled")
    void getLessonForStudent_notEnrolled_shouldDenyAccess() {
        // Setup: Student has NO enrollment
        Long userId = 200L;
        Long classId = 1000L;

        com.kiteclass.core.module.clazz.entity.Class testClass = com.kiteclass.core.module.clazz.entity.Class.builder()
                .courseId(1L)
                .name("Test Class")
                .build();
        testClass.setId(classId);

        when(lessonRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(testLesson));
        when(courseModuleRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testModule));
        when(classRepository.findByCourseIdAndDeletedFalse(eq(1L), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(testClass)));
        when(enrollmentRepository.existsByStudentIdAndClassIdAndStatusAndDeletedFalse(
                userId, classId, com.kiteclass.core.common.constant.EnrollmentStatus.ACTIVE))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> lmsService.getLessonForStudent(2L, userId))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("STUDENT_NOT_ENROLLED_IN_COURSE");
    }

    @Test
    @DisplayName("getLessonForStudent - should allow trial lesson without enrollment")
    void getLessonForStudent_trialLesson_shouldAllowAccess() {
        // Given - Trial lesson, no enrollment check needed
        Long userId = 200L;
        when(lessonRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(trialLesson));
        when(learningResourceRepository.findByLessonIdAndDeletedFalse(1L)).thenReturn(List.of());
        when(lmsMapper.toResourceResponseList(anyList())).thenReturn(List.of());

        // When
        LessonDetailResponse result = lmsService.getLessonForStudent(1L, userId);

        // Then
        assertThat(result).isNotNull();
        verify(enrollmentRepository, never()).existsByStudentIdAndClassIdAndStatusAndDeletedFalse(
                anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("getLessonForStudent - should deny when course has no classes")
    void getLessonForStudent_noCourseClasses_shouldDenyAccess() {
        // Setup: Course exists but has no classes
        Long userId = 200L;

        when(lessonRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(testLesson));
        when(courseModuleRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testModule));
        when(classRepository.findByCourseIdAndDeletedFalse(eq(1L), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        // When & Then
        assertThatThrownBy(() -> lmsService.getLessonForStudent(2L, userId))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("STUDENT_NOT_ENROLLED_IN_COURSE");
    }

    // ==================== Teacher CRUD Tests ====================

    @Test
    @DisplayName("createModule - should create module successfully for course owner")
    void createModule_shouldCreate_whenTeacherIsCourseOwner() {
        // Given
        Long teacherId = 100L;
        CreateCourseModuleRequest request = new CreateCourseModuleRequest(
                "New Module", "Description", 2);

        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testCourse));
        when(courseModuleRepository.existsByCourseIdAndOrderNumberAndDeletedFalse(1L, 2))
                .thenReturn(false);
        when(lmsMapper.toModuleEntity(request)).thenReturn(testModule);
        when(courseModuleRepository.save(any(CourseModule.class))).thenReturn(testModule);
        when(lmsMapper.toModuleResponse(testModule)).thenReturn(
                CourseModuleResponse.builder().id(1L).build());

        // When
        CourseModuleResponse result = lmsService.createModule(1L, request, teacherId);

        // Then
        assertThat(result).isNotNull();
        verify(courseModuleRepository).save(any(CourseModule.class));
    }

    @Test
    @DisplayName("createModule - should deny access for non-owner")
    void createModule_shouldDenyAccess_whenTeacherNotOwner() {
        // Given
        Long nonOwnerTeacherId = 999L;
        CreateCourseModuleRequest request = new CreateCourseModuleRequest(
                "New Module", "Description", 2);

        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testCourse));

        // When & Then
        assertThatThrownBy(() -> lmsService.createModule(1L, request, nonOwnerTeacherId))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("COURSE_OWNER_ONLY");
    }

    @Test
    @DisplayName("createModule - should throw ValidationException for duplicate order number")
    void createModule_shouldThrowValidationException_whenDuplicateOrderNumber() {
        // Given
        Long teacherId = 100L;
        CreateCourseModuleRequest request = new CreateCourseModuleRequest(
                "New Module", "Description", 1);

        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testCourse));
        when(courseModuleRepository.existsByCourseIdAndOrderNumberAndDeletedFalse(1L, 1))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> lmsService.createModule(1L, request, teacherId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("DUPLICATE_ORDER_NUMBER");
    }

    @Test
    @DisplayName("deleteModule - should fail when module has lessons")
    void deleteModule_shouldFail_whenModuleHasLessons() {
        // Given
        Long teacherId = 100L;
        when(courseModuleRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testModule));
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testCourse));
        when(lessonRepository.countByModuleIdAndDeletedFalse(1L)).thenReturn(2L);

        // When & Then
        assertThatThrownBy(() -> lmsService.deleteModule(1L, teacherId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("MODULE_HAS_LESSONS");

        verify(courseModuleRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLesson - should create lesson successfully")
    void createLesson_shouldCreate_whenValid() {
        // Given
        Long teacherId = 100L;
        CreateLessonRequest request = new CreateLessonRequest(
                "New Lesson", "Content", "http://video.url", false, 1, 30);

        when(courseModuleRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testModule));
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testCourse));
        when(lessonRepository.existsByModuleIdAndOrderNumberAndDeletedFalse(1L, 1))
                .thenReturn(false);
        when(lmsMapper.toLessonEntity(request)).thenReturn(testLesson);
        when(lessonRepository.save(any(Lesson.class))).thenReturn(testLesson);
        when(lmsMapper.toLessonResponse(testLesson)).thenReturn(
                LessonResponse.builder().id(2L).build());

        // When
        LessonResponse result = lmsService.createLesson(1L, request, teacherId);

        // Then
        assertThat(result).isNotNull();
        verify(lessonRepository).save(any(Lesson.class));
    }

    @Test
    @DisplayName("createLesson - should throw ValidationException for duplicate order number")
    void createLesson_shouldThrowValidationException_whenDuplicateOrderNumber() {
        // Given
        Long teacherId = 100L;
        CreateLessonRequest request = new CreateLessonRequest(
                "New Lesson", "Content", "http://video.url", false, 1, 30);

        when(courseModuleRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testModule));
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testCourse));
        when(lessonRepository.existsByModuleIdAndOrderNumberAndDeletedFalse(1L, 1))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> lmsService.createLesson(1L, request, teacherId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("DUPLICATE_ORDER_NUMBER");
    }
}
