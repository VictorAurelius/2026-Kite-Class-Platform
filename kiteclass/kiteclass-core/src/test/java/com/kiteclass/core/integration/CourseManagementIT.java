package com.kiteclass.core.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for Course and Class management flow.
 *
 * <p>Tests complete workflow:
 * 1. Create course
 * 2. Publish course
 * 3. Create class under course
 * 4. Verify course-class relationship
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
class CourseManagementIT {

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
    @DisplayName("Create course -> publish -> create class -> verify relationship")
    void shouldCreateCourseAndClassWithRelationship() throws Exception {
        // Step 1: Create publishable course
        Long courseId = testDataBuilder.createPublishableCourse(
                mockMvc, objectMapper, tenantId, "Course Management IT", teacherId);

        // Step 2: Verify course was created
        mockMvc.perform(get("/api/v1/courses/" + courseId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(courseId))
                .andExpect(jsonPath("$.data.name").value("Course Management IT"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        // Step 3: Publish course
        mockMvc.perform(post("/api/v1/courses/" + courseId + "/publish")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // Step 4: Create class under the course
        CreateClassRequest classRequest = new CreateClassRequest(
                "Course Mgmt IT Class - Spring 2026",
                "Spring 2026",
                null,
                null,
                null,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(120),
                25
        );

        MvcResult classResult = mockMvc.perform(post("/api/v1/courses/" + courseId + "/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(classRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Course Mgmt IT Class - Spring 2026"))
                .andReturn();

        Long classId = objectMapper.readTree(classResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Step 5: Verify class belongs to the course
        mockMvc.perform(get("/api/v1/classes/" + classId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(classId))
                .andExpect(jsonPath("$.data.courseId").value(courseId));

        // Step 6: Verify course lists its classes
        mockMvc.perform(get("/api/v1/courses/" + courseId + "/classes")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[?(@.id == " + classId + ")]").exists());
    }

    @Test
    @DisplayName("Create multiple classes under one course -> verify all listed")
    void shouldCreateMultipleClassesUnderCourse() throws Exception {
        // Create and publish course
        Long courseId = testDataBuilder.createPublishableCourse(
                mockMvc, objectMapper, tenantId, "Multi Class Course", teacherId);

        mockMvc.perform(post("/api/v1/courses/" + courseId + "/publish")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk());

        // Create class 1
        CreateClassRequest class1Request = new CreateClassRequest(
                "Class A - Spring 2026",
                "Spring 2026",
                null,
                null,
                null,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(120),
                20
        );

        MvcResult class1Result = mockMvc.perform(post("/api/v1/courses/" + courseId + "/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(class1Request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long class1Id = objectMapper.readTree(class1Result.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Create class 2
        CreateClassRequest class2Request = new CreateClassRequest(
                "Class B - Summer 2026",
                "Summer 2026",
                null,
                null,
                null,
                LocalDate.now().plusDays(130),
                LocalDate.now().plusDays(240),
                15
        );

        MvcResult class2Result = mockMvc.perform(post("/api/v1/courses/" + courseId + "/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(class2Request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long class2Id = objectMapper.readTree(class2Result.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Verify course has both classes
        mockMvc.perform(get("/api/v1/courses/" + courseId + "/classes")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[?(@.id == " + class1Id + ")]").exists())
                .andExpect(jsonPath("$.data.content[?(@.id == " + class2Id + ")]").exists());
    }
}
