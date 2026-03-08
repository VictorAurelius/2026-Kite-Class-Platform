package com.kiteclass.core.integration;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end flow integration test for Student lifecycle.
 *
 * <p>Tests complete business workflow:
 * 1. Create student (ACTIVE status)
 * 2. Update student profile
 * 3. Update student status (suspend/activate)
 * 4. Soft delete student (archived)
 * 5. Verify student no longer appears in active list
 *
 * <p>This test verifies:
 * - Student CRUD operations work end-to-end
 * - Status transitions are properly enforced
 * - Soft delete isolation (deleted students don't appear in queries)
 * - Multi-tenant isolation
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

class StudentFlowIntegrationTest {

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
    @DisplayName("Student Flow: Create → Update → Suspend → Activate → Soft Delete")
    void testCompleteStudentLifecycle() throws Exception {
        // ========== Step 1: Create Student ==========
        CreateStudentRequest createRequest = new CreateStudentRequest(
                "Alice Johnson",
                "alice.flow@test.com",
                "0901234567",
                LocalDate.of(2008, 5, 15),
                Gender.FEMALE,
                "123 Test Street, Hanoi",
                "Parent: Jane Johnson, Phone: 0912345678"
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Alice Johnson"))
                .andExpect(jsonPath("$.data.email").value("alice.flow@test.com"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();

        // Extract student ID
        String response = createResult.getResponse().getContentAsString();
        Long studentId = objectMapper.readTree(response).get("data").get("id").asLong();

        // ========== Step 2: Update Student Profile ==========
        UpdateStudentRequest updateRequest = new UpdateStudentRequest(
                null,  // name
                null,  // email
                "0909999999",  // phone
                null,  // dateOfBirth
                null,  // gender
                "456 New Address, Hanoi",  // address
                null,  // status
                null   // note
        );

        mockMvc.perform(put("/api/v1/students/" + studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phone").value("0909999999"))
                .andExpect(jsonPath("$.data.address").value("456 New Address, Hanoi"));

        // ========== Step 3: Get Student to Verify Update ==========
        mockMvc.perform(get("/api/v1/students/" + studentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Alice Johnson"))
                .andExpect(jsonPath("$.data.phone").value("0909999999"))
                .andExpect(jsonPath("$.data.address").value("456 New Address, Hanoi"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // ========== Step 4: Update Student Status to INACTIVE ==========
        UpdateStudentRequest inactiveRequest = new UpdateStudentRequest(
                null,  // name
                null,  // email
                null,  // phone
                null,  // dateOfBirth
                null,  // gender
                null,  // address
                StudentStatus.INACTIVE,  // status
                null   // note
        );

        mockMvc.perform(put("/api/v1/students/" + studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(inactiveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        // ========== Step 5: Reactivate Student ==========
        UpdateStudentRequest reactivateRequest = new UpdateStudentRequest(
                null,  // name
                null,  // email
                null,  // phone
                null,  // dateOfBirth
                null,  // gender
                null,  // address
                StudentStatus.ACTIVE,  // status
                null   // note
        );

        mockMvc.perform(put("/api/v1/students/" + studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(reactivateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // ========== Step 6: Soft Delete Student ==========
        mockMvc.perform(delete("/api/v1/students/" + studentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNoContent());

        // ========== Step 7: Verify Student No Longer Accessible ==========
        mockMvc.perform(get("/api/v1/students/" + studentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNotFound());

        // ========== Step 8: Verify Student Not in List ==========
        mockMvc.perform(get("/api/v1/students")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.id == " + studentId + ")]").doesNotExist());
    }

    @Test
    @DisplayName("Student Flow: Multi-tenant isolation verification")
    void testMultiTenantIsolation() throws Exception {
        // ========== Tenant A: Create Student ==========
        UUID tenantA = UUID.randomUUID();
        CreateStudentRequest requestA = new CreateStudentRequest(
                "Student A",
                "student.a@tenant-a.com",
                "0901111111",
                LocalDate.of(2008, 1, 1),
                Gender.MALE,
                "Address A",
                null
        );

        MvcResult resultA = mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantA.toString())
                        .content(objectMapper.writeValueAsString(requestA)))
                .andExpect(status().isCreated())
                .andReturn();

        Long studentIdA = objectMapper.readTree(resultA.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // ========== Tenant B: Create Student ==========
        UUID tenantB = UUID.randomUUID();
        CreateStudentRequest requestB = new CreateStudentRequest(
                "Student B",
                "student.b@tenant-b.com",
                "0902222222",
                LocalDate.of(2008, 2, 2),
                Gender.FEMALE,
                "Address B",
                null
        );

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantB.toString())
                        .content(objectMapper.writeValueAsString(requestB)))
                .andExpect(status().isCreated());

        // ========== Tenant A: Verify Can Access Own Student ==========
        mockMvc.perform(get("/api/v1/students/" + studentIdA)
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Student A"));

        // ========== Tenant B: Verify Cannot Access Tenant A's Student ==========
        mockMvc.perform(get("/api/v1/students/" + studentIdA)
                        .header("X-Tenant-Id", tenantB.toString()))
                .andExpect(status().isNotFound());

        // ========== Tenant A: Verify List Shows Only Own Students ==========
        mockMvc.perform(get("/api/v1/students")
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'Student A')]").exists())
                .andExpect(jsonPath("$.data[?(@.name == 'Student B')]").doesNotExist());
    }
}
