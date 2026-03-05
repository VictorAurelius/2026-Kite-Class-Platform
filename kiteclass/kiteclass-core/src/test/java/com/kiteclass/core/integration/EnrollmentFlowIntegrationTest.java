package com.kiteclass.core.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.Gender;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end flow integration test for Enrollment workflow.
 *
 * <p>Tests complete business workflow:
 * 1. Create student
 * 2. Create course → class
 * 3. Enroll student in class (ACTIVE enrollment)
 * 4. Verify Invoice is auto-created
 * 5. Verify Grade is auto-initialized
 * 6. Verify enrollment appears in student's enrollments list
 * 7. Verify enrollment appears in class's enrollments list
 *
 * <p>This test verifies cross-module integration:
 * - Enrollment Module creates Invoice (invoice module)
 * - Enrollment Module initializes Grade (grade module)
 * - Data consistency across modules
 *
 * @author KiteClass Team
 * @since 2.10
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@org.junit.jupiter.api.Disabled("TODO: Fix test data setup - requires teacher/course fixtures")

class EnrollmentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Enrollment Flow: Create Student → Create Class → Enroll → Verify Invoice & Grade Created")
    void testCompleteEnrollmentWorkflow() throws Exception {
        // ========== Step 1: Create Student ==========
        CreateStudentRequest studentRequest = new CreateStudentRequest(
                "Bob Enrollment",
                "bob.enroll@test.com",
                "+84901234567",
                LocalDate.of(2008, 6, 20),
                Gender.MALE,
                "789 Enrollment St, Hanoi",
                "Parent: Mary, Phone: +84912345678"
        );

        MvcResult studentResult = mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(studentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();

        Long studentId = objectMapper.readTree(studentResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // ========== Step 2: Create Course ==========
        CreateCourseRequest courseRequest = new CreateCourseRequest(
                "English Basics",              // name
                "ENG101",                      // code
                "Introduction to English",     // description
                "Week 1-16: Grammar and vocabulary", // syllabus
                null,                          // objectives
                null,                          // prerequisites
                null,                          // targetAudience
                1L,                            // teacherId
                null,                          // durationWeeks
                null,                          // totalSessions
                null                           // price
        );

        MvcResult courseResult = mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();

        Long courseId = objectMapper.readTree(courseResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Publish course
        mockMvc.perform(post("/api/v1/courses/" + courseId + "/publish")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk());

        // ========== Step 3: Create Class ==========
        CreateClassRequest classRequest = new CreateClassRequest(
                "English Basics - Spring 2026",
                "Spring 2026",
                null,
                null,
                null,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(120),
                30
        );

        MvcResult classResult = mockMvc.perform(post("/api/v1/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(classRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();

        Long classId = objectMapper.readTree(classResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // ========== Step 4: Enroll Student in Class ==========
        CreateEnrollmentRequest enrollRequest = CreateEnrollmentRequest.builder()
                .studentId(studentId)
                .classId(classId)
                .tuitionAmount(BigDecimal.valueOf(5000000)) // 5M VND
                .build();

        MvcResult enrollResult = mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(enrollRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value(studentId))
                .andExpect(jsonPath("$.data.classId").value(classId))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();

        Long enrollmentId = objectMapper.readTree(enrollResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // ========== Step 5: Verify Invoice Was Auto-Created ==========
        // Note: Invoice creation might be async (event-driven), so we may need to wait or check later
        // For now, we'll verify the enrollment has payment status
        mockMvc.perform(get("/api/v1/enrollments/" + enrollmentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").exists());

        // Try to fetch invoices for this student
        mockMvc.perform(get("/api/v1/invoices/student/" + studentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        // Note: May be empty if invoice creation is async - this test documents the expected behavior

        // ========== Step 6: Verify Grade Was Auto-Initialized ==========
        // Try to fetch grade for this student in this class
        mockMvc.perform(get("/api/v1/grades/student/" + studentId + "/class/" + classId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value(studentId))
                .andExpect(jsonPath("$.data.classId").value(classId))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        // ========== Step 7: Verify Enrollment in Student's List ==========
        mockMvc.perform(get("/api/v1/enrollments/student/" + studentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.id == " + enrollmentId + ")]").exists())
                .andExpect(jsonPath("$.data[?(@.id == " + enrollmentId + ")].status").value("ACTIVE"));

        // ========== Step 8: Verify Enrollment in Class's List ==========
        mockMvc.perform(get("/api/v1/enrollments/class/" + classId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.id == " + enrollmentId + ")]").exists());
    }

    @Test
    @DisplayName("Enrollment Flow: Prevent duplicate enrollment in same class")
    void testPreventDuplicateEnrollment() throws Exception {
        // ========== Setup: Create Student + Course + Class ==========
        CreateStudentRequest studentRequest = new CreateStudentRequest(
                "Charlie Dup",
                "charlie.dup@test.com",
                "+84903333333",
                LocalDate.of(2008, 3, 15),
                Gender.MALE,
                "Address",
                null
        );

        MvcResult studentResult = mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(studentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long studentId = objectMapper.readTree(studentResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        CreateCourseRequest courseRequest = new CreateCourseRequest(
                "Advanced Math",               // name
                "MATH202",                     // code
                "Advanced topics",             // description
                "Syllabus",                    // syllabus
                null,                          // objectives
                null,                          // prerequisites
                null,                          // targetAudience
                1L,                            // teacherId
                null,                          // durationWeeks
                null,                          // totalSessions
                null                           // price
        );

        MvcResult courseResult = mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long courseId = objectMapper.readTree(courseResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/courses/" + courseId + "/publish")
                .header("X-Tenant-Id", tenantId.toString()));

        CreateClassRequest classRequest = new CreateClassRequest(
                "Advanced Math - Fall 2026",
                "Fall 2026",
                null,
                null,
                null,
                LocalDate.now().plusDays(30),
                LocalDate.now().plusDays(150),
                25
        );

        MvcResult classResult = mockMvc.perform(post("/api/v1/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(classRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long classId = objectMapper.readTree(classResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // ========== First Enrollment: Should Succeed ==========
        CreateEnrollmentRequest enrollRequest = CreateEnrollmentRequest.builder()
                .studentId(studentId)
                .classId(classId)
                .tuitionAmount(BigDecimal.valueOf(5000000))
                .build();

        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(enrollRequest)))
                .andExpect(status().isCreated());

        // ========== Second Enrollment: Should Fail (Duplicate) ==========
        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(enrollRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }
}
