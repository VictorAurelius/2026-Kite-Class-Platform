package com.kiteclass.core.module.lms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.enrollment.entity.Enrollment;
import com.kiteclass.core.module.enrollment.repository.EnrollmentRepository;
import com.kiteclass.core.module.lms.dto.request.CreateCourseModuleRequest;
import com.kiteclass.core.module.lms.dto.request.CreateLessonRequest;
import com.kiteclass.core.module.lms.entity.CourseModule;
import com.kiteclass.core.module.lms.entity.Lesson;
import com.kiteclass.core.module.lms.repository.CourseModuleRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for LMS module.
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
@DisplayName("LMS Integration Tests")
class LmsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseModuleRepository courseModuleRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private com.kiteclass.core.module.clazz.repository.ClassRepository classRepository;

    private Course testCourse;
    private CourseModule testModule;
    private Lesson trialLesson;
    private Lesson paidLesson;
    private final UUID tenantId = UUID.randomUUID();
    private final Long teacherId = 100L;
    private final Long studentId = 200L;

    @BeforeEach
    void setUp() {
        // Set tenant context
        TenantContext.setCurrentTenant(tenantId);

        // Create test course
        testCourse = Course.builder()
                .name("Test Course")
                .code("TEST-LMS-001")
                .teacherId(teacherId)
                .status(CourseStatus.PUBLISHED)
                .price(new BigDecimal("1000.00"))
                .build();
        testCourse.setInstanceId(tenantId);
        testCourse = courseRepository.save(testCourse);

        // Create test module
        testModule = CourseModule.builder()
                .courseId(testCourse.getId())
                .title("Module 1: Introduction")
                .description("Introduction to the course")
                .orderNumber(1)
                .build();
        testModule.setInstanceId(tenantId);
        testModule = courseModuleRepository.save(testModule);

        // Create trial lesson
        trialLesson = Lesson.builder()
                .moduleId(testModule.getId())
                .title("Trial Lesson 1")
                .content("This is a free trial lesson")
                .videoUrl("https://youtube.com/trial")
                .isTrial(true)
                .orderNumber(1)
                .estimatedDuration(15)
                .build();
        trialLesson.setInstanceId(tenantId);
        trialLesson = lessonRepository.save(trialLesson);

        // Create paid lesson
        paidLesson = Lesson.builder()
                .moduleId(testModule.getId())
                .title("Paid Lesson 1")
                .content("This is a paid lesson")
                .videoUrl("https://youtube.com/paid")
                .isTrial(false)
                .orderNumber(2)
                .estimatedDuration(30)
                .build();
        paidLesson.setInstanceId(tenantId);
        paidLesson = lessonRepository.save(paidLesson);
    }

    // ==================== Guest Access Tests ====================

    @Test
    @DisplayName("Guest - Get course structure should return trial lessons only")
    void getCourseStructure_guest_shouldReturnTrialLessonsOnly() throws Exception {
        mockMvc.perform(get("/api/v1/lms/courses/{courseId}/modules", testCourse.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(testModule.getId()))
                .andExpect(jsonPath("$.data[0].lessons").isArray())
                .andExpect(jsonPath("$.data[0].lessons", hasSize(1))) // Only trial lesson
                .andExpect(jsonPath("$.data[0].lessons[0].title").value("Trial Lesson 1"))
                .andExpect(jsonPath("$.data[0].lessons[0].isTrial").value(true));
    }

    @Test
    @DisplayName("Guest - Get trial lesson should succeed")
    void getLesson_guest_trialLesson_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/lms/lessons/{lessonId}", trialLesson.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(trialLesson.getId()))
                .andExpect(jsonPath("$.data.title").value("Trial Lesson 1"))
                .andExpect(jsonPath("$.data.isTrial").value(true));
    }

    @Test
    @DisplayName("Guest - Get paid lesson should return 403")
    void getLesson_guest_paidLesson_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/lms/lessons/{lessonId}", paidLesson.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Guest - Get DRAFT course should return 400")
    void getCourseStructure_guest_draftCourse_shouldReturn400() throws Exception {
        // Create draft course
        Course draftCourse = Course.builder()
                .name("Draft Course")
                .code("DRAFT-001")
                .teacherId(teacherId)
                .status(CourseStatus.DRAFT)
                .build();
        draftCourse.setInstanceId(tenantId);
        draftCourse = courseRepository.save(draftCourse);

        mockMvc.perform(get("/api/v1/lms/courses/{courseId}/modules", draftCourse.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== Student Access Tests ====================

    @Test
    @DisplayName("Student - Get course structure should return all lessons (no enrollment check)")
    void getCourseStructure_student_shouldReturnAllLessons() throws Exception {
        // Note: getCourseStructureForStudent does NOT verify enrollment (UX decision)
        mockMvc.perform(get("/api/v1/lms/courses/{courseId}/modules", testCourse.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", studentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].lessons", hasSize(2))) // Both trial and paid lessons
                .andExpect(jsonPath("$.data[0].lessons[0].title").value("Trial Lesson 1"))
                .andExpect(jsonPath("$.data[0].lessons[1].title").value("Paid Lesson 1"));
    }

    @Test
    @DisplayName("Student - Get paid lesson should succeed when enrolled")
    void getLesson_student_enrolled_shouldSucceed() throws Exception {
        // Create class
        com.kiteclass.core.module.clazz.entity.Class testClass = com.kiteclass.core.module.clazz.entity.Class.builder()
                .courseId(testCourse.getId())
                .name("Test Class")
                .startDate(java.time.LocalDate.now())
                .endDate(java.time.LocalDate.now().plusMonths(3))
                .build();
        testClass.setInstanceId(tenantId);
        testClass = classRepository.save(testClass);

        // Create ACTIVE enrollment
        Enrollment enrollment = Enrollment.builder()
                .studentId(studentId)
                .classId(testClass.getId())
                .status(EnrollmentStatus.ACTIVE)
                .tuitionAmount(new BigDecimal("1000.00"))
                .build();
        enrollment.setInstanceId(tenantId);
        enrollmentRepository.save(enrollment);

        mockMvc.perform(get("/api/v1/lms/lessons/{lessonId}", paidLesson.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", studentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(paidLesson.getId()))
                .andExpect(jsonPath("$.data.title").value("Paid Lesson 1"));
    }

    @Test
    @DisplayName("Student - Get paid lesson should return 403 when not enrolled")
    void getLesson_student_notEnrolled_shouldReturn403() throws Exception {
        // Create class BUT no enrollment
        com.kiteclass.core.module.clazz.entity.Class testClass = com.kiteclass.core.module.clazz.entity.Class.builder()
                .courseId(testCourse.getId())
                .name("Test Class")
                .startDate(java.time.LocalDate.now())
                .endDate(java.time.LocalDate.now().plusMonths(3))
                .build();
        testClass.setInstanceId(tenantId);
        classRepository.save(testClass);

        mockMvc.perform(get("/api/v1/lms/lessons/{lessonId}", paidLesson.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", studentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Student - Get paid lesson should return 403 when enrollment is PENDING_PAYMENT")
    void getLesson_student_pendingPayment_shouldReturn403() throws Exception {
        // Create class
        com.kiteclass.core.module.clazz.entity.Class testClass = com.kiteclass.core.module.clazz.entity.Class.builder()
                .courseId(testCourse.getId())
                .name("Test Class")
                .startDate(java.time.LocalDate.now())
                .endDate(java.time.LocalDate.now().plusMonths(3))
                .build();
        testClass.setInstanceId(tenantId);
        testClass = classRepository.save(testClass);

        // Create PENDING_PAYMENT enrollment (should NOT grant access)
        Enrollment enrollment = Enrollment.builder()
                .studentId(studentId)
                .classId(testClass.getId())
                .status(EnrollmentStatus.PENDING_PAYMENT)
                .tuitionAmount(new BigDecimal("1000.00"))
                .build();
        enrollment.setInstanceId(tenantId);
        enrollmentRepository.save(enrollment);

        mockMvc.perform(get("/api/v1/lms/lessons/{lessonId}", paidLesson.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", studentId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== Teacher CRUD Tests ====================

    @Test
    @DisplayName("Teacher - Create module should succeed for course owner")
    void createModule_teacher_courseOwner_shouldSucceed() throws Exception {
        CreateCourseModuleRequest request = new CreateCourseModuleRequest(
                "Module 2: Advanced Topics",
                "Deep dive into advanced concepts",
                2
        );

        mockMvc.perform(post("/api/v1/lms/courses/{courseId}/modules", testCourse.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", teacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Module 2: Advanced Topics"))
                .andExpect(jsonPath("$.data.orderNumber").value(2));
    }

    @Test
    @DisplayName("Teacher - Create module should return 403 for non-owner")
    void createModule_teacher_notOwner_shouldReturn403() throws Exception {
        CreateCourseModuleRequest request = new CreateCourseModuleRequest(
                "Module 2",
                "Description",
                2
        );

        Long nonOwnerTeacherId = 999L;

        mockMvc.perform(post("/api/v1/lms/courses/{courseId}/modules", testCourse.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", nonOwnerTeacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Teacher - Create module should return 400 for duplicate order number")
    void createModule_teacher_duplicateOrderNumber_shouldReturn400() throws Exception {
        CreateCourseModuleRequest request = new CreateCourseModuleRequest(
                "Duplicate Module",
                "Description",
                1 // Same as existing module
        );

        mockMvc.perform(post("/api/v1/lms/courses/{courseId}/modules", testCourse.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", teacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Teacher - Create lesson should succeed")
    void createLesson_teacher_shouldSucceed() throws Exception {
        CreateLessonRequest request = new CreateLessonRequest(
                "Lesson 3: New Topic",
                "Content for new topic",
                "https://youtube.com/new",
                false,
                3,
                45
        );

        mockMvc.perform(post("/api/v1/lms/modules/{moduleId}/lessons", testModule.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", teacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Lesson 3: New Topic"))
                .andExpect(jsonPath("$.data.orderNumber").value(3))
                .andExpect(jsonPath("$.data.estimatedDuration").value(45));
    }

    @Test
    @DisplayName("Teacher - Delete module should return 400 when module has lessons")
    void deleteModule_teacher_hasLessons_shouldReturn400() throws Exception {
        mockMvc.perform(delete("/api/v1/lms/modules/{moduleId}", testModule.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", teacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Teacher - Delete module should succeed when no lessons")
    void deleteModule_teacher_noLessons_shouldSucceed() throws Exception {
        // Create empty module
        CourseModule emptyModule = CourseModule.builder()
                .courseId(testCourse.getId())
                .title("Empty Module")
                .orderNumber(99)
                .build();
        emptyModule.setInstanceId(tenantId);
        emptyModule = courseModuleRepository.save(emptyModule);

        mockMvc.perform(delete("/api/v1/lms/modules/{moduleId}", emptyModule.getId())
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", teacherId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.success").value(true));
    }
}
