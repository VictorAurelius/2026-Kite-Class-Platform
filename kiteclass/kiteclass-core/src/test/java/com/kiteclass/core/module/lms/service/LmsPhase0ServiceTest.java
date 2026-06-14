package com.kiteclass.core.module.lms.service;

import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.lms.dto.request.ReorderRequest;
import com.kiteclass.core.module.lms.dto.response.CompletionRosterResponse;
import com.kiteclass.core.module.lms.entity.CourseModule;
import com.kiteclass.core.module.lms.entity.Lesson;
import com.kiteclass.core.module.lms.entity.LessonProgress;
import com.kiteclass.core.module.lms.mapper.LmsMapper;
import com.kiteclass.core.module.lms.repository.CourseModuleRepository;
import com.kiteclass.core.module.lms.repository.LessonProgressRepository;
import com.kiteclass.core.module.lms.repository.LessonRepository;
import com.kiteclass.core.module.storage.constant.FileType;
import com.kiteclass.core.module.storage.dto.PresignedUploadRequest;
import com.kiteclass.core.module.storage.dto.PresignedUploadResponse;
import com.kiteclass.core.module.storage.service.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the LMS Phase0-BE gap-fill methods on {@link LmsServiceImpl}:
 * reorder (modules/lessons), completion roster, and presigned resource upload.
 *
 * @since GAP-1113 LMS Phase0-BE
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LmsService Phase0-BE Tests")
class LmsPhase0ServiceTest {

    @Mock private CourseModuleRepository courseModuleRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private StorageService storageService;
    @Mock private LmsMapper lmsMapper;
    @Mock private com.kiteclass.core.common.security.AuthorizationBean authz;

    @InjectMocks
    private LmsServiceImpl lmsService;

    private final Long teacherId = 100L;
    private final UUID tenantId = UUID.randomUUID();
    private Course course;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(tenantId);
        course = Course.builder()
                .name("C").code("C-1").teacherId(teacherId).status(CourseStatus.PUBLISHED).build();
        course.setId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private CourseModule module(long id, int order) {
        CourseModule m = CourseModule.builder().courseId(1L).title("M" + id).orderNumber(order).build();
        m.setId(id);
        return m;
    }

    private Lesson lesson(long id, long moduleId, int order) {
        Lesson l = Lesson.builder().moduleId(moduleId).title("L" + id).orderNumber(order).isTrial(false).build();
        l.setId(id);
        return l;
    }

    // -------------------- Reorder modules --------------------

    @Test
    @DisplayName("reorderModules swaps order numbers atomically")
    void reorderModules_swap() {
        CourseModule m10 = module(10L, 1);
        CourseModule m11 = module(11L, 2);
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(courseModuleRepository.findByCourseIdAndDeletedFalseOrderByOrderNumber(1L))
                .thenReturn(List.of(m10, m11));
        when(courseModuleRepository.saveAll(any())).thenReturn(List.of(m10, m11));

        ReorderRequest req = new ReorderRequest(List.of(
                new ReorderRequest.ReorderItem(10L, 2),
                new ReorderRequest.ReorderItem(11L, 1)));

        lmsService.reorderModules(1L, req, teacherId);

        assertThat(m10.getOrderNumber()).isEqualTo(2);
        assertThat(m11.getOrderNumber()).isEqualTo(1);
        // two-phase: park-negative + final-positive
        verify(courseModuleRepository, times(2)).saveAll(any());
        verify(courseModuleRepository, times(2)).flush();
    }

    @Test
    @DisplayName("reorderModules rejects incomplete set")
    void reorderModules_incompleteSet() {
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(courseModuleRepository.findByCourseIdAndDeletedFalseOrderByOrderNumber(1L))
                .thenReturn(List.of(module(10L, 1), module(11L, 2)));

        ReorderRequest req = new ReorderRequest(List.of(
                new ReorderRequest.ReorderItem(10L, 2)));  // missing 11

        assertThatThrownBy(() -> lmsService.reorderModules(1L, req, teacherId))
                .isInstanceOf(ValidationException.class);
        verify(courseModuleRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("reorderModules rejects duplicate order numbers")
    void reorderModules_duplicateOrder() {
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(courseModuleRepository.findByCourseIdAndDeletedFalseOrderByOrderNumber(1L))
                .thenReturn(List.of(module(10L, 1), module(11L, 2)));

        ReorderRequest req = new ReorderRequest(List.of(
                new ReorderRequest.ReorderItem(10L, 1),
                new ReorderRequest.ReorderItem(11L, 1)));  // both order 1

        assertThatThrownBy(() -> lmsService.reorderModules(1L, req, teacherId))
                .isInstanceOf(ValidationException.class);
        verify(courseModuleRepository, never()).saveAll(any());
    }

    // -------------------- Reorder lessons --------------------

    @Test
    @DisplayName("reorderLessons swaps order numbers atomically")
    void reorderLessons_swap() {
        CourseModule m = module(20L, 1);
        Lesson l30 = lesson(30L, 20L, 1);
        Lesson l31 = lesson(31L, 20L, 2);
        when(courseModuleRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(m));
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(lessonRepository.findByModuleIdAndDeletedFalseOrderByOrderNumber(20L))
                .thenReturn(List.of(l30, l31));
        when(lessonRepository.saveAll(any())).thenReturn(List.of(l30, l31));

        ReorderRequest req = new ReorderRequest(List.of(
                new ReorderRequest.ReorderItem(30L, 2),
                new ReorderRequest.ReorderItem(31L, 1)));

        lmsService.reorderLessons(20L, req, teacherId);

        assertThat(l30.getOrderNumber()).isEqualTo(2);
        assertThat(l31.getOrderNumber()).isEqualTo(1);
        verify(lessonRepository, times(2)).saveAll(any());
    }

    // -------------------- Completion roster --------------------

    @Test
    @DisplayName("getCompletionRoster aggregates per student with progress percent")
    void completionRoster_aggregates() {
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(lessonRepository.countLessonsByCourseId(1L)).thenReturn(4L);
        when(lessonProgressRepository.findCompletedProgressByCourseId(1L)).thenReturn(List.of(
                progress(200L, 1L), progress(200L, 2L), progress(201L, 1L)));

        CompletionRosterResponse roster = lmsService.getCompletionRoster(1L, teacherId);

        assertThat(roster.courseId()).isEqualTo(1L);
        assertThat(roster.totalLessons()).isEqualTo(4L);
        assertThat(roster.students()).hasSize(2);
        var s200 = roster.students().stream().filter(s -> s.userId().equals(200L)).findFirst().orElseThrow();
        assertThat(s200.completedLessons()).isEqualTo(2L);
        assertThat(s200.progressPercent()).isEqualTo(50.0);
        assertThat(s200.completedLessonIds()).containsExactlyInAnyOrder(1L, 2L);
        var s201 = roster.students().stream().filter(s -> s.userId().equals(201L)).findFirst().orElseThrow();
        assertThat(s201.completedLessons()).isEqualTo(1L);
        assertThat(s201.progressPercent()).isEqualTo(25.0);
    }

    private LessonProgress progress(long userId, long lessonId) {
        return LessonProgress.builder().userId(userId).lessonId(lessonId).completed(true).build();
    }

    // -------------------- Resource upload (presigned) --------------------

    @Test
    @DisplayName("generateResourceUploadUrl delegates to StorageService after ownership check")
    void uploadUrl_delegatesToStorage() {
        Lesson l = lesson(30L, 20L, 1);
        CourseModule m = module(20L, 1);
        when(lessonRepository.findByIdAndDeletedFalse(30L)).thenReturn(Optional.of(l));
        when(courseModuleRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(m));
        when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(course));

        PresignedUploadRequest req = new PresignedUploadRequest(
                "slides.pdf", 1024L, "application/pdf", FileType.DOCUMENT, null);
        PresignedUploadResponse expected = new PresignedUploadResponse(7L, "http://minio/put", Instant.now());
        when(storageService.generatePresignedUploadUrl(req, teacherId, tenantId)).thenReturn(expected);

        PresignedUploadResponse actual = lmsService.generateResourceUploadUrl(30L, req, teacherId);

        assertThat(actual).isEqualTo(expected);
        verify(storageService).generatePresignedUploadUrl(req, teacherId, tenantId);
    }
}
