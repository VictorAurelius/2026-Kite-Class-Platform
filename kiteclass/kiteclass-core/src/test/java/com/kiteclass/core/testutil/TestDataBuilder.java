package com.kiteclass.core.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.Gender;
import com.kiteclass.core.common.constant.TeacherClassRole;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.teacher.dto.CreateTeacherRequest;
import com.kiteclass.core.module.teacher.entity.TeacherClass;
import com.kiteclass.core.module.teacher.repository.TeacherClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Utility class for creating test data fixtures in integration tests.
 *
 * <p>Provides helper methods to create common test entities (teachers, courses, students)
 * with reasonable defaults, reducing boilerplate in integration tests.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * @Autowired
 * private TestDataBuilder testDataBuilder;
 *
 * @BeforeEach
 * void setUp() {
 *     tenantId = UUID.randomUUID();
 *     teacherId = testDataBuilder.createTestTeacher(mockMvc, objectMapper, tenantId);
 * }
 * }</pre>
 *
 * @author KiteClass Team
 * @since 2.13
 */
@Component
@RequiredArgsConstructor
public class TestDataBuilder {

    private final TeacherClassRepository teacherClassRepository;

    /**
     * Creates a test teacher with default values.
     *
     * <p>Default teacher profile:
     * <ul>
     *     <li>Name: "Test Teacher"</li>
     *     <li>Email: "test.teacher@kiteclass.test" (unique with timestamp)</li>
     *     <li>Phone: "+84900000000"</li>
     *     <li>Status: ACTIVE</li>
     *     <li>Specialization: "Computer Science"</li>
     * </ul>
     *
     * @param mockMvc MockMvc instance for making HTTP requests
     * @param objectMapper ObjectMapper for JSON serialization/deserialization
     * @param tenantId Tenant ID for multi-tenant isolation
     * @return the created teacher's ID
     * @throws Exception if teacher creation fails
     */
    public Long createTestTeacher(MockMvc mockMvc, ObjectMapper objectMapper, UUID tenantId) throws Exception {
        return createTestTeacher(mockMvc, objectMapper, tenantId, "Test Teacher", "Computer Science");
    }

    /**
     * Creates a test teacher with custom name and specialization.
     *
     * @param mockMvc MockMvc instance for making HTTP requests
     * @param objectMapper ObjectMapper for JSON serialization/deserialization
     * @param tenantId Tenant ID for multi-tenant isolation
     * @param name Teacher's full name
     * @param specialization Teacher's specialization/subject area
     * @return the created teacher's ID
     * @throws Exception if teacher creation fails
     */
    public Long createTestTeacher(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UUID tenantId,
            String name,
            String specialization
    ) throws Exception {
        // Generate unique email with timestamp to avoid conflicts in parallel tests
        String email = "teacher." + System.currentTimeMillis() + "@kiteclass.test";

        CreateTeacherRequest teacherRequest = new CreateTeacherRequest(
                name,                           // name
                email,                          // email
                "0900000000",                   // phoneNumber
                specialization,                 // specialization
                "Experienced educator",         // bio
                "Bachelor of Education",        // qualification
                5                               // experienceYears
        );

        MvcResult result = mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        // GAP-1524: TeacherController.createTeacher gained @PreAuthorize
                        // (OWNER/ADMIN/PRINCIPAL/PLATFORM_ADMIN) in GAP-1491. Authenticate the
                        // fixture setup as ADMIN via the SecurityContext so method-security-enabled
                        // tests (e.g. CrossUserAuthzTest) can build their world. No-op when the
                        // test context has no @EnableMethodSecurity.
                        .with(user("fixture-admin").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(teacherRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data")
                .get("id")
                .asLong();
    }

    /**
     * Creates multiple test teachers for bulk testing scenarios.
     *
     * <p>Each teacher will have:
     * <ul>
     *     <li>Unique email (teacher1@test.com, teacher2@test.com, ...)</li>
     *     <li>Sequential names (Teacher 1, Teacher 2, ...)</li>
     *     <li>Different specializations if count {@literal >} 3</li>
     * </ul>
     *
     * @param mockMvc MockMvc instance for making HTTP requests
     * @param objectMapper ObjectMapper for JSON serialization/deserialization
     * @param tenantId Tenant ID for multi-tenant isolation
     * @param count Number of teachers to create
     * @return array of created teacher IDs
     * @throws Exception if teacher creation fails
     */
    public Long[] createMultipleTeachers(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UUID tenantId,
            int count
    ) throws Exception {
        Long[] teacherIds = new Long[count];
        String[] specializations = {"Computer Science", "Mathematics", "Physics", "Chemistry", "Biology"};

        for (int i = 0; i < count; i++) {
            String specialization = specializations[i % specializations.length];
            teacherIds[i] = createTestTeacher(
                    mockMvc,
                    objectMapper,
                    tenantId,
                    "Teacher " + (i + 1),
                    specialization
            );
        }

        return teacherIds;
    }

    /**
     * Creates a test student with default values.
     *
     * <p>Default student profile:
     * <ul>
     *     <li>Name: "Test Student"</li>
     *     <li>Email: "test.student@kiteclass.test" (unique with timestamp)</li>
     *     <li>Phone: "0901234567" (valid Vietnamese format)</li>
     *     <li>Date of Birth: 2005-01-01 (18 years old)</li>
     *     <li>Gender: MALE</li>
     * </ul>
     *
     * @param mockMvc MockMvc instance for making HTTP requests
     * @param objectMapper ObjectMapper for JSON serialization/deserialization
     * @param tenantId Tenant ID for multi-tenant isolation
     * @return the created student's ID
     * @throws Exception if student creation fails
     */
    public Long createTestStudent(MockMvc mockMvc, ObjectMapper objectMapper, UUID tenantId) throws Exception {
        return createTestStudent(mockMvc, objectMapper, tenantId, "Test Student");
    }

    /**
     * Creates a test student with custom name.
     *
     * @param mockMvc MockMvc instance for making HTTP requests
     * @param objectMapper ObjectMapper for JSON serialization/deserialization
     * @param tenantId Tenant ID for multi-tenant isolation
     * @param name Student's full name
     * @return the created student's ID
     * @throws Exception if student creation fails
     */
    public Long createTestStudent(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UUID tenantId,
            String name
    ) throws Exception {
        // Generate unique email with timestamp to avoid conflicts in parallel tests
        String email = "student." + System.currentTimeMillis() + "@kiteclass.test";

        CreateStudentRequest studentRequest = new CreateStudentRequest(
                name,                               // name
                email,                              // email
                "0901234567",                       // phone (valid Vietnamese format)
                LocalDate.of(2005, 1, 1),          // dateOfBirth (18 years old)
                Gender.MALE,                        // gender
                "123 Test Street, Hanoi",          // address
                "Test student for integration tests" // note
        );

        MvcResult result = mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        // GAP-1524: StudentController.createStudent gained @PreAuthorize
                        // (OWNER/ADMIN/PRINCIPAL/TEACHER/STAFF/PLATFORM_ADMIN) in GAP-1491.
                        // Authenticate fixture setup as ADMIN; no-op without method security.
                        .with(user("fixture-admin").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(studentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data")
                .get("id")
                .asLong();
    }

    /**
     * Creates a test student with custom name, email, and phone.
     *
     * <p>Use this method when you need specific values for name/email/phone
     * (e.g., testing uniqueness constraints, multi-tenant isolation).
     *
     * @param mockMvc MockMvc instance for making HTTP requests
     * @param objectMapper ObjectMapper for JSON serialization/deserialization
     * @param tenantId Tenant ID for multi-tenant isolation
     * @param name Student's full name
     * @param email Student's email address
     * @param phone Student's phone (must be valid Vietnamese format: 0xxxxxxxxx)
     * @return the created student's ID
     * @throws Exception if student creation fails
     */
    public Long createTestStudent(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UUID tenantId,
            String name,
            String email,
            String phone
    ) throws Exception {
        CreateStudentRequest studentRequest = new CreateStudentRequest(
                name,                               // name
                email,                              // email
                phone,                              // phone
                LocalDate.of(2005, 1, 1),          // dateOfBirth
                Gender.MALE,                        // gender
                "123 Test Street, Hanoi",          // address
                "Test student for integration tests" // note
        );

        MvcResult result = mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        // GAP-1524: StudentController.createStudent gained @PreAuthorize
                        // (OWNER/ADMIN/PRINCIPAL/TEACHER/STAFF/PLATFORM_ADMIN) in GAP-1491.
                        // Authenticate fixture setup as ADMIN; no-op without method security.
                        .with(user("fixture-admin").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(studentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data")
                .get("id")
                .asLong();
    }

    /**
     * Creates a test course with default values (basic course, NOT publishable).
     *
     * <p>Default course profile:
     * <ul>
     *     <li>Name: "Test Course"</li>
     *     <li>Code: "TEST-{timestamp}" (unique)</li>
     *     <li>Teacher: provided teacherId</li>
     *     <li>Status: DRAFT</li>
     * </ul>
     *
     * <p><b>Note:</b> This course is NOT publishable because it lacks objectives and durationWeeks.
     * Use {@link #createPublishableCourse} if you need to publish the course.
     *
     * @param mockMvc MockMvc instance for making HTTP requests
     * @param objectMapper ObjectMapper for JSON serialization/deserialization
     * @param tenantId Tenant ID for multi-tenant isolation
     * @param teacherId Teacher ID who creates the course
     * @return the created course's ID
     * @throws Exception if course creation fails
     */
    public Long createTestCourse(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UUID tenantId,
            Long teacherId
    ) throws Exception {
        return createTestCourse(mockMvc, objectMapper, tenantId, "Test Course", teacherId);
    }

    /**
     * Creates a test course with custom name (basic course, NOT publishable).
     *
     * @param mockMvc MockMvc instance for making HTTP requests
     * @param objectMapper ObjectMapper for JSON serialization/deserialization
     * @param tenantId Tenant ID for multi-tenant isolation
     * @param name Course name
     * @param teacherId Teacher ID who creates the course
     * @return the created course's ID
     * @throws Exception if course creation fails
     */
    public Long createTestCourse(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UUID tenantId,
            String name,
            Long teacherId
    ) throws Exception {
        // Generate unique code with timestamp to avoid conflicts
        String code = "TEST-" + System.currentTimeMillis();

        CreateCourseRequest courseRequest = new CreateCourseRequest(
                name,                                   // name
                code,                                   // code
                "Test course description",              // description
                null,                                   // syllabus
                null,                                   // objectives (null = not publishable)
                null,                                   // prerequisites
                null,                                   // targetAudience
                teacherId,                              // teacherId
                null,                                   // durationWeeks (null = not publishable)
                null,                                   // totalSessions
                null,                                   // price
                null,                                   // level
                null                                    // category
        );

        MvcResult result = mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        // GAP-1524: CourseController.createCourse gained @PreAuthorize
                        // (TEACHER/ADMIN/OWNER/STAFF/PLATFORM_ADMIN) in GAP-1491. Authenticate the
                        // fixture setup as ADMIN via the SecurityContext; no-op when the test
                        // context has no @EnableMethodSecurity.
                        .with(user("fixture-admin").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data")
                .get("id")
                .asLong();
    }

    /**
     * Creates a publishable test course with all required fields.
     *
     * <p>This course includes:
     * <ul>
     *     <li>Objectives (required for publishing)</li>
     *     <li>Duration in weeks (required for publishing)</li>
     *     <li>Description and syllabus</li>
     *     <li>Total sessions and price</li>
     * </ul>
     *
     * <p>After creation, you can immediately call {@code POST /api/v1/courses/{id}/publish}
     * without getting validation errors.
     *
     * @param mockMvc MockMvc instance for making HTTP requests
     * @param objectMapper ObjectMapper for JSON serialization/deserialization
     * @param tenantId Tenant ID for multi-tenant isolation
     * @param name Course name
     * @param teacherId Teacher ID who creates the course
     * @return the created course's ID
     * @throws Exception if course creation fails
     */
    public Long createPublishableCourse(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UUID tenantId,
            String name,
            Long teacherId
    ) throws Exception {
        // Generate unique code with timestamp to avoid conflicts
        String code = "PUB-" + System.currentTimeMillis();

        CreateCourseRequest courseRequest = new CreateCourseRequest(
                name,                                   // name
                code,                                   // code
                "Comprehensive test course with all required fields", // description
                "Week 1-4: Fundamentals\nWeek 5-8: Advanced topics", // syllabus
                "Students will learn fundamental and advanced concepts", // objectives (REQUIRED for publish)
                "Basic programming knowledge",          // prerequisites
                "High school students and adults",     // targetAudience
                teacherId,                              // teacherId
                8,                                      // durationWeeks (REQUIRED for publish)
                16,                                     // totalSessions
                BigDecimal.valueOf(5000000),           // price (5,000,000 VND)
                null,                                   // level
                null                                    // category
        );

        MvcResult result = mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        // GAP-1524: CourseController.createCourse gained @PreAuthorize
                        // (TEACHER/ADMIN/OWNER/STAFF/PLATFORM_ADMIN) in GAP-1491. Authenticate the
                        // fixture setup as ADMIN via the SecurityContext; no-op when the test
                        // context has no @EnableMethodSecurity.
                        .with(user("fixture-admin").roles("ADMIN"))
                        .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data")
                .get("id")
                .asLong();
    }

    /**
     * Assigns a teacher to a class with specified role.
     *
     * <p>Creates a TeacherClass relationship to enable teacher permissions
     * for class operations (create assignments, manage students, etc.).
     *
     * @param teacherId the teacher ID
     * @param classId the class ID
     * @param role the teacher's role in the class (MAIN_TEACHER or ASSISTANT_TEACHER)
     * @param tenantId the tenant ID (not used - TeacherClass doesn't have instanceId)
     * @since 2.15
     */
    public void assignTeacherToClass(Long teacherId, Long classId, TeacherClassRole role, UUID tenantId) {
        TeacherClass teacherClass = TeacherClass.builder()
                .teacherId(teacherId)
                .classId(classId)
                .role(role)
                .build();
        teacherClassRepository.save(teacherClass);
    }

    /**
     * Assigns a teacher to a class as MAIN_TEACHER (convenience method).
     *
     * @param teacherId the teacher ID
     * @param classId the class ID
     * @param tenantId the tenant ID
     * @since 2.15
     */
    public void assignMainTeacherToClass(Long teacherId, Long classId, UUID tenantId) {
        assignTeacherToClass(teacherId, classId, TeacherClassRole.MAIN_TEACHER, tenantId);
    }
}
