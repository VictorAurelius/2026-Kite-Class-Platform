package com.kiteclass.core.module.lms;

import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.lms.entity.CourseModule;
import com.kiteclass.core.module.lms.entity.Lesson;
import com.kiteclass.core.module.lms.entity.LessonProgress;
import com.kiteclass.core.module.lms.repository.CourseModuleRepository;
import com.kiteclass.core.module.lms.repository.LessonProgressRepository;
import com.kiteclass.core.module.lms.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Lesson Progress tracking.
 * Tests the full stack: Controller → Service → Repository → Database.
 *
 * @author KiteClass Team
 * @since 2.9.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@DisplayName("Lesson Progress Integration Tests")
class LessonProgressIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseModuleRepository courseModuleRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    private Course testCourse;
    private CourseModule testModule;
    private Lesson lesson1;
    private Lesson lesson2;
    private Lesson lesson3;
    private final UUID tenantId = UUID.randomUUID();
    private final Long teacherId = 100L;
    private final Long studentId = 200L;

    @BeforeEach
    void setUp() {
        // Set tenant context
        TenantContext.setCurrentTenant(tenantId);

        // Create test course
        testCourse = Course.builder()
                .name("Progress Test Course")
                .code("PROG-001")
                .teacherId(teacherId)
                .status(CourseStatus.PUBLISHED)
                .price(new BigDecimal("1000.00"))
                .build();
        testCourse.setInstanceId(tenantId);
        testCourse = courseRepository.save(testCourse);

        // Create test module
        testModule = CourseModule.builder()
                .courseId(testCourse.getId())
                .title("Test Module")
                .orderNumber(1)
                .build();
        testModule.setInstanceId(tenantId);
        testModule = courseModuleRepository.save(testModule);

        // Create 3 lessons
        lesson1 = Lesson.builder()
                .moduleId(testModule.getId())
                .title("Lesson 1")
                .isTrial(true)
                .orderNumber(1)
                .build();
        lesson1.setInstanceId(tenantId);
        lesson1 = lessonRepository.save(lesson1);

        lesson2 = Lesson.builder()
                .moduleId(testModule.getId())
                .title("Lesson 2")
                .isTrial(true)
                .orderNumber(2)
                .build();
        lesson2.setInstanceId(tenantId);
        lesson2 = lessonRepository.save(lesson2);

        lesson3 = Lesson.builder()
                .moduleId(testModule.getId())
                .title("Lesson 3")
                .isTrial(true)
                .orderNumber(3)
                .build();
        lesson3.setInstanceId(tenantId);
        lesson3 = lessonRepository.save(lesson3);
    }

    // ==================== Complete Lesson Tests ====================

    @Test
    @DisplayName("Complete lesson - should create new progress record")
    void completeLesson_shouldCreateNewProgressRecord() throws Exception {
        mockMvc.perform(post("/api/v1/lms/progress/lessons/{lessonId}/complete", lesson1.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", studentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lessonId").value(lesson1.getId()))
                .andExpect(jsonPath("$.data.userId").value(studentId))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.progressPercent").value(100))
                .andExpect(jsonPath("$.data.completedAt").exists());

        // Verify database
        LessonProgress progress = lessonProgressRepository
                .findByUserIdAndLessonIdAndDeletedFalse(studentId, lesson1.getId())
                .orElseThrow();

        assertThat(progress.getCompleted()).isTrue();
        assertThat(progress.getProgressPercent()).isEqualTo(100);
        assertThat(progress.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Complete lesson - should be idempotent")
    void completeLesson_shouldBeIdempotent() throws Exception {
        // Complete lesson first time
        mockMvc.perform(post("/api/v1/lms/progress/lessons/{lessonId}/complete", lesson1.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", studentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        long countAfterFirst = lessonProgressRepository.count();

        // Complete lesson second time (should be idempotent - BR-LMS-010)
        mockMvc.perform(post("/api/v1/lms/progress/lessons/{lessonId}/complete", lesson1.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", studentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.completed").value(true));

        long countAfterSecond = lessonProgressRepository.count();

        // Should still have only 1 record
        assertThat(countAfterSecond).isEqualTo(countAfterFirst);
    }

    @Test
    @DisplayName("Complete lesson - should return 404 for non-existent lesson")
    void completeLesson_nonExistentLesson_shouldReturn404() throws Exception {
        mockMvc.perform(post("/api/v1/lms/progress/lessons/{lessonId}/complete", 99999L)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", studentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== Course Progress Tests ====================

    @Test
    @DisplayName("Get course progress - should return 0% when no lessons completed")
    void getCourseProgress_noLessonsCompleted_shouldReturn0Percent() throws Exception {
        mockMvc.perform(get("/api/v1/lms/progress/courses/{courseId}", testCourse.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", studentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseId").value(testCourse.getId()))
                .andExpect(jsonPath("$.data.userId").value(studentId))
                .andExpect(jsonPath("$.data.totalLessons").value(3))
                .andExpect(jsonPath("$.data.completedLessons").value(0))
                .andExpect(jsonPath("$.data.progressPercent").value(0.0));
    }

    @Test
    @DisplayName("Get course progress - should calculate correctly with partial completion")
    void getCourseProgress_partialCompletion_shouldCalculateCorrectly() throws Exception {
        // Complete 2 out of 3 lessons
        completeLesson(lesson1.getId(), studentId);
        completeLesson(lesson2.getId(), studentId);

        mockMvc.perform(get("/api/v1/lms/progress/courses/{courseId}", testCourse.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", studentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalLessons").value(3))
                .andExpect(jsonPath("$.data.completedLessons").value(2))
                .andExpect(jsonPath("$.data.progressPercent").value(66.7)); // 2/3 * 100 = 66.7%
    }

    @Test
    @DisplayName("Get course progress - should return 100% when all lessons completed")
    void getCourseProgress_allLessonsCompleted_shouldReturn100Percent() throws Exception {
        // Complete all 3 lessons
        completeLesson(lesson1.getId(), studentId);
        completeLesson(lesson2.getId(), studentId);
        completeLesson(lesson3.getId(), studentId);

        mockMvc.perform(get("/api/v1/lms/progress/courses/{courseId}", testCourse.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", studentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalLessons").value(3))
                .andExpect(jsonPath("$.data.completedLessons").value(3))
                .andExpect(jsonPath("$.data.progressPercent").value(100.0));
    }

    @Test
    @DisplayName("Get course progress - should isolate progress by user")
    void getCourseProgress_shouldIsolateByUser() throws Exception {
        Long otherStudentId = 300L;

        // Student 1 completes 2 lessons
        completeLesson(lesson1.getId(), studentId);
        completeLesson(lesson2.getId(), studentId);

        // Student 2 completes 1 lesson
        completeLesson(lesson1.getId(), otherStudentId);

        // Check student 1 progress
        mockMvc.perform(get("/api/v1/lms/progress/courses/{courseId}", testCourse.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", studentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completedLessons").value(2))
                .andExpect(jsonPath("$.data.progressPercent").value(66.7));

        // Check student 2 progress
        mockMvc.perform(get("/api/v1/lms/progress/courses/{courseId}", testCourse.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", otherStudentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completedLessons").value(1))
                .andExpect(jsonPath("$.data.progressPercent").value(33.3));
    }

    // ==================== Get Lesson Progress Tests ====================

    @Test
    @DisplayName("Get lesson progress - should return null when no progress exists")
    void getLessonProgress_noProgress_shouldReturnNull() throws Exception {
        mockMvc.perform(get("/api/v1/lms/progress/lessons/{lessonId}", lesson1.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", studentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("Get lesson progress - should return progress when exists")
    void getLessonProgress_progressExists_shouldReturnProgress() throws Exception {
        // Complete lesson
        completeLesson(lesson1.getId(), studentId);

        mockMvc.perform(get("/api/v1/lms/progress/lessons/{lessonId}", lesson1.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", studentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lessonId").value(lesson1.getId()))
                .andExpect(jsonPath("$.data.userId").value(studentId))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.progressPercent").value(100));
    }

    // ==================== Helper Methods ====================

    private void completeLesson(Long lessonId, Long userId) {
        LessonProgress progress = LessonProgress.builder()
                .userId(userId)
                .lessonId(lessonId)
                .build();
        progress.setInstanceId(tenantId);
        progress.markAsCompleted();
        lessonProgressRepository.save(progress);
    }
}
