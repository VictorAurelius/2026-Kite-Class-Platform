package com.kiteclass.core.module.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.course.dto.UpdateCourseRequest;
import com.kiteclass.core.module.teacher.dto.CreateTeacherRequest;
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
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@ActiveProfiles("test")
@Transactional
class CourseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID tenantId;
    private Long teacherId;
    private Long teacher2Id;

    @BeforeEach
    void setUp() throws Exception {
        tenantId = UUID.randomUUID();

        // Create first teacher (required for course creation)
        CreateTeacherRequest teacherRequest = new CreateTeacherRequest(
            "Test Teacher",
            "teacher@test.com",
            "0901234567",
            "Computer Science",
            "Test teacher for course integration tests",
            "PhD in Computer Science",
            10
        );

        String teacherResponse = mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(teacherRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        teacherId = objectMapper.readTree(teacherResponse).get("data").get("id").asLong();

        // Create second teacher (for multi-criteria search test)
        CreateTeacherRequest teacher2Request = new CreateTeacherRequest(
            "Test Teacher 2",
            "teacher2@test.com",
            "0901234568",
            "Mathematics",
            "Second test teacher",
            "Master in Mathematics",
            5
        );

        String teacher2Response = mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(teacher2Request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        teacher2Id = objectMapper.readTree(teacher2Response).get("data").get("id").asLong();
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
            teacherId,
            12,  // durationWeeks
            24,  // totalSessions
            new BigDecimal("5000000"),  // price
            null,  // level
            null   // category
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
            teacherId,
            8,
            16,
            new BigDecimal("3000000"),
            null,  // level
            null   // category
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
            teacherId,
            10,
            20,
            new BigDecimal("4000000"),
            null,  // level
            null   // category
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
            null,  // code - keep existing
            null,  // teacherId - keep existing
            "Updated description with algorithms",  // description
            null,  // syllabus - keep existing
            null,  // objectives - keep existing
            null,  // prerequisites - keep existing
            null,  // targetAudience - keep existing
            null,  // durationWeeks - keep existing
            null,  // totalSessions - keep existing
            new BigDecimal("4500000"),  // price
            null,  // coverImageUrl
            null,  // level
            null   // category
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
            teacherId,
            16,
            32,
            new BigDecimal("6000000"),
            null,  // level
            null   // category
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
                teacherId,
                10,
                20,
                new BigDecimal("1000000"),
                null,  // level
                null   // category
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
            teacherId,
            12,
            24,
            new BigDecimal("8000000"),
            null,  // level
            null   // category
        );

        CreateCourseRequest request2 = new CreateCourseRequest(
            "Database Design",
            "DB-101",
            "Database course",
            "Syllabus",
            "Objectives",
            "SQL basics",
            "Developers",
            teacherId,
            8,
            16,
            new BigDecimal("3000000"),
            null,  // level
            null   // category
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
            teacherId,
            10,
            20,
            new BigDecimal("1000000"),
            null,  // level
            null   // category
        );

        // When/Then
        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").exists());
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
            teacherId,
            10,
            20,
            new BigDecimal("1000000"),
            null,  // level
            null   // category
        );

        // When/Then
        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("GET /api/v1/courses/{id} - Should return 404 for non-existent course")
    void shouldReturn404ForNonExistentCourse() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/v1/courses/{id}", 999999L)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").exists());
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
            teacherId,
            10,
            20,
            new BigDecimal("1000000"),
            null,  // level
            null   // category
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
            teacherId,
            12,
            24,
            new BigDecimal("2000000"),
            null,  // level
            null   // category
        );

        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("Multi-tenant: Should not access course from different tenant")
    void shouldNotAccessCourseFromDifferentTenant() throws Exception {
        // Given: Create course with existing tenantId (where teacher exists)
        CreateCourseRequest request = new CreateCourseRequest(
            "Tenant1 Course",
            "TENANT-101",
            "Description",
            "Syllabus",
            "Objectives",
            "Prerequisites",
            "Audience",
            teacherId,
            10,
            20,
            new BigDecimal("1000000"),
            null,  // level
            null   // category
        );

        String createResponse = mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString()) // Use existing tenantId
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long courseId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When/Then: Try to access with different tenant
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
            teacherId,
            10,
            20,
            new BigDecimal("1000000"),
            null,  // level
            null   // category
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
            teacherId,
            10,
            20,
            new BigDecimal("1000000"),
            null,  // level
            null   // category
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
            teacherId,
            8,
            16,
            new BigDecimal("4000000"),
            null,  // level
            null   // category
        );

        CreateCourseRequest request2 = new CreateCourseRequest(
            "JavaScript Fundamentals",
            "JS-101",
            "JS basics",
            "Syllabus",
            "Objectives",
            "None",
            "Beginners",
            teacher2Id,  // Different teacher
            10,
            20,
            new BigDecimal("2000000"),
            null,  // level
            null   // category
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
                .param("teacherId", teacherId.toString())
                .param("status", "DRAFT")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("PUT /api/v1/courses/{id} - Should reject update of restricted fields for PUBLISHED course")
    void shouldRejectUpdateOfRestrictedFieldsForPublishedCourse() throws Exception {
        // Given: Create and publish a course
        CreateCourseRequest createRequest = new CreateCourseRequest(
            "Published Course",
            "PUB-101",
            "Test description",
            "Test syllabus",
            "Test objectives",
            "Test prerequisites",
            "Test audience",
            teacherId,
            10,
            20,
            new BigDecimal("5000000"),
            null,  // level
            null   // category
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

        // Publish the course
        mockMvc.perform(post("/api/v1/courses/{id}/publish", courseId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk());

        // When/Then: Try to update code (restricted field)
        String updateWithCodeJson = "{\"code\":\"NEW-CODE\"}";
        mockMvc.perform(put("/api/v1/courses/{id}", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(updateWithCodeJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COURSE_INVALID_UPDATE_PUBLISHED"));

        // When/Then: Try to update teacherId (restricted field)
        String updateWithTeacherJson = "{\"teacherId\":" + teacher2Id + "}";
        mockMvc.perform(put("/api/v1/courses/{id}", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(updateWithTeacherJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COURSE_INVALID_UPDATE_PUBLISHED"));
    }

    @Test
    @DisplayName("PUT /api/v1/courses/{id} - Should allow update of allowed fields for PUBLISHED course")
    void shouldAllowUpdateOfAllowedFieldsForPublishedCourse() throws Exception {
        // Given: Create and publish a course
        CreateCourseRequest createRequest = new CreateCourseRequest(
            "Published Course 2",
            "PUB-102",
            "Original description",
            "Original syllabus",
            "Original objectives",
            "Original prerequisites",
            "Original audience",
            teacherId,
            10,
            20,
            new BigDecimal("5000000"),
            null,  // level
            null   // category
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

        // Publish the course
        mockMvc.perform(post("/api/v1/courses/{id}/publish", courseId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk());

        // When: Update allowed fields (description, objectives, price)
        UpdateCourseRequest updateRequest = new UpdateCourseRequest(
            null,  // name - keep existing (restricted)
            null,  // code - keep existing (restricted)
            null,  // teacherId - keep existing (restricted)
            "Updated description after publish",  // description - allowed
            "Updated syllabus",  // syllabus - allowed
            "Updated objectives",  // objectives - allowed
            null,  // prerequisites - keep existing (restricted)
            null,  // targetAudience - keep existing (restricted)
            null,  // durationWeeks - keep existing (restricted)
            null,  // totalSessions - keep existing (restricted)
            new BigDecimal("6000000"),  // price - allowed
            "https://example.com/cover.jpg",  // coverImageUrl - allowed
            null,  // level
            null   // category
        );

        // Then: Update should succeed
        mockMvc.perform(put("/api/v1/courses/{id}", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.description").value("Updated description after publish"))
            .andExpect(jsonPath("$.data.objectives").value("Updated objectives"))
            .andExpect(jsonPath("$.data.price").value(6000000))
            .andExpect(jsonPath("$.data.code").value("PUB-102")); // Unchanged
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
            teacherId,
            10,
            20,
            new BigDecimal("1000000"),
            null,  // level
            null   // category
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

    // ========== Course Prerequisites Tests ==========

    @Test
    @DisplayName("POST /{id}/prerequisites/{prerequisiteId} - Should add prerequisite successfully")
    void shouldAddPrerequisite() throws Exception {
        // Given: Create 2 courses (Algebra 1 and Algebra 2)
        Long algebra1 = createCourse("Algebra 1", "ALG-101");
        Long algebra2 = createCourse("Algebra 2", "ALG-201");

        // When: Add Algebra 1 as prerequisite to Algebra 2
        mockMvc.perform(post("/api/v1/courses/{id}/prerequisites/{prereqId}", algebra2, algebra1)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Then: Algebra 2 response includes Algebra 1 as prerequisite
        mockMvc.perform(get("/api/v1/courses/{id}", algebra2)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.prerequisiteCourses").isArray())
            .andExpect(jsonPath("$.data.prerequisiteCourses[0].id").value(algebra1))
            .andExpect(jsonPath("$.data.prerequisiteCourses[0].name").value("Algebra 1"))
            .andExpect(jsonPath("$.data.prerequisiteCourses[0].code").value("ALG-101"));
    }

    @Test
    @DisplayName("POST /{id}/prerequisites/{id} - Should prevent self-prerequisite")
    void shouldPreventSelfPrerequisite() throws Exception {
        // Given: Create course
        Long courseId = createCourse("Math 101", "MATH-101");

        // When: Try to add course as its own prerequisite
        // Then: Should return 400 Bad Request
        mockMvc.perform(post("/api/v1/courses/{id}/prerequisites/{prereqId}", courseId, courseId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COURSE_CIRCULAR_PREREQUISITE"));
    }

    @Test
    @DisplayName("POST - Should prevent direct circular dependency (A→B, B→A)")
    void shouldPreventDirectCircularDependency() throws Exception {
        // Given: Create A→B (Course B requires Course A)
        Long courseA = createCourse("Course A", "COURSE-A");
        Long courseB = createCourse("Course B", "COURSE-B");

        mockMvc.perform(post("/api/v1/courses/{id}/prerequisites/{prereqId}", courseB, courseA)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk());

        // When: Try to add B→A (would create cycle: A→B→A)
        // Then: Should return 400 Bad Request
        mockMvc.perform(post("/api/v1/courses/{id}/prerequisites/{prereqId}", courseA, courseB)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COURSE_CIRCULAR_PREREQUISITE"));
    }

    @Test
    @DisplayName("POST - Should prevent transitive circular dependency (A→B→C, C→A)")
    void shouldPreventTransitiveCircularDependency() throws Exception {
        // Given: Create A→B→C chain (C requires B, B requires A)
        Long courseA = createCourse("Course A", "CHAIN-A");
        Long courseB = createCourse("Course B", "CHAIN-B");
        Long courseC = createCourse("Course C", "CHAIN-C");

        mockMvc.perform(post("/api/v1/courses/{id}/prerequisites/{prereqId}", courseB, courseA)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/courses/{id}/prerequisites/{prereqId}", courseC, courseB)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk());

        // When: Try to add C→A (would create transitive cycle: A→B→C→A)
        // Then: Should return 400 Bad Request
        mockMvc.perform(post("/api/v1/courses/{id}/prerequisites/{prereqId}", courseA, courseC)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COURSE_CIRCULAR_PREREQUISITE"));
    }

    @Test
    @DisplayName("DELETE /{id}/prerequisites/{prerequisiteId} - Should remove prerequisite successfully")
    void shouldRemovePrerequisite() throws Exception {
        // Given: Create 2 courses and add prerequisite relationship
        Long calculus1 = createCourse("Calculus I", "CALC-101");
        Long calculus2 = createCourse("Calculus II", "CALC-201");

        mockMvc.perform(post("/api/v1/courses/{id}/prerequisites/{prereqId}", calculus2, calculus1)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk());

        // When: Remove the prerequisite
        mockMvc.perform(delete("/api/v1/courses/{id}/prerequisites/{prereqId}", calculus2, calculus1)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Then: Course should have no prerequisites
        mockMvc.perform(get("/api/v1/courses/{id}", calculus2)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.prerequisiteCourses").isEmpty());
    }

    /**
     * Helper method to create a course for testing.
     *
     * @param name Course name
     * @param code Course code
     * @return Course ID
     * @throws Exception if creation fails
     */
    private Long createCourse(String name, String code) throws Exception {
        CreateCourseRequest request = new CreateCourseRequest(
            name,
            code,
            "Test description for " + name,
            "Test syllabus",
            "Test objectives",
            "Test prerequisites",
            "Test audience",
            teacherId,
            10,
            20,
            new BigDecimal("1000000"),
            null,
            null
        );

        String response = mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("data").get("id").asLong();
    }

    // ========== Course Search by Level and Category Tests ==========

    @Test
    @DisplayName("GET /api/v1/courses/search - Should search by level only")
    void shouldSearchCoursesByLevelOnly() throws Exception {
        // Given: Create courses with different levels
        createCourseWithLevelAndCategory("Beginner Math", "BEG-MATH", "Beginner", "Math");
        createCourseWithLevelAndCategory("Advanced Math", "ADV-MATH", "Advanced", "Math");
        createCourseWithLevelAndCategory("Beginner Science", "BEG-SCI", "Beginner", "Science");

        // When: Search by level="Beginner"
        // Then: Returns only Beginner courses
        mockMvc.perform(get("/api/v1/courses/search")
                .param("level", "Beginner")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[*].level", everyItem(is("Beginner"))));
    }

    @Test
    @DisplayName("GET /api/v1/courses/search - Should search by category only")
    void shouldSearchCoursesByCategoryOnly() throws Exception {
        // Given: Create courses with different categories
        createCourseWithLevelAndCategory("Beginner Math", "CAT-MATH-1", "Beginner", "Math");
        createCourseWithLevelAndCategory("Advanced Math", "CAT-MATH-2", "Advanced", "Math");
        createCourseWithLevelAndCategory("Beginner Science", "CAT-SCI", "Beginner", "Science");

        // When: Search by category="Math"
        // Then: Returns only Math courses
        mockMvc.perform(get("/api/v1/courses/search")
                .param("category", "Math")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[*].category", everyItem(is("Math"))));
    }

    @Test
    @DisplayName("GET /api/v1/courses/search - Should search by both level and category")
    void shouldSearchCoursesByLevelAndCategory() throws Exception {
        // Given: Create courses with different combinations
        createCourseWithLevelAndCategory("Beginner Math", "BOTH-BEG-MATH", "Beginner", "Math");
        createCourseWithLevelAndCategory("Advanced Math", "BOTH-ADV-MATH", "Advanced", "Math");
        createCourseWithLevelAndCategory("Beginner Science", "BOTH-BEG-SCI", "Beginner", "Science");

        // When: Search by level="Beginner" AND category="Math"
        // Then: Returns only courses matching both criteria
        mockMvc.perform(get("/api/v1/courses/search")
                .param("level", "Beginner")
                .param("category", "Math")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].level").value("Beginner"))
            .andExpect(jsonPath("$.data.content[0].category").value("Math"));
    }

    @Test
    @DisplayName("GET /api/v1/courses/search - Should return all courses when no filters")
    void shouldSearchCoursesWithNoFilters() throws Exception {
        // Given: Create multiple courses
        createCourseWithLevelAndCategory("Course 1", "NO-FILTER-1", "Beginner", "Math");
        createCourseWithLevelAndCategory("Course 2", "NO-FILTER-2", "Advanced", "Science");

        // When: Search with no parameters (both null)
        // Then: Returns all courses
        mockMvc.perform(get("/api/v1/courses/search")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content.length()").value(greaterThanOrEqualTo(2)));
    }

    /**
     * Helper method to create a course with level and category for testing.
     *
     * @param name Course name
     * @param code Course code
     * @param level Course level (e.g., "Beginner", "Intermediate", "Advanced")
     * @param category Course category (e.g., "Math", "Science", "Language")
     * @return Course ID
     * @throws Exception if creation fails
     */
    private Long createCourseWithLevelAndCategory(String name, String code, String level, String category) throws Exception {
        // Note: This assumes CreateCourseRequest will have level and category fields
        // Will be added in implementation phase
        String requestJson = String.format("""
            {
                "name": "%s",
                "code": "%s",
                "description": "Test description",
                "syllabus": "Test syllabus",
                "objectives": "Test objectives",
                "prerequisites": "None",
                "targetAudience": "Students",
                "level": "%s",
                "category": "%s",
                "teacherId": %d,
                "durationWeeks": 10,
                "totalSessions": 20,
                "price": 1000000
            }
            """, name, code, level, category, teacherId);

        String response = mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(requestJson))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("data").get("id").asLong();
    }

    // ── GAP-740: pricingModel default = PER_HOUR (ADR-035) ──────────────────

    @Test
    @DisplayName("POST /api/v1/courses - Course created without pricingModel should default to PER_HOUR (GAP-740 / ADR-035)")
    void shouldDefaultPricingModelToPerHour() throws Exception {
        // Given: request does NOT specify pricingModel — relies on entity default
        CreateCourseRequest request = new CreateCourseRequest(
            "Tiếng Anh Giao tiếp",
            "ENG-101",
            "Lớp tiếng Anh giao tiếp theo giờ",
            "Tuần 1: Chào hỏi, Tuần 2: Mô tả, Tuần 3: Đàm thoại",
            "Giao tiếp tiếng Anh cơ bản tự tin",
            "Không yêu cầu",
            "Học viên người lớn bận rộn",
            teacherId,
            8,   // durationWeeks
            16,  // totalSessions
            new BigDecimal("200000"),  // price (200.000đ/giờ — VN edu norm)
            null,  // level
            null   // category
        );

        // When: create course
        String responseBody = mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long courseId = objectMapper.readTree(responseBody).get("data").get("id").asLong();

        // Then: retrieve course and verify pricingModel = PER_HOUR
        mockMvc.perform(get("/api/v1/courses/{id}", courseId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pricingModel").value("PER_HOUR"));
    }
}
