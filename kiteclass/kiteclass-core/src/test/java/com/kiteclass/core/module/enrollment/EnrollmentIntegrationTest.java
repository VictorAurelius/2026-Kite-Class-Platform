package com.kiteclass.core.module.enrollment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.module.enrollment.dto.UpdateEnrollmentStatusRequest;
import com.kiteclass.core.module.enrollment.entity.Enrollment;
import com.kiteclass.core.module.enrollment.repository.EnrollmentRepository;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import com.kiteclass.core.testutil.CourseTestDataBuilder;
import com.kiteclass.core.testutil.EnrollmentTestDataBuilder;
import com.kiteclass.core.testutil.StudentTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Enrollment module.
 *
 * <p>Tests the full stack: Controller → Service → Repository → Database.
 *
 * @author KiteClass Team
 * @since 2.6.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@Rollback(true)
@Sql(scripts = "/cleanup-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
class EnrollmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ClassRepository classRepository;
    @Autowired
    private CourseRepository courseRepository;

    private Student savedStudent;
    private Class savedClass;
    private final UUID tenantId = EnrollmentTestDataBuilder.DEFAULT_TENANT;

    @BeforeEach
    void setUp() {
        // Set TenantContext for EntityPersistenceListener
        TenantContext.setCurrentTenant(tenantId);
        try {
            // Create course
            Course course = CourseTestDataBuilder.createDefaultCourse();
            course.setId(null);
            course.setTeacherId(null);
            course.setStatus(CourseStatus.PUBLISHED);
            Course savedCourse = courseRepository.save(course);

            // Create class
            Class clazz = Class.builder()
                    .courseId(savedCourse.getId())
                    .name("Test Class")
                    .classCode("TC-001")
                    .startDate(LocalDate.now().plusDays(7))
                    .endDate(LocalDate.now().plusDays(90))
                    .maxStudents(3)
                    .currentEnrolled(0)
                    .status(ClassStatus.SCHEDULED)
                    .build();
            savedClass = classRepository.save(clazz);

            // Create student
            Student student = StudentTestDataBuilder.createDefaultStudent();
            student.setId(null);
            student.setEmail("student1@example.com");
            student.setStatus(StudentStatus.ACTIVE);
            savedStudent = studentRepository.save(student);
        } finally {
            TenantContext.clear();
        }
    }

    // =========================================================================
    // Enroll student - Success cases
    // =========================================================================

    @Test
    void enrollStudent_shouldReturn201_whenValidRequest() throws Exception {
        CreateEnrollmentRequest request = EnrollmentTestDataBuilder.createRequestForStudentAndClass(
                savedStudent.getId(),
                savedClass.getId()
        );

        mockMvc.perform(post("/api/v1/enrollments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.studentId").value(savedStudent.getId()))
                .andExpect(jsonPath("$.data.classId").value(savedClass.getId()))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.finalAmount").value(1000.00));

        // Verify in database
        long count = enrollmentRepository.countByClassIdAndStatusAndDeletedFalse(
                savedClass.getId(), EnrollmentStatus.ACTIVE
        );
        assertThat(count).isEqualTo(0); // Still PENDING_PAYMENT, not ACTIVE
    }

    @Test
    void enrollStudent_shouldCalculateDiscount_correctly() throws Exception {
        CreateEnrollmentRequest request = EnrollmentTestDataBuilder.createRequestWithDiscount(
                savedStudent.getId(),
                savedClass.getId(),
                new BigDecimal("1000.00"),
                new BigDecimal("10.00") // 10% discount
        );

        mockMvc.perform(post("/api/v1/enrollments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tuitionAmount").value(1000.00))
                .andExpect(jsonPath("$.data.discountPercent").value(10.00))
                .andExpect(jsonPath("$.data.finalAmount").value(900.00)); // 1000 - 10%
    }

    // =========================================================================
    // Enroll student - Validation errors
    // =========================================================================

    @Test
    void enrollStudent_shouldReturn404_whenStudentNotFound() throws Exception {
        CreateEnrollmentRequest request = EnrollmentTestDataBuilder.createRequestForStudentAndClass(
                99999L, // Non-existent student
                savedClass.getId()
        );

        mockMvc.perform(post("/api/v1/enrollments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void enrollStudent_shouldReturn404_whenClassNotFound() throws Exception {
        CreateEnrollmentRequest request = EnrollmentTestDataBuilder.createRequestForStudentAndClass(
                savedStudent.getId(),
                99999L // Non-existent class
        );

        mockMvc.perform(post("/api/v1/enrollments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void enrollStudent_shouldReturn400_whenDuplicateEnrollment() throws Exception {
        CreateEnrollmentRequest request = EnrollmentTestDataBuilder.createRequestForStudentAndClass(
                savedStudent.getId(),
                savedClass.getId()
        );

        // First enrollment - success
        mockMvc.perform(post("/api/v1/enrollments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Activate the enrollment
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndClassIdAndDeletedFalse(
                savedStudent.getId(), savedClass.getId()
        ).orElseThrow();
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollmentRepository.save(enrollment);

        // Second enrollment - duplicate (should fail with 409 Conflict)
        mockMvc.perform(post("/api/v1/enrollments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void enrollStudent_shouldReturn400_whenClassFull() throws Exception {
        // savedClass.maxStudents = 3
        // Enroll 3 students and activate them
        for (int i = 0; i < 3; i++) {
            TenantContext.setCurrentTenant(tenantId);
            try {
                Student student = StudentTestDataBuilder.createDefaultStudent();
                student.setId(null);
                student.setEmail("student" + i + "@example.com");
                student.setPhone("012345678" + i);
                Student saved = studentRepository.save(student);

            CreateEnrollmentRequest request = EnrollmentTestDataBuilder.createRequestForStudentAndClass(
                    saved.getId(),
                    savedClass.getId()
            );

            mockMvc.perform(post("/api/v1/enrollments")
                            .header("X-Tenant-Id", tenantId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

                // Activate enrollment
                Enrollment enrollment = enrollmentRepository.findByStudentIdAndClassIdAndDeletedFalse(
                        saved.getId(), savedClass.getId()
                ).orElseThrow();
                enrollment.setStatus(EnrollmentStatus.ACTIVE);
                enrollmentRepository.save(enrollment);
            } finally {
                TenantContext.clear();
            }
        }

        // Try to enroll 4th student - should fail (class full)
        TenantContext.setCurrentTenant(tenantId);
        try {
            Student extraStudent = StudentTestDataBuilder.createDefaultStudent();
            extraStudent.setId(null);
            extraStudent.setEmail("extra@example.com");
            extraStudent.setPhone("0123456790");
            Student savedExtra = studentRepository.save(extraStudent);

            CreateEnrollmentRequest request = EnrollmentTestDataBuilder.createRequestForStudentAndClass(
                    savedExtra.getId(),
                    savedClass.getId()
            );

            mockMvc.perform(post("/api/v1/enrollments")
                            .header("X-Tenant-Id", tenantId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        } finally {
            TenantContext.clear();
        }
    }

    // =========================================================================
    // Get enrollment by ID
    // =========================================================================

    @Test
    void getEnrollmentById_shouldReturn200_whenFound() throws Exception {
        // Create enrollment first
        CreateEnrollmentRequest request = EnrollmentTestDataBuilder.createRequestForStudentAndClass(
                savedStudent.getId(),
                savedClass.getId()
        );

        String response = mockMvc.perform(post("/api/v1/enrollments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long enrollmentId = objectMapper.readTree(response).get("data").get("id").asLong();

        // Get by ID
        mockMvc.perform(get("/api/v1/enrollments/" + enrollmentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(enrollmentId))
                .andExpect(jsonPath("$.data.studentId").value(savedStudent.getId()));
    }

    @Test
    void getEnrollmentById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/99999")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // Update enrollment status
    // =========================================================================

    @Test
    void updateEnrollmentStatus_shouldReturn200_whenValid() throws Exception {
        // Create enrollment
        CreateEnrollmentRequest createRequest = EnrollmentTestDataBuilder.createRequestForStudentAndClass(
                savedStudent.getId(),
                savedClass.getId()
        );

        String response = mockMvc.perform(post("/api/v1/enrollments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long enrollmentId = objectMapper.readTree(response).get("data").get("id").asLong();

        // Update status to ACTIVE
        UpdateEnrollmentStatusRequest updateRequest = EnrollmentTestDataBuilder
                .createUpdateStatusRequest(EnrollmentStatus.ACTIVE);

        mockMvc.perform(put("/api/v1/enrollments/" + enrollmentId + "/status")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // Verify in database
        Enrollment enrollment = enrollmentRepository.findByIdAndDeletedFalse(enrollmentId).orElseThrow();
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    }

    // =========================================================================
    // Withdraw student
    // =========================================================================

    @Test
    void withdrawStudent_shouldReturn200_whenValid() throws Exception {
        // Create and activate enrollment
        CreateEnrollmentRequest createRequest = EnrollmentTestDataBuilder.createRequestForStudentAndClass(
                savedStudent.getId(),
                savedClass.getId()
        );

        String response = mockMvc.perform(post("/api/v1/enrollments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long enrollmentId = objectMapper.readTree(response).get("data").get("id").asLong();

        // Withdraw
        mockMvc.perform(put("/api/v1/enrollments/" + enrollmentId + "/withdraw")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));

        // Verify in database
        Enrollment enrollment = enrollmentRepository.findByIdAndDeletedFalse(enrollmentId).orElseThrow();
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.WITHDRAWN);
    }

    @Test
    void withdrawStudent_shouldReturn400_whenAlreadyWithdrawn() throws Exception {
        // Create enrollment
        CreateEnrollmentRequest createRequest = EnrollmentTestDataBuilder.createRequestForStudentAndClass(
                savedStudent.getId(),
                savedClass.getId()
        );

        String response = mockMvc.perform(post("/api/v1/enrollments")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long enrollmentId = objectMapper.readTree(response).get("data").get("id").asLong();

        // First withdraw - success
        mockMvc.perform(put("/api/v1/enrollments/" + enrollmentId + "/withdraw")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk());

        // Second withdraw - should fail
        mockMvc.perform(put("/api/v1/enrollments/" + enrollmentId + "/withdraw")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // Multi-tenant isolation
    // =========================================================================

    @Test
    void enrollStudent_shouldIsolate_multiTenantData() throws Exception {
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();

        // Tenant 1: Create student, class, enroll
        TenantContext.setCurrentTenant(tenant1);
        try {
            Course course1 = CourseTestDataBuilder.createDefaultCourse();
            course1.setId(null);
            course1.setCode("TENANT1-COURSE"); // Unique code
            course1.setTeacherId(null);
            course1.setStatus(CourseStatus.PUBLISHED);
            Course savedCourse1 = courseRepository.save(course1);

            Class class1 = Class.builder()
                    .courseId(savedCourse1.getId())
                    .name("Tenant1 Class")
                    .classCode("T1-001")
                    .startDate(LocalDate.now().plusDays(7))
                    .endDate(LocalDate.now().plusDays(90))
                    .maxStudents(10)
                    .currentEnrolled(0)
                    .status(ClassStatus.SCHEDULED)
                    .build();
            Class savedClass1 = classRepository.save(class1);

            Student student1 = StudentTestDataBuilder.createDefaultStudent();
            student1.setId(null);
            student1.setEmail("tenant1@example.com");
            Student savedStudent1 = studentRepository.save(student1);

            // Enroll in tenant1
            CreateEnrollmentRequest request1 = EnrollmentTestDataBuilder.createRequestForStudentAndClass(
                    savedStudent1.getId(),
                    savedClass1.getId()
            );

            String response1 = mockMvc.perform(post("/api/v1/enrollments")
                            .header("X-Tenant-Id", tenant1.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            Long enrollmentId = objectMapper.readTree(response1).get("data").get("id").asLong();

            // Tenant 2: Try to access tenant1's enrollment - should get 404
            mockMvc.perform(get("/api/v1/enrollments/" + enrollmentId)
                            .header("X-Tenant-Id", tenant2.toString()))
                    .andExpect(status().isNotFound());
        } finally {
            TenantContext.clear();
        }
    }

    // =========================================================================
    // Get enrollments by student
    // =========================================================================

    @Test
    void getEnrollmentsByStudent_shouldReturn200_withPagination() throws Exception {
        // Create 2 enrollments for same student
        for (int i = 0; i < 2; i++) {
            TenantContext.setCurrentTenant(tenantId);
            try {
                Course course = CourseTestDataBuilder.createDefaultCourse();
                course.setId(null);
                course.setCode("COURSE-" + i); // Unique code
                course.setTeacherId(null);
                course.setStatus(CourseStatus.PUBLISHED);
                Course savedCourse = courseRepository.save(course);

                Class clazz = Class.builder()
                        .courseId(savedCourse.getId())
                        .name("Class " + i)
                        .classCode("C-00" + i)
                        .startDate(LocalDate.now().plusDays(7))
                        .endDate(LocalDate.now().plusDays(90))
                        .maxStudents(10)
                        .currentEnrolled(0)
                        .status(ClassStatus.SCHEDULED)
                        .build();
                Class savedClassX = classRepository.save(clazz);

                CreateEnrollmentRequest request = EnrollmentTestDataBuilder.createRequestForStudentAndClass(
                        savedStudent.getId(),
                        savedClassX.getId()
                );

                mockMvc.perform(post("/api/v1/enrollments")
                                .header("X-Tenant-Id", tenantId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated());
            } finally {
                TenantContext.clear();
            }
        }

        // Get all enrollments for student
        mockMvc.perform(get("/api/v1/enrollments/student/" + savedStudent.getId())
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    // =========================================================================
    // Get enrollments by class
    // =========================================================================

    @Test
    void getEnrollmentsByClass_shouldReturn200_withPagination() throws Exception {
        // Create 2 students and enroll them in same class
        for (int i = 0; i < 2; i++) {
            TenantContext.setCurrentTenant(tenantId);
            try {
                Student student = StudentTestDataBuilder.createDefaultStudent();
                student.setId(null);
                student.setEmail("student" + i + "@test.com");
                student.setPhone("098765432" + i);
                Student saved = studentRepository.save(student);

                CreateEnrollmentRequest request = EnrollmentTestDataBuilder.createRequestForStudentAndClass(
                        saved.getId(),
                        savedClass.getId()
                );

                mockMvc.perform(post("/api/v1/enrollments")
                                .header("X-Tenant-Id", tenantId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated());
            } finally {
                TenantContext.clear();
            }
        }

        // Get all enrollments for class
        mockMvc.perform(get("/api/v1/enrollments/class/" + savedClass.getId())
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }
}
