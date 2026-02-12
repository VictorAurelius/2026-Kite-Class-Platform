package com.kiteclass.core.module.teacher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.TeacherStatus;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.teacher.dto.CreateTeacherRequest;
import com.kiteclass.core.module.teacher.dto.UpdateTeacherRequest;
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
 * Full integration tests for Teacher API endpoints.
 *
 * <p>Tests the complete request/response cycle:
 * API → Controller → Service → Repository → Database
 *
 * <p>Uses @SpringBootTest with real database (Testcontainers)
 * and MockMvc for HTTP requests.
 *
 * @author KiteClass Team
 * @since 2.3.1
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@ActiveProfiles("test")
@Transactional
class TeacherIntegrationTest {

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
    @DisplayName("POST /api/v1/teachers - Should create teacher successfully")
    void shouldCreateTeacher() throws Exception {
        // Given
        CreateTeacherRequest request = new CreateTeacherRequest(
            "Dr. John Smith",
            "john.smith@teacher.com",
            "0901234567",
            "Mathematics",
            "Experienced mathematics teacher with a passion for numbers",
            "PhD in Mathematics",
            15
        );

        // When/Then
        mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Dr. John Smith"))
            .andExpect(jsonPath("$.data.email").value("john.smith@teacher.com"))
            .andExpect(jsonPath("$.data.phoneNumber").value("0901234567"))
            .andExpect(jsonPath("$.data.specialization").value("Mathematics"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/teachers/{id} - Should return teacher by ID")
    void shouldGetTeacherById() throws Exception {
        // Given: Create a teacher first
        CreateTeacherRequest createRequest = new CreateTeacherRequest(
            "Jane Doe",
            "jane.doe@teacher.com",
            "0909876543",
            "Physics",
            "Physics expert with 10 years of teaching experience",
            "Master of Science in Physics",
            10
        );

        String createResponse = mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long teacherId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When/Then: Get the teacher
        mockMvc.perform(get("/api/v1/teachers/{id}", teacherId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(teacherId))
            .andExpect(jsonPath("$.data.name").value("Jane Doe"))
            .andExpect(jsonPath("$.data.email").value("jane.doe@teacher.com"))
            .andExpect(jsonPath("$.data.specialization").value("Physics"));
    }

    @Test
    @DisplayName("PUT /api/v1/teachers/{id} - Should update teacher successfully")
    void shouldUpdateTeacher() throws Exception {
        // Given: Create a teacher
        CreateTeacherRequest createRequest = new CreateTeacherRequest(
            "Bob Wilson",
            "bob.wilson@teacher.com",
            "0908765432",
            "Chemistry",
            "Chemistry teacher",
            "Bachelor of Science",
            5
        );

        String createResponse = mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long teacherId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When: Update the teacher
        UpdateTeacherRequest updateRequest = new UpdateTeacherRequest(
            "Dr. Bob Wilson",  // name
            null,  // email - keep existing
            null,  // phone - keep existing
            "Chemistry and Biochemistry",  // specialization
            "Expert in chemistry and biochemistry",  // bio
            "PhD in Chemistry",  // qualification
            8,  // experienceYears
            null   // status - keep existing
        );

        // Then
        mockMvc.perform(put("/api/v1/teachers/{id}", teacherId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Dr. Bob Wilson"))
            .andExpect(jsonPath("$.data.specialization").value("Chemistry and Biochemistry"))
            .andExpect(jsonPath("$.data.experienceYears").value(8))
            .andExpect(jsonPath("$.data.phoneNumber").value("0908765432")); // Unchanged
    }

    @Test
    @DisplayName("DELETE /api/v1/teachers/{id} - Should soft delete teacher")
    void shouldDeleteTeacher() throws Exception {
        // Given: Create a teacher
        CreateTeacherRequest createRequest = new CreateTeacherRequest(
            "Alice Brown",
            "alice.brown@teacher.com",
            "0907654321",
            "English",
            "English language teacher",
            "Master of Arts in English",
            7
        );

        String createResponse = mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long teacherId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When: Delete the teacher
        mockMvc.perform(delete("/api/v1/teachers/{id}", teacherId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isNoContent());

        // Then: Should not be found (soft deleted)
        mockMvc.perform(get("/api/v1/teachers/{id}", teacherId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/teachers - Should return paginated list")
    void shouldGetTeachersPaginated() throws Exception {
        // Given: Create multiple teachers
        for (int i = 1; i <= 3; i++) {
            CreateTeacherRequest request = new CreateTeacherRequest(
                "Teacher " + i,
                "teacher" + i + "@test.com",
                "090111111" + i,
                "Subject " + i,
                "Bio " + i,
                "Qualification " + i,
                i
            );

            mockMvc.perform(post("/api/v1/teachers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Tenant-Id", tenantId.toString())
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        }

        // When/Then: Get paginated list
        mockMvc.perform(get("/api/v1/teachers")
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
    @DisplayName("GET /api/v1/teachers - Should search by name")
    void shouldSearchTeachersByName() throws Exception {
        // Given: Create teachers with different names
        CreateTeacherRequest request1 = new CreateTeacherRequest(
            "John Search Test",
            "john.search@test.com",
            "0901111111",
            "Mathematics",
            "Math teacher",
            "Bachelor of Science",
            5
        );

        CreateTeacherRequest request2 = new CreateTeacherRequest(
            "Mary Different Name",
            "mary.different@test.com",
            "0902222222",
            "English",
            "English teacher",
            "Master of Arts",
            3
        );

        mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isCreated());

        // When/Then: Search by name
        mockMvc.perform(get("/api/v1/teachers")
                .param("search", "John")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[*].name", hasItem(containsString("John"))));
    }

    @Test
    @DisplayName("POST /api/v1/teachers - Should return 400 for invalid email")
    void shouldReturn400ForInvalidEmail() throws Exception {
        // Given: Invalid email
        CreateTeacherRequest request = new CreateTeacherRequest(
            "Test Teacher",
            "invalid-email",  // Invalid format
            "0901234567",
            "Mathematics",
            "Math teacher",
            "Bachelor of Science",
            5
        );

        // When/Then
        mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("POST /api/v1/teachers - Should return 400 for blank required fields")
    void shouldReturn400ForBlankRequiredFields() throws Exception {
        // Given: Blank name
        CreateTeacherRequest request = new CreateTeacherRequest(
            "",  // Blank name
            "test@test.com",
            "0901234567",
            "Mathematics",
            "Math teacher",
            "Bachelor of Science",
            5
        );

        // When/Then
        mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("POST /api/v1/teachers - Should return 400 for invalid phone number")
    void shouldReturn400ForInvalidPhone() throws Exception {
        // Given: Invalid phone (not 10 digits)
        CreateTeacherRequest request = new CreateTeacherRequest(
            "Test Teacher",
            "test@test.com",
            "123",  // Invalid phone
            "Mathematics",
            "Math teacher",
            "Bachelor of Science",
            5
        );

        // When/Then
        mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("GET /api/v1/teachers/{id} - Should return 404 for non-existent teacher")
    void shouldReturn404ForNonExistentTeacher() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/v1/teachers/{id}", 999999L)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("POST /api/v1/teachers - Should return 409 for duplicate email")
    void shouldReturn409ForDuplicateEmail() throws Exception {
        // Given: Create first teacher
        CreateTeacherRequest request1 = new CreateTeacherRequest(
            "First Teacher",
            "duplicate@test.com",
            "0901111111",
            "Mathematics",
            "Math teacher",
            "Bachelor of Science",
            5
        );

        mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isCreated());

        // When/Then: Try to create second teacher with same email
        CreateTeacherRequest request2 = new CreateTeacherRequest(
            "Second Teacher",
            "duplicate@test.com",  // Same email
            "0902222222",
            "Physics",
            "Physics teacher",
            "Master of Science",
            3
        );

        mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("Multi-tenant: Should not access teacher from different tenant")
    void shouldNotAccessTeacherFromDifferentTenant() throws Exception {
        // Given: Create teacher with tenant1
        UUID tenant1 = UUID.randomUUID();
        CreateTeacherRequest request = new CreateTeacherRequest(
            "Tenant1 Teacher",
            "tenant1@test.com",
            "0901111111",
            "Mathematics",
            "Math teacher",
            "Bachelor of Science",
            5
        );

        String createResponse = mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenant1.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long teacherId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When/Then: Try to access with tenant2
        UUID tenant2 = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/teachers/{id}", teacherId)
                .header("X-Tenant-Id", tenant2.toString()))
            .andExpect(status().isNotFound()); // Should not find - different tenant
    }

    @Test
    @DisplayName("PUT /api/v1/teachers/{id} - Should update teacher status")
    void shouldUpdateTeacherStatus() throws Exception {
        // Given: Create a teacher
        CreateTeacherRequest createRequest = new CreateTeacherRequest(
            "Status Test Teacher",
            "status.test@teacher.com",
            "0903333333",
            "Computer Science",
            "CS teacher",
            "Master of Science",
            6
        );

        String createResponse = mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long teacherId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When: Update status to ON_LEAVE
        UpdateTeacherRequest updateRequest = new UpdateTeacherRequest(
            null,  // name
            null,  // email
            null,  // phone
            null,  // specialization
            null,  // bio
            null,  // qualification
            null,  // experienceYears
            TeacherStatus.ON_LEAVE  // status
        );

        // Then
        mockMvc.perform(put("/api/v1/teachers/{id}", teacherId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ON_LEAVE"));
    }

    @Test
    @DisplayName("GET /api/v1/teachers - Should filter by status")
    void shouldFilterTeachersByStatus() throws Exception {
        // Given: Create teachers with different statuses
        CreateTeacherRequest activeTeacher = new CreateTeacherRequest(
            "Active Teacher",
            "active@test.com",
            "0904444444",
            "Biology",
            "Biology teacher",
            "Bachelor of Science",
            4
        );

        mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(activeTeacher)))
            .andExpect(status().isCreated());

        // When/Then: Filter by ACTIVE status
        mockMvc.perform(get("/api/v1/teachers")
                .param("status", "ACTIVE")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[*].status", everyItem(is("ACTIVE"))));
    }

    @Test
    @DisplayName("DELETE /api/v1/teachers/{id} - Verify soft delete does not physically remove")
    void shouldVerifySoftDelete() throws Exception {
        // Given: Create a teacher
        CreateTeacherRequest createRequest = new CreateTeacherRequest(
            "Soft Delete Test Teacher",
            "soft.delete@test.com",
            "0905555555",
            "History",
            "History teacher",
            "Master of Arts",
            9
        );

        String createResponse = mockMvc.perform(post("/api/v1/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long teacherId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When: Soft delete the teacher
        mockMvc.perform(delete("/api/v1/teachers/{id}", teacherId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isNoContent());

        // Then: Verify it's not returned in normal queries
        mockMvc.perform(get("/api/v1/teachers/{id}", teacherId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isNotFound());

        // And: Verify it's not in the list
        mockMvc.perform(get("/api/v1/teachers")
                .param("search", "Soft Delete Test")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[?(@.id == " + teacherId + ")]").doesNotExist());
    }
}
