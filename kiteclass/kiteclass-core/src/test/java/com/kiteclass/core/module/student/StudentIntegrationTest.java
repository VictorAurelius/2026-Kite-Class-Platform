package com.kiteclass.core.module.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.Gender;
import com.kiteclass.core.common.constant.StudentStatus;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.dto.UpdateStudentRequest;
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

import java.time.LocalDate;
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
 * Full integration tests for Student API endpoints.
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
@Transactional
class StudentIntegrationTest {

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
    @DisplayName("POST /api/v1/students - Should create student successfully")
    void shouldCreateStudent() throws Exception {
        // Given
        CreateStudentRequest request = new CreateStudentRequest(
            "John Doe",
            "john.doe@student.com",
            "0901234567",
            LocalDate.of(2005, 1, 15),
            Gender.MALE,
            "123 Main Street, District 1",
            "Parent: Jane Doe, Phone: 0912345678"
        );

        // When/Then
        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("John Doe"))
            .andExpect(jsonPath("$.data.email").value("john.doe@student.com"))
            .andExpect(jsonPath("$.data.phoneNumber").value("0901234567"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/students/{id} - Should return student by ID")
    void shouldGetStudentById() throws Exception {
        // Given: Create a student first
        CreateStudentRequest createRequest = new CreateStudentRequest(
            "Jane Smith",
            "jane.smith@student.com",
            "0909876543",
            LocalDate.of(2006, 3, 20),
            Gender.FEMALE,
            "456 Oak Avenue",
            null
        );

        String createResponse = mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long studentId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When/Then: Get the student
        mockMvc.perform(get("/api/v1/students/{id}", studentId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(studentId))
            .andExpect(jsonPath("$.data.name").value("Jane Smith"))
            .andExpect(jsonPath("$.data.email").value("jane.smith@student.com"));
    }

    @Test
    @DisplayName("PUT /api/v1/students/{id} - Should update student successfully")
    void shouldUpdateStudent() throws Exception {
        // Given: Create a student
        CreateStudentRequest createRequest = new CreateStudentRequest(
            "Bob Wilson",
            "bob.wilson@student.com",
            "0908765432",
            LocalDate.of(2005, 5, 10),
            Gender.MALE,
            "789 Pine Road",
            null
        );

        String createResponse = mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long studentId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When: Update the student
        UpdateStudentRequest updateRequest = new UpdateStudentRequest(
            "Bob Wilson Jr.",  // name
            null,  // email - keep existing
            null,  // phone - keep existing
            null,  // dateOfBirth - keep existing
            null,  // gender - keep existing
            null,  // address - keep existing
            StudentStatus.ACTIVE,  // status
            null   // note
        );

        // Then
        mockMvc.perform(put("/api/v1/students/{id}", studentId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Bob Wilson Jr."))
            .andExpect(jsonPath("$.data.phoneNumber").value("0908765432")); // Unchanged
    }

    @Test
    @DisplayName("DELETE /api/v1/students/{id} - Should soft delete student")
    void shouldDeleteStudent() throws Exception {
        // Given: Create a student
        CreateStudentRequest createRequest = new CreateStudentRequest(
            "Alice Brown",
            "alice.brown@student.com",
            "0907654321",
            LocalDate.of(2006, 7, 25),
            Gender.FEMALE,
            "321 Elm Street",
            null
        );

        String createResponse = mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long studentId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When: Delete the student
        mockMvc.perform(delete("/api/v1/students/{id}", studentId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isNoContent());

        // Then: Should not be found (soft deleted)
        mockMvc.perform(get("/api/v1/students/{id}", studentId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/students - Should return paginated list")
    void shouldGetStudentsPaginated() throws Exception {
        // Given: Create multiple students
        for (int i = 1; i <= 3; i++) {
            CreateStudentRequest request = new CreateStudentRequest(
                "Student " + i,
                "student" + i + "@test.com",
                "090111111" + i,
                LocalDate.of(2005, 1, i),
                Gender.MALE,
                "Address " + i,
                null
            );

            mockMvc.perform(post("/api/v1/students")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Tenant-Id", tenantId.toString())
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        }

        // When/Then: Get paginated list
        mockMvc.perform(get("/api/v1/students")
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
    @DisplayName("GET /api/v1/students - Should search by name")
    void shouldSearchStudentsByName() throws Exception {
        // Given: Create students with different names
        CreateStudentRequest request1 = new CreateStudentRequest(
            "John Search Test",
            "john.search@test.com",
            "0901111111",
            LocalDate.of(2005, 1, 1),
            Gender.MALE,
            "Address",
            null
        );

        CreateStudentRequest request2 = new CreateStudentRequest(
            "Mary Different Name",
            "mary.different@test.com",
            "0902222222",
            LocalDate.of(2005, 2, 2),
            Gender.FEMALE,
            "Address",
            null
        );

        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isCreated());

        // When/Then: Search by name
        mockMvc.perform(get("/api/v1/students")
                .param("name", "John")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[*].name", hasItem(containsString("John"))));
    }

    @Test
    @DisplayName("POST /api/v1/students - Should return 400 for invalid email")
    void shouldReturn400ForInvalidEmail() throws Exception {
        // Given: Invalid email
        CreateStudentRequest request = new CreateStudentRequest(
            "Test Student",
            "invalid-email",  // Invalid format
            "0901234567",
            LocalDate.of(2005, 1, 15),
            Gender.MALE,
            "Address",
            null
        );

        // When/Then
        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("POST /api/v1/students - Should return 400 for blank name")
    void shouldReturn400ForBlankName() throws Exception {
        // Given: Blank name
        CreateStudentRequest request = new CreateStudentRequest(
            "",  // Blank name
            "test@test.com",
            "0901234567",
            LocalDate.of(2005, 1, 15),
            Gender.MALE,
            "Address",
            null
        );

        // When/Then
        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("POST /api/v1/students - Should return 400 for invalid phone number")
    void shouldReturn400ForInvalidPhone() throws Exception {
        // Given: Invalid phone (not 10 digits)
        CreateStudentRequest request = new CreateStudentRequest(
            "Test Student",
            "test@test.com",
            "123",  // Invalid phone
            LocalDate.of(2005, 1, 15),
            Gender.MALE,
            "Address",
            null
        );

        // When/Then
        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("POST /api/v1/students - Should return 400 for future birth date")
    void shouldReturn400ForFutureBirthDate() throws Exception {
        // Given: Future birth date
        CreateStudentRequest request = new CreateStudentRequest(
            "Test Student",
            "test@test.com",
            "0901234567",
            LocalDate.now().plusDays(1),  // Future date
            Gender.MALE,
            "Address",
            null
        );

        // When/Then
        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("POST /api/v1/students - Should return 409 for duplicate email")
    void shouldReturn409ForDuplicateEmail() throws Exception {
        // Given: Create first student
        CreateStudentRequest request1 = new CreateStudentRequest(
            "First Student",
            "duplicate@test.com",
            "0901111111",
            LocalDate.of(2005, 1, 1),
            Gender.MALE,
            "Address",
            null
        );

        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isCreated());

        // When/Then: Try to create second student with same email
        CreateStudentRequest request2 = new CreateStudentRequest(
            "Second Student",
            "duplicate@test.com",  // Same email
            "0902222222",
            LocalDate.of(2005, 2, 2),
            Gender.FEMALE,
            "Address",
            null
        );

        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("GET /api/v1/students/{id} - Should return 404 for non-existent student")
    void shouldReturn404ForNonExistentStudent() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/v1/students/{id}", 999999L)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("Multi-tenant: Should not access student from different tenant")
    void shouldNotAccessStudentFromDifferentTenant() throws Exception {
        // Given: Create student with tenant1
        UUID tenant1 = UUID.randomUUID();
        CreateStudentRequest request = new CreateStudentRequest(
            "Tenant1 Student",
            "tenant1@test.com",
            "0901111111",
            LocalDate.of(2005, 1, 1),
            Gender.MALE,
            "Address",
            null
        );

        String createResponse = mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenant1.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long studentId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When/Then: Try to access with tenant2
        UUID tenant2 = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/students/{id}", studentId)
                .header("X-Tenant-Id", tenant2.toString()))
            .andExpect(status().isNotFound()); // Should not find - different tenant
    }

    @Test
    @DisplayName("PUT /api/v1/students/{id} - Should update status")
    void shouldUpdateStudentStatus() throws Exception {
        // Given: Create a student
        CreateStudentRequest createRequest = new CreateStudentRequest(
            "Status Test Student",
            "status.test@student.com",
            "0903333333",
            LocalDate.of(2005, 1, 1),
            Gender.MALE,
            "Address",
            null
        );

        String createResponse = mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long studentId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // When: Update status to INACTIVE
        UpdateStudentRequest updateRequest = new UpdateStudentRequest(
            null,  // name
            null,  // email
            null,  // phone
            null,  // dateOfBirth
            null,  // gender
            null,  // address
            StudentStatus.INACTIVE,  // status
            null   // note
        );

        // Then
        mockMvc.perform(put("/api/v1/students/{id}", studentId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/students - Should filter by status")
    void shouldFilterStudentsByStatus() throws Exception {
        // Given: Create students with different statuses
        CreateStudentRequest activeStudent = new CreateStudentRequest(
            "Active Student",
            "active@test.com",
            "0904444444",
            LocalDate.of(2005, 1, 1),
            Gender.MALE,
            "Address",
            null
        );

        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(activeStudent)))
            .andExpect(status().isCreated());

        // When/Then: Filter by ACTIVE status
        mockMvc.perform(get("/api/v1/students")
                .param("status", "ACTIVE")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[*].status", everyItem(is("ACTIVE"))));
    }
}
