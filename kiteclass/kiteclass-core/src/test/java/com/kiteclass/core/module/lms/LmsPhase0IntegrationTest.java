package com.kiteclass.core.module.lms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.lms.dto.request.ReorderRequest;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for LMS Phase0-BE gap-fill endpoints (reorder + completion roster)
 * against real PostgreSQL — verifies the two-phase reorder swap does not transiently
 * violate the {@code (…, order_number, instance_id)} unique constraint, and RLS scoping.
 *
 * @since GAP-1113 LMS Phase0-BE
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@DisplayName("LMS Phase0-BE Integration Tests")
class LmsPhase0IntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CourseModuleRepository courseModuleRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private LessonProgressRepository lessonProgressRepository;

    private final UUID tenantId = UUID.randomUUID();
    private final Long teacherId = 100L;
    private Course course;
    private CourseModule moduleA;
    private CourseModule moduleB;
    private Lesson lesson1;
    private Lesson lesson2;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(tenantId);

        course = Course.builder()
                .name("Phase0 Course").code("P0-1").teacherId(teacherId).status(CourseStatus.PUBLISHED).build();
        course.setInstanceId(tenantId);
        course = courseRepository.save(course);

        moduleA = saveModule("Module A", 1);
        moduleB = saveModule("Module B", 2);

        lesson1 = saveLesson(moduleA.getId(), "Lesson 1", 1);
        lesson2 = saveLesson(moduleA.getId(), "Lesson 2", 2);
    }

    private CourseModule saveModule(String title, int order) {
        CourseModule m = CourseModule.builder().courseId(course.getId()).title(title).orderNumber(order).build();
        m.setInstanceId(tenantId);
        return courseModuleRepository.save(m);
    }

    private Lesson saveLesson(Long moduleId, String title, int order) {
        Lesson l = Lesson.builder().moduleId(moduleId).title(title).isTrial(false).orderNumber(order).build();
        l.setInstanceId(tenantId);
        return lessonRepository.save(l);
    }

    @Test
    @DisplayName("Reorder modules swaps order numbers without unique-constraint violation")
    void reorderModules_swap() throws Exception {
        ReorderRequest req = new ReorderRequest(List.of(
                new ReorderRequest.ReorderItem(moduleA.getId(), 2),
                new ReorderRequest.ReorderItem(moduleB.getId(), 1)));

        mockMvc.perform(put("/api/v1/lms/courses/{courseId}/modules/reorder", course.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", teacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                // ascending order: moduleB (now order 1) first, moduleA (order 2) second
                .andExpect(jsonPath("$.data[0].id").value(moduleB.getId()))
                .andExpect(jsonPath("$.data[0].orderNumber").value(1))
                .andExpect(jsonPath("$.data[1].id").value(moduleA.getId()))
                .andExpect(jsonPath("$.data[1].orderNumber").value(2));
    }

    @Test
    @DisplayName("Reorder lessons swaps order numbers without unique-constraint violation")
    void reorderLessons_swap() throws Exception {
        ReorderRequest req = new ReorderRequest(List.of(
                new ReorderRequest.ReorderItem(lesson1.getId(), 2),
                new ReorderRequest.ReorderItem(lesson2.getId(), 1)));

        mockMvc.perform(put("/api/v1/lms/modules/{moduleId}/lessons/reorder", moduleA.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", teacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(lesson2.getId()))
                .andExpect(jsonPath("$.data[0].orderNumber").value(1))
                .andExpect(jsonPath("$.data[1].id").value(lesson1.getId()))
                .andExpect(jsonPath("$.data[1].orderNumber").value(2));
    }

    @Test
    @DisplayName("Reorder with incomplete set returns 400")
    void reorderModules_incompleteSet_returns400() throws Exception {
        ReorderRequest req = new ReorderRequest(List.of(
                new ReorderRequest.ReorderItem(moduleA.getId(), 1)));  // missing moduleB

        mockMvc.perform(put("/api/v1/lms/courses/{courseId}/modules/reorder", course.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", teacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Reorder by a non-owner teacher returns 403")
    void reorderModules_nonOwner_returns403() throws Exception {
        ReorderRequest req = new ReorderRequest(List.of(
                new ReorderRequest.ReorderItem(moduleA.getId(), 2),
                new ReorderRequest.ReorderItem(moduleB.getId(), 1)));

        mockMvc.perform(put("/api/v1/lms/courses/{courseId}/modules/reorder", course.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", "999")  // not the owner
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Completion roster aggregates per-student completion")
    void completionRoster_aggregates() throws Exception {
        // student 200 completed lesson1; lesson2 not completed
        LessonProgress progress = LessonProgress.builder()
                .userId(200L).lessonId(lesson1.getId()).completed(true).progressPercent(100).build();
        progress.setInstanceId(tenantId);
        lessonProgressRepository.save(progress);

        mockMvc.perform(get("/api/v1/lms/courses/{courseId}/completion-roster", course.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", teacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseId").value(course.getId()))
                .andExpect(jsonPath("$.data.totalLessons").value(2))
                .andExpect(jsonPath("$.data.students", hasSize(1)))
                .andExpect(jsonPath("$.data.students[0].userId").value(200))
                .andExpect(jsonPath("$.data.students[0].completedLessons").value(1))
                .andExpect(jsonPath("$.data.students[0].progressPercent").value(50.0));
    }
}
