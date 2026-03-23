package com.kiteclass.core.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.testutil.TestDataBuilder;
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
 * Integration test for Student Enrollment flow.
 *
 * <p>Tests complete workflow:
 * 1. Create student
 * 2. List students and verify student exists
 * 3. Create course + class + enroll student
 * 4. Verify enrollment
 *
 * <p>Uses Testcontainers for real database, MockMvc for HTTP requests.
 *
 * @author KiteClass Team
 * @since 2.16
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
class StudentEnrollmentIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataBuilder testDataBuilder;

    private UUID tenantId;
    private Long teacherId;

    @BeforeEach
    void setUp() throws Exception {
        tenantId = UUID.randomUUID();
        teacherId = testDataBuilder.createTestTeacher(mockMvc, objectMapper, tenantId);
    }

    @Test
    @DisplayName("Create student -> list students -> verify student exists")
    void shouldCreateAndListStudents() throws Exception {
        // Step 1: Create student
        Long studentId = testDataBuilder.createTestStudent(
                mockMvc, objectMapper, tenantId,
                "Enrollment Test Student",
                "enroll.test." + System.currentTimeMillis() + "@kiteclass.test",
                "0901112233"
        );

        // Step 2: List students and verify the created student exists
        mockMvc.perform(get("/api/v1/students")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[?(@.id == " + studentId + ")]").exists())
                .andExpect(jsonPath("$.data.content[?(@.name == 'Enrollment Test Student')]").exists());

        // Step 3: Get student by ID
        mockMvc.perform(get("/api/v1/students/" + studentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(studentId))
                .andExpect(jsonPath("$.data.name").value("Enrollment Test Student"));
    }

    @Test
    @DisplayName("Create student -> enroll in class -> verify enrollment")
    void shouldEnrollStudentInClass() throws Exception {
        // Step 1: Create student
        Long studentId = testDataBuilder.createTestStudent(
                mockMvc, objectMapper, tenantId,
                "Enroll Student",
                "enroll.student." + System.currentTimeMillis() + "@kiteclass.test",
                "0902223344"
        );

        // Step 2: Create publishable course and publish it
        Long courseId = testDataBuilder.createPublishableCourse(
                mockMvc, objectMapper, tenantId, "Enrollment Course", teacherId);

        mockMvc.perform(post("/api/v1/courses/" + courseId + "/publish")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk());

        // Step 3: Create class
        CreateClassRequest classRequest = new CreateClassRequest(
                "Enrollment Class - Spring 2026",
                "Spring 2026",
                null,
                null,
                null,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(120),
                30
        );

        MvcResult classResult = mockMvc.perform(post("/api/v1/courses/" + courseId + "/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(classRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long classId = objectMapper.readTree(classResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Step 4: Enroll student
        CreateEnrollmentRequest enrollRequest = CreateEnrollmentRequest.builder()
                .studentId(studentId)
                .classId(classId)
                .tuitionAmount(BigDecimal.valueOf(5000000))
                .build();

        MvcResult enrollResult = mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(enrollRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value(studentId))
                .andExpect(jsonPath("$.data.classId").value(classId))
                .andReturn();

        Long enrollmentId = objectMapper.readTree(enrollResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Step 5: Verify enrollment exists
        mockMvc.perform(get("/api/v1/enrollments/" + enrollmentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value(studentId))
                .andExpect(jsonPath("$.data.classId").value(classId));

        // Step 6: Verify enrollment appears in student's enrollment list
        mockMvc.perform(get("/api/v1/enrollments/student/" + studentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[?(@.id == " + enrollmentId + ")]").exists());
    }
}
