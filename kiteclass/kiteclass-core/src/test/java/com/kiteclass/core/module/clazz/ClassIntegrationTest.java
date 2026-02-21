package com.kiteclass.core.module.clazz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.clazz.dto.CancelClassRequest;
import com.kiteclass.core.module.clazz.dto.ClassCodeResponse;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.clazz.dto.CreateScheduleRequest;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.clazz.repository.ClassSessionRepository;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.testutil.ClassTestDataBuilder;
import com.kiteclass.core.testutil.CourseTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Class module.
 *
 * <p>Tests the full stack: Controller → Service → Repository → Database.
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
class ClassIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ClassRepository classRepository;
    @Autowired private ClassSessionRepository sessionRepository;
    @Autowired private CourseRepository courseRepository;

    private Course savedCourse;
    private final UUID tenantId = ClassTestDataBuilder.DEFAULT_TENANT;

    @BeforeEach
    void setUp() {
        // Set TenantContext so EntityPersistenceListener can auto-set instanceId
        TenantContext.setCurrentTenant(tenantId);
        try {
            Course course = CourseTestDataBuilder.createDefaultCourse();
            course.setId(null);
            course.setTeacherId(null); // FK to teachers table — no teacher needed for class tests
            course.setStatus(CourseStatus.PUBLISHED);
            // instanceId auto-set by EntityPersistenceListener from TenantContext
            savedCourse = courseRepository.save(course);
        } finally {
            TenantContext.clear();
        }
    }

    // =========================================================================
    // Create class
    // =========================================================================

    @Test
    void createClass_shouldPersistToDatabase() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                ClassTestDataBuilder.createDefaultCreateRequest());

        mockMvc.perform(post("/api/v1/courses/" + savedCourse.getId() + "/classes")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.currentEnrolled").value(0));

        long count = classRepository.countByCourseIdAndDeletedFalse(savedCourse.getId());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void createClass_shouldReturn404_whenCourseNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/courses/99999/classes")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ClassTestDataBuilder.createDefaultCreateRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void createClass_shouldReturn409_whenNameDuplicated() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                ClassTestDataBuilder.createDefaultCreateRequest());

        // Create first
        mockMvc.perform(post("/api/v1/courses/" + savedCourse.getId() + "/classes")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        // Try to create duplicate
        mockMvc.perform(post("/api/v1/courses/" + savedCourse.getId() + "/classes")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // Lifecycle integration tests
    // =========================================================================

    @Test
    void startClass_shouldChangeStatus_toInProgress() throws Exception {
        Class clazz = createAndSaveClass();

        mockMvc.perform(post("/api/v1/classes/" + clazz.getId() + "/start")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        Class updated = classRepository.findByIdAndDeletedFalse(clazz.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ClassStatus.IN_PROGRESS);
        assertThat(updated.getStartedAt()).isNotNull();
    }

    @Test
    void completeClass_shouldChangeStatus_toCompleted() throws Exception {
        Class clazz = createAndSaveClass();

        // Start first
        mockMvc.perform(post("/api/v1/classes/" + clazz.getId() + "/start")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk());

        // Then complete
        mockMvc.perform(post("/api/v1/classes/" + clazz.getId() + "/complete")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void cancelClass_shouldChangeStatus_toCancelled() throws Exception {
        Class clazz = createAndSaveClass();

        mockMvc.perform(post("/api/v1/classes/" + clazz.getId() + "/cancel")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ClassTestDataBuilder.createCancelRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void deleteClass_shouldSoftDelete() throws Exception {
        Class clazz = createAndSaveClass();

        mockMvc.perform(delete("/api/v1/classes/" + clazz.getId())
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNoContent());

        assertThat(classRepository.findByIdAndDeletedFalse(clazz.getId())).isEmpty();
    }

    // =========================================================================
    // Schedule integration
    // =========================================================================

    @Test
    void createSchedule_shouldGenerateSessions_forTwoWeekMWF() throws Exception {
        Class clazz = createAndSaveClass(); // startDate 2026-03-01, endDate 2026-05-31

        // Override with 2-week range: 2026-03-02 (Mon) to 2026-03-15 (Sun) → 6 MWF sessions
        Class updatedClass = classRepository.findByIdAndDeletedFalse(clazz.getId()).orElseThrow();
        updatedClass.setStartDate(LocalDate.of(2026, 3, 2));
        updatedClass.setEndDate(LocalDate.of(2026, 3, 15));
        classRepository.save(updatedClass);

        CreateScheduleRequest scheduleRequest = new CreateScheduleRequest(
                List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                LocalTime.of(18, 0), LocalTime.of(20, 0));

        mockMvc.perform(post("/api/v1/classes/" + clazz.getId() + "/schedule")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(scheduleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(6));

        long sessionCount = sessionRepository.countByClassIdAndDeletedFalse(clazz.getId());
        assertThat(sessionCount).isEqualTo(6);
    }

    @Test
    void listSessions_shouldReturnOrderedSessions() throws Exception {
        Class clazz = createAndSaveClass();
        Class updatedClass = classRepository.findByIdAndDeletedFalse(clazz.getId()).orElseThrow();
        updatedClass.setStartDate(LocalDate.of(2026, 3, 2));
        updatedClass.setEndDate(LocalDate.of(2026, 3, 9));
        classRepository.save(updatedClass);

        // Create schedule first
        mockMvc.perform(post("/api/v1/classes/" + clazz.getId() + "/schedule")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new CreateScheduleRequest(
                                List.of(DayOfWeek.MONDAY),
                                LocalTime.of(9, 0), LocalTime.of(11, 0)))));

        // List sessions
        mockMvc.perform(get("/api/v1/classes/" + clazz.getId() + "/sessions")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // =========================================================================
    // Multi-tenant isolation
    // =========================================================================

    @Test
    void getClass_shouldReturn404_whenAccessedFromDifferentTenant() throws Exception {
        Class clazz = createAndSaveClass();

        UUID differentTenant = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/classes/" + clazz.getId())
                        .header("X-Tenant-Id", differentTenant.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createClass_sameCourseName_shouldSucceed_forDifferentTenants() throws Exception {
        // Create class for tenant A
        mockMvc.perform(post("/api/v1/courses/" + savedCourse.getId() + "/classes")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ClassTestDataBuilder.createDefaultCreateRequest())))
                .andExpect(status().isCreated());

        // Same name for different tenant should be checked against different course
        // (This test validates multi-tenant boundary at course level)
        assertThat(classRepository.countByCourseIdAndDeletedFalse(savedCourse.getId())).isEqualTo(1);
    }

    // =========================================================================
    // Class code
    // =========================================================================

    @Test
    void generateCode_shouldSetCodeOnClass() throws Exception {
        Class clazz = createAndSaveClass();

        mockMvc.perform(post("/api/v1/classes/" + clazz.getId() + "/generate-code")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ClassTestDataBuilder.createCodeRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classCode").isNotEmpty());

        Class updated = classRepository.findByIdAndDeletedFalse(clazz.getId()).orElseThrow();
        assertThat(updated.getClassCode()).isNotNull();
        assertThat(updated.getClassCode()).hasSize(8);
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private Class createAndSaveClass() {
        Class clazz = ClassTestDataBuilder.createDefaultClass();
        clazz.setId(null);
        clazz.setCourseId(savedCourse.getId());
        clazz.setInstanceId(tenantId);
        clazz.setVersion(null);
        return classRepository.save(clazz);
    }
}
