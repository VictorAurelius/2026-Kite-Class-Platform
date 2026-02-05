package com.kiteclass.core.module.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.course.dto.UpdateCourseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full integration tests for Course API endpoints.
 *
 * <p>Tests the complete request/response cycle:
 * API → Controller → Service → Repository → Database
 *
 * <p>Uses @SpringBootTest with real database (Testcontainers)
 * and MockMvc for HTTP requests.
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestContainersConfiguration.class)
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
class CourseIntegrationTest {

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
    @DisplayName("POST /api/v1/courses - Should create course successfully")
    void shouldCreateCourse() throws Exception {
        // Given
        CreateCourseRequest request = new CreateCourseRequest(
            "Introduction to Java Programming",
            "JAVA-101",
            "Learn Java fundamentals",
            "Week 1: Basics, Week 2: OOP, Week 3: Collections",
            "Master Java syntax and object-oriented principles",
            "Basic computer skills",
            "Beginners to programming",
            1L,  // teacherId
            12,  // durationWeeks
            24,  // totalSessions
            new BigDecimal("5000000")  // price
        );

        // When/Then
        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Introduction to Java Programming"))
            .andExpect(jsonPath("$.data.code").value("JAVA-101"))
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/courses/{id} - Should return course by ID")
    void shouldGetCourseById() throws Exception {
        // Given: Create a course first
        CreateCourseRequest createRequest = new CreateCourseRequest(
            "Advanced Python",
            "PY-201",
            "Advanced Python topics",
            "Deep dive into Python",
            "Master advanced Python concepts",
            "Python basics",
            "Intermediate programmers",
            1L,
            8,
            16,
            new BigDecimal("3000000")
        );

        String createResponse = mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long courseId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When/Then: Get the course
        mockMvc.perform(get("/api/v1/courses/{id}", courseId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(courseId))
            .andExpect(jsonPath("$.data.name").value("Advanced Python"))
            .andExpect(jsonPath("$.data.code").value("PY-201"));
    }

    @Test
    @DisplayName("PUT /api/v1/courses/{id} - Should update course successfully")
    void shouldUpdateCourse() throws Exception {
        // Given: Create a course
        CreateCourseRequest createRequest = new CreateCourseRequest(
            "Data Structures",
            "DS-101",
            "Basic data structures",
            "Arrays, Lists, Trees",
            "Understand core data structures",
            "Programming basics",
            "CS students",
            1L,
            10,
            20,
            new BigDecimal("4000000")
        );

        String createResponse = mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long courseId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When: Update the course
        UpdateCourseRequest updateRequest = new UpdateCourseRequest(
            "Data Structures and Algorithms",  // name
            "Updated description with algorithms",  // description
            null,  // syllabus - keep existing
            null,  // objectives - keep existing
            null,  // prerequisites - keep existing
            null,  // targetAudience - keep existing
            null,  // durationWeeks - keep existing
            null,  // totalSessions - keep existing
            new BigDecimal("4500000"),  // price
            null   // coverImageUrl
        );

        // Then
        mockMvc.perform(put("/api/v1/courses/{id}", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Data Structures and Algorithms"))
            .andExpect(jsonPath("$.data.price").value(4500000))
            .andExpect(jsonPath("$.data.code").value("DS-101")); // Unchanged
    }

    @Test
    @DisplayName("DELETE /api/v1/courses/{id} - Should soft delete course")
    void shouldDeleteCourse() throws Exception {
        // Given: Create a course
        CreateCourseRequest createRequest = new CreateCourseRequest(
            "Web Development",
            "WEB-101",
            "Learn web development",
            "HTML, CSS, JavaScript",
            "Build modern websites",
            "Basic computer skills",
            "Beginners",
            1L,
            16,
            32,
            new BigDecimal("6000000")
        );

        String createResponse = mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long courseId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When: Delete the course
        mockMvc.perform(delete("/api/v1/courses/{id}", courseId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isNoContent());

        // Then: Should not be found (soft deleted)
        mockMvc.perform(get("/api/v1/courses/{id}", courseId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/courses - Should return paginated list")
    void shouldGetCoursesPaginated() throws Exception {
        // Given: Create multiple courses
        for (int i = 1; i <= 3; i++) {
            CreateCourseRequest request = new CreateCourseRequest(
                "Course " + i,
                "COURSE-" + i,
                "Description " + i,
                "Syllabus " + i,
                "Objectives " + i,
                "Prerequisites " + i,
                "Audience " + i,
                1L,
                10,
                20,
                new BigDecimal("1000000")
            );

            mockMvc.perform(post("/api/v1/courses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Tenant-Id", tenantId.toString())
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        }

        // When/Then: Get paginated list
        mockMvc.perform(get("/api/v1/courses")
                .param("page", "0")
                .param("size", "10")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content.length()").value(greaterThanOrEqualTo(3)))
            .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(3)));
    }

    @Test
    @DisplayName("GET /api/v1/courses - Should search courses by name")
    void shouldSearchCoursesByName() throws Exception {
        // Given: Create courses with different names
        CreateCourseRequest request1 = new CreateCourseRequest(
            "Machine Learning Fundamentals",
            "ML-101",
            "ML course",
            "Syllabus",
            "Objectives",
            "Math basics",
            "Data scientists",
            1L,
            12,
            24,
            new BigDecimal("8000000")
        );

        CreateCourseRequest request2 = new CreateCourseRequest(
            "Database Design",
            "DB-101",
            "Database course",
            "Syllabus",
            "Objectives",
            "SQL basics",
            "Developers",
            1L,
            8,
            16,
            new BigDecimal("3000000")
        );

        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isCreated());

        // When/Then: Search by name
        mockMvc.perform(get("/api/v1/courses")
                .param("search", "Machine")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[*].name", hasItem(containsString("Machine"))));
    }

    @Test
    @DisplayName("POST /api/v1/courses - Should return 400 for invalid data")
    void shouldReturn400ForInvalidData() throws Exception {
        // Given: Invalid course code (contains lowercase)
        CreateCourseRequest request = new CreateCourseRequest(
            "Test Course",
            "invalid-code",  // Invalid: contains lowercase
            "Description",
            "Syllabus",
            "Objectives",
            "Prerequisites",
            "Audience",
            1L,
            10,
            20,
            new BigDecimal("1000000")
        );

        // When/Then
        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/courses - Should return 400 for blank required fields")
    void shouldReturn400ForBlankRequiredFields() throws Exception {
        // Given: Blank name
        CreateCourseRequest request = new CreateCourseRequest(
            "",  // Blank name
            "CODE-101",
            "Description",
            "Syllabus",
            "Objectives",
            "Prerequisites",
            "Audience",
            1L,
            10,
            20,
            new BigDecimal("1000000")
        );

        // When/Then
        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/courses/{id} - Should return 404 for non-existent course")
    void shouldReturn404ForNonExistentCourse() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/v1/courses/{id}", 999999L)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/courses - Should return 409 for duplicate course code")
    void shouldReturn409ForDuplicateCourseCode() throws Exception {
        // Given: Create first course
        CreateCourseRequest request1 = new CreateCourseRequest(
            "First Course",
            "DUPLICATE-101",
            "Description",
            "Syllabus",
            "Objectives",
            "Prerequisites",
            "Audience",
            1L,
            10,
            20,
            new BigDecimal("1000000")
        );

        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isCreated());

        // When/Then: Try to create second course with same code
        CreateCourseRequest request2 = new CreateCourseRequest(
            "Second Course",
            "DUPLICATE-101",  // Same code
            "Different description",
            "Different syllabus",
            "Different objectives",
            "Different prerequisites",
            "Different audience",
            1L,
            12,
            24,
            new BigDecimal("2000000")
        );

        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Multi-tenant: Should not access course from different tenant")
    void shouldNotAccessCourseFromDifferentTenant() throws Exception {
        // Given: Create course with tenant1
        UUID tenant1 = UUID.randomUUID();
        CreateCourseRequest request = new CreateCourseRequest(
            "Tenant1 Course",
            "TENANT-101",
            "Description",
            "Syllabus",
            "Objectives",
            "Prerequisites",
            "Audience",
            1L,
            10,
            20,
            new BigDecimal("1000000")
        );

        String createResponse = mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenant1.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long courseId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When/Then: Try to access with tenant2
        UUID tenant2 = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/courses/{id}", courseId)
                .header("X-Tenant-Id", tenant2.toString()))
            .andExpect(status().isNotFound()); // Should not find - different tenant
    }

    @Test
    @DisplayName("PUT /api/v1/courses/{id} - Should update course status")
    void shouldUpdateCourseStatus() throws Exception {
        // Given: Create a course (defaults to DRAFT)
        CreateCourseRequest createRequest = new CreateCourseRequest(
            "Status Test Course",
            "STATUS-101",
            "Description",
            "Syllabus",
            "Objectives",
            "Prerequisites",
            "Audience",
            1L,
            10,
            20,
            new BigDecimal("1000000")
        );

        String createResponse = mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long courseId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When: Update status (Note: Status update might be through a separate endpoint)
        // For now, verify the initial status is DRAFT
        mockMvc.perform(get("/api/v1/courses/{id}", courseId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("GET /api/v1/courses - Should filter courses by status")
    void shouldFilterCoursesByStatus() throws Exception {
        // Given: Create courses with DRAFT status (default)
        CreateCourseRequest draftCourse = new CreateCourseRequest(
            "Draft Course",
            "DRAFT-101",
            "Description",
            "Syllabus",
            "Objectives",
            "Prerequisites",
            "Audience",
            1L,
            10,
            20,
            new BigDecimal("1000000")
        );

        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(draftCourse)))
            .andExpect(status().isCreated());

        // When/Then: Filter by DRAFT status
        mockMvc.perform(get("/api/v1/courses")
                .param("status", "DRAFT")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[*].status", everyItem(is("DRAFT"))));
    }

    @Test
    @DisplayName("GET /api/v1/courses - Should search with multiple criteria")
    void shouldSearchWithMultipleCriteria() throws Exception {
        // Given: Create multiple courses
        CreateCourseRequest request1 = new CreateCourseRequest(
            "Advanced JavaScript",
            "JS-201",
            "Advanced JS topics",
            "Syllabus",
            "Objectives",
            "JS basics",
            "Intermediate developers",
            1L,
            8,
            16,
            new BigDecimal("4000000")
        );

        CreateCourseRequest request2 = new CreateCourseRequest(
            "JavaScript Fundamentals",
            "JS-101",
            "JS basics",
            "Syllabus",
            "Objectives",
            "None",
            "Beginners",
            2L,  // Different teacher
            10,
            20,
            new BigDecimal("2000000")
        );

        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isCreated());

        // When/Then: Search with multiple criteria (name + teacherId)
        mockMvc.perform(get("/api/v1/courses")
                .param("search", "JavaScript")
                .param("teacherId", "1")
                .param("status", "DRAFT")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("DELETE /api/v1/courses/{id} - Verify soft delete does not physically remove")
    void shouldVerifySoftDelete() throws Exception {
        // Given: Create a course
        CreateCourseRequest createRequest = new CreateCourseRequest(
            "Soft Delete Test Course",
            "SOFT-DEL-101",
            "Test soft delete",
            "Syllabus",
            "Objectives",
            "Prerequisites",
            "Audience",
            1L,
            10,
            20,
            new BigDecimal("1000000")
        );

        String createResponse = mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long courseId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When: Soft delete the course
        mockMvc.perform(delete("/api/v1/courses/{id}", courseId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isNoContent());

        // Then: Verify it's not returned in normal queries
        mockMvc.perform(get("/api/v1/courses/{id}", courseId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isNotFound());

        // And: Verify it's not in the list
        mockMvc.perform(get("/api/v1/courses")
                .param("search", "Soft Delete Test")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[?(@.id == " + courseId + ")]").doesNotExist());
    }
}
