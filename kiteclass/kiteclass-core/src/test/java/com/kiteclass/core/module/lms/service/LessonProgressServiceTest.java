package com.kiteclass.core.module.lms.service;

import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.lms.dto.response.CourseProgressResponse;
import com.kiteclass.core.module.lms.dto.response.LessonProgressResponse;
import com.kiteclass.core.module.lms.entity.CourseModule;
import com.kiteclass.core.module.lms.entity.Lesson;
import com.kiteclass.core.module.lms.entity.LessonProgress;
import com.kiteclass.core.module.lms.event.LessonCompletedEvent;
import com.kiteclass.core.module.lms.mapper.LmsMapper;
import com.kiteclass.core.module.lms.repository.CourseModuleRepository;
import com.kiteclass.core.module.lms.repository.LessonProgressRepository;
import com.kiteclass.core.module.lms.repository.LessonRepository;
import com.kiteclass.core.module.course.entity.Course;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LessonProgressServiceImpl}.
 *
 * @author KiteClass Team
 * @since 2.9.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LessonProgressService Tests")
class LessonProgressServiceTest {

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseModuleRepository courseModuleRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonAccessGuard lessonAccessGuard;

    @Mock
    private LmsMapper lmsMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LessonProgressServiceImpl lessonProgressService;

    private Lesson testLesson;
    private CourseModule testModule;
    private Course testCourse;
    private LessonProgress testProgress;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();

        testCourse = Course.builder()
                .name("Test Course")
                .code("TEST-001")
                .build();
        testCourse.setId(1L);
        testCourse.setInstanceId(tenantId);

        testModule = CourseModule.builder()
                .courseId(1L)
                .title("Module 1")
                .orderNumber(1)
                .build();
        testModule.setId(1L);
        testModule.setInstanceId(tenantId);

        testLesson = Lesson.builder()
                .moduleId(1L)
                .title("Test Lesson")
                .isTrial(false)
                .orderNumber(1)
                .build();
        testLesson.setId(1L);
        testLesson.setInstanceId(tenantId);

        testProgress = LessonProgress.builder()
                .userId(200L)
                .lessonId(1L)
                .completed(false)
                .progressPercent(0)
                .build();
        testProgress.setId(1L);
        testProgress.setInstanceId(tenantId);
    }

    // ==================== Complete Lesson Tests ====================

    @Test
    @DisplayName("completeLesson - should create new progress record when not exists")
    void completeLesson_shouldCreateNewProgress_whenNotExists() {
        // Given
        Long userId = 200L;
        when(lessonRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testLesson));
        when(courseModuleRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testModule));
        // GAP-1116: paid lesson requires enrollment; guard mock (void) defaults to "enrolled".
        when(lessonProgressRepository.findByUserIdAndLessonIdAndDeletedFalse(userId, 1L))
                .thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenReturn(testProgress);
        when(lmsMapper.toProgressResponse(any())).thenReturn(
                LessonProgressResponse.builder()
                        .id(1L)
                        .userId(userId)
                        .lessonId(1L)
                        .completed(true)
                        .progressPercent(100)
                        .build()
        );

        // When
        LessonProgressResponse result = lessonProgressService.completeLesson(1L, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.completed()).isTrue();
        assertThat(result.progressPercent()).isEqualTo(100);

        ArgumentCaptor<LessonProgress> progressCaptor = ArgumentCaptor.forClass(LessonProgress.class);
        verify(lessonProgressRepository).save(progressCaptor.capture());

        LessonProgress savedProgress = progressCaptor.getValue();
        assertThat(savedProgress.getCompleted()).isTrue();
        assertThat(savedProgress.getProgressPercent()).isEqualTo(100);
        assertThat(savedProgress.getCompletedAt()).isNotNull();

        // Verify event published
        verify(eventPublisher).publishEvent(any(LessonCompletedEvent.class));
    }

    @Test
    @DisplayName("completeLesson - should update existing progress when not completed")
    void completeLesson_shouldUpdateProgress_whenExistsAndNotCompleted() {
        // Given
        Long userId = 200L;
        when(lessonRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testLesson));
        when(courseModuleRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testModule));
        // GAP-1116: paid lesson requires enrollment; guard mock (void) defaults to "enrolled".
        when(lessonProgressRepository.findByUserIdAndLessonIdAndDeletedFalse(userId, 1L))
                .thenReturn(Optional.of(testProgress));
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenReturn(testProgress);
        when(lmsMapper.toProgressResponse(any())).thenReturn(
                LessonProgressResponse.builder()
                        .id(1L)
                        .userId(userId)
                        .lessonId(1L)
                        .completed(true)
                        .progressPercent(100)
                        .build()
        );

        // When
        LessonProgressResponse result = lessonProgressService.completeLesson(1L, userId);

        // Then
        assertThat(result).isNotNull();
        verify(lessonProgressRepository).save(testProgress);
        verify(eventPublisher).publishEvent(any(LessonCompletedEvent.class));
    }

    @Test
    @DisplayName("completeLesson - should be idempotent when already completed")
    void completeLesson_shouldBeIdempotent_whenAlreadyCompleted() {
        // Given
        Long userId = 200L;
        testProgress.markAsCompleted(); // Already completed

        when(lessonRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testLesson));
        when(courseModuleRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testModule));
        // GAP-1116: paid lesson requires enrollment; guard mock (void) defaults to "enrolled".
        when(lessonProgressRepository.findByUserIdAndLessonIdAndDeletedFalse(userId, 1L))
                .thenReturn(Optional.of(testProgress));
        when(lmsMapper.toProgressResponse(any())).thenReturn(
                LessonProgressResponse.builder()
                        .id(1L)
                        .userId(userId)
                        .lessonId(1L)
                        .completed(true)
                        .progressPercent(100)
                        .build()
        );

        // When
        LessonProgressResponse result = lessonProgressService.completeLesson(1L, userId);

        // Then
        assertThat(result).isNotNull();
        // Should NOT save again (idempotent - BR-LMS-010)
        verify(lessonProgressRepository, never()).save(any());
        // Should NOT publish event again
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("completeLesson - should throw EntityNotFoundException when lesson not found")
    void completeLesson_shouldThrowException_whenLessonNotFound() {
        // Given
        Long userId = 200L;
        when(lessonRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> lessonProgressService.completeLesson(999L, userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("LESSON_NOT_FOUND");
    }

    @Test
    @DisplayName("completeLesson - should deny completing a paid lesson when not enrolled (GAP-1116)")
    void completeLesson_shouldDenyPaidLesson_whenNotEnrolled() {
        // Given - paid lesson, guard rejects (student not enrolled)
        Long userId = 200L;
        when(lessonRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testLesson));
        when(courseModuleRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testModule));
        doThrow(new com.kiteclass.core.common.exception.PermissionDeniedException("STUDENT_NOT_ENROLLED_IN_COURSE"))
                .when(lessonAccessGuard).verifyStudentEnrollment(userId, 1L);

        // When & Then
        assertThatThrownBy(() -> lessonProgressService.completeLesson(1L, userId))
                .isInstanceOf(com.kiteclass.core.common.exception.PermissionDeniedException.class)
                .hasMessageContaining("STUDENT_NOT_ENROLLED_IN_COURSE");

        // Progress must NOT be recorded for a paywalled lesson
        verify(lessonProgressRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("completeLesson - should allow completing a trial lesson without enrollment (GAP-1116)")
    void completeLesson_shouldAllowTrialLesson_withoutEnrollment() {
        // Given - trial lesson: no enrollment check, no module lookup
        Long userId = 200L;
        Lesson trialLesson = Lesson.builder()
                .moduleId(1L).title("Trial").isTrial(true).orderNumber(1).build();
        trialLesson.setId(5L);
        trialLesson.setInstanceId(tenantId);

        when(lessonRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(trialLesson));
        when(lessonProgressRepository.findByUserIdAndLessonIdAndDeletedFalse(userId, 5L))
                .thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenReturn(testProgress);
        when(lmsMapper.toProgressResponse(any())).thenReturn(
                LessonProgressResponse.builder().id(1L).userId(userId).lessonId(5L)
                        .completed(true).progressPercent(100).build());

        // When
        LessonProgressResponse result = lessonProgressService.completeLesson(5L, userId);

        // Then - allowed; paywall guard never consulted for trial content
        assertThat(result).isNotNull();
        verify(lessonProgressRepository).save(any(LessonProgress.class));
        verify(lessonAccessGuard, never()).verifyStudentEnrollment(any(), any());
    }

    // ==================== Course Progress Tests ====================

    @Test
    @DisplayName("getCourseProgress - should calculate progress correctly")
    void getCourseProgress_shouldCalculateCorrectly() {
        // Given
        Long userId = 200L;
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testCourse));
        when(lessonRepository.countLessonsByCourseId(1L)).thenReturn(10L);
        when(lessonProgressRepository.countCompletedLessonsByCourseIdAndUserId(1L, userId))
                .thenReturn(7L);

        // When
        CourseProgressResponse result = lessonProgressService.getCourseProgress(1L, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.courseId()).isEqualTo(1L);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.totalLessons()).isEqualTo(10);
        assertThat(result.completedLessons()).isEqualTo(7);
        assertThat(result.progressPercent()).isEqualTo(70.0); // 7/10 * 100 = 70%
    }

    @Test
    @DisplayName("getCourseProgress - should return 0% when no lessons")
    void getCourseProgress_shouldReturnZero_whenNoLessons() {
        // Given
        Long userId = 200L;
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testCourse));
        when(lessonRepository.countLessonsByCourseId(1L)).thenReturn(0L);
        when(lessonProgressRepository.countCompletedLessonsByCourseIdAndUserId(1L, userId))
                .thenReturn(0L);

        // When
        CourseProgressResponse result = lessonProgressService.getCourseProgress(1L, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalLessons()).isEqualTo(0);
        assertThat(result.completedLessons()).isEqualTo(0);
        assertThat(result.progressPercent()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getCourseProgress - should return 100% when all completed")
    void getCourseProgress_shouldReturn100Percent_whenAllCompleted() {
        // Given
        Long userId = 200L;
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testCourse));
        when(lessonRepository.countLessonsByCourseId(1L)).thenReturn(5L);
        when(lessonProgressRepository.countCompletedLessonsByCourseIdAndUserId(1L, userId))
                .thenReturn(5L);

        // When
        CourseProgressResponse result = lessonProgressService.getCourseProgress(1L, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalLessons()).isEqualTo(5);
        assertThat(result.completedLessons()).isEqualTo(5);
        assertThat(result.progressPercent()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("getCourseProgress - should round to 1 decimal place")
    void getCourseProgress_shouldRoundToOneDecimal() {
        // Given
        Long userId = 200L;
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testCourse));
        when(lessonRepository.countLessonsByCourseId(1L)).thenReturn(3L);
        when(lessonProgressRepository.countCompletedLessonsByCourseIdAndUserId(1L, userId))
                .thenReturn(1L);

        // When
        CourseProgressResponse result = lessonProgressService.getCourseProgress(1L, userId);

        // Then
        assertThat(result).isNotNull();
        // 1/3 * 100 = 33.333... should round to 33.3
        assertThat(result.progressPercent()).isEqualTo(33.3);
    }

    @Test
    @DisplayName("getCourseProgress - should throw EntityNotFoundException when course not found")
    void getCourseProgress_shouldThrowException_whenCourseNotFound() {
        // Given
        Long userId = 200L;
        when(courseRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> lessonProgressService.getCourseProgress(999L, userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("COURSE_NOT_FOUND");
    }

    // ==================== Get Lesson Progress Tests ====================

    @Test
    @DisplayName("getLessonProgress - should return progress when exists")
    void getLessonProgress_shouldReturnProgress_whenExists() {
        // Given
        Long userId = 200L;
        when(lessonRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testLesson));
        when(lessonProgressRepository.findByUserIdAndLessonIdAndDeletedFalse(userId, 1L))
                .thenReturn(Optional.of(testProgress));
        when(lmsMapper.toProgressResponse(testProgress)).thenReturn(
                LessonProgressResponse.builder()
                        .id(1L)
                        .userId(userId)
                        .lessonId(1L)
                        .build()
        );

        // When
        LessonProgressResponse result = lessonProgressService.getLessonProgress(1L, userId);

        // Then
        assertThat(result).isNotNull();
        verify(lmsMapper).toProgressResponse(testProgress);
    }

    @Test
    @DisplayName("getLessonProgress - should return null when progress not exists")
    void getLessonProgress_shouldReturnNull_whenNotExists() {
        // Given
        Long userId = 200L;
        when(lessonRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testLesson));
        when(lessonProgressRepository.findByUserIdAndLessonIdAndDeletedFalse(userId, 1L))
                .thenReturn(Optional.empty());

        // When
        LessonProgressResponse result = lessonProgressService.getLessonProgress(1L, userId);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getLessonProgress - should throw EntityNotFoundException when lesson not found")
    void getLessonProgress_shouldThrowException_whenLessonNotFound() {
        // Given
        Long userId = 200L;
        when(lessonRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> lessonProgressService.getLessonProgress(999L, userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("LESSON_NOT_FOUND");
    }
}
