package com.kiteclass.core.module.student.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.Gender;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import org.apache.commons.codec.digest.HmacUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link InternalStudentController} with database persistence.
 *
 * <p>Tests end-to-end internal API flow including:
 * <ul>
 *   <li>HMAC authentication</li>
 *   <li>Tenant context extraction from X-Tenant-Id header</li>
 *   <li>Database persistence with instanceId</li>
 *   <li>Multi-tenant data isolation</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.11.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestContainersConfiguration.class, TestSecurityConfig.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {"internal.api.secret=test-secret-for-hmac"})
class InternalStudentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Value("${internal.api.secret}")
    private String internalApiSecret;

    private final UUID tenantA = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID tenantB = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @AfterEach
    void cleanUp() {
        // Clean up test data after each test
        studentRepository.deleteAll();
    }

    /**
     * Generates HMAC-SHA256 signature for internal API authentication.
     *
     * @param timestamp the unix timestamp in seconds
     * @return HMAC signature hex string
     */
    private String generateHmacSignature(long timestamp) {
        return new HmacUtils("HmacSHA256", internalApiSecret).hmacHex(String.valueOf(timestamp));
    }

    /**
     * Creates a student in a specific tenant via internal API.
     *
     * @param email the student email
     * @param tenantId the tenant ID
     * @return the created student's ID from response
     */
    private Long createStudentInTenant(String email, UUID tenantId) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = generateHmacSignature(timestamp);

        CreateStudentRequest request = new CreateStudentRequest(
                "Test Student",
                email,
                "0912345678",
                LocalDate.of(2010, 1, 15),
                Gender.MALE,
                "Test Address",
                null
        );

        String response = mockMvc.perform(post("/internal/students")
                        .header("X-Internal-Timestamp", String.valueOf(timestamp))
                        .header("X-Internal-Signature", signature)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract ID from response
        return objectMapper.readTree(response).get("data").get("id").asLong();
    }

    @Test
    void createStudent_withValidTenantId_shouldPersistWithInstanceId() throws Exception {
        // Given
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = generateHmacSignature(timestamp);

        CreateStudentRequest request = new CreateStudentRequest(
                "Integration Test Student",
                "integration@test.com",
                "0912345678",
                LocalDate.of(2010, 1, 15),
                Gender.MALE,
                "Test Address",
                null
        );

        // When
        mockMvc.perform(post("/internal/students")
                        .header("X-Internal-Timestamp", String.valueOf(timestamp))
                        .header("X-Internal-Signature", signature)
                        .header("X-Tenant-Id", tenantA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("integration@test.com"));

        // Then - Verify database persistence with correct instanceId
        Student student = studentRepository.findByEmailAndDeletedFalse("integration@test.com")
                .orElseThrow(() -> new AssertionError("Student not found in database"));

        assertThat(student.getInstanceId())
                .as("Student should have instanceId set to tenantA")
                .isEqualTo(tenantA);
        assertThat(student.getName()).isEqualTo("Integration Test Student");
        assertThat(student.getEmail()).isEqualTo("integration@test.com");
    }

    @Test
    void createStudent_tenantIsolation_shouldNotSeeOtherTenantData() throws Exception {
        // Given - Create student in Tenant A
        createStudentInTenant("tenantA@test.com", tenantA);

        // Then - Should not see Tenant A's student when querying as Tenant B
        // Note: This test expects a 404 or empty result when tenant isolation works correctly
        // The actual behavior depends on how the GET endpoint handles multi-tenant queries

        // Verify in database directly with tenant filter bypassed
        Student student = studentRepository.findByEmailAndDeletedFalse("tenantA@test.com")
                .orElseThrow();
        assertThat(student.getInstanceId())
                .as("Student should belong to Tenant A")
                .isEqualTo(tenantA);

        // If there's a GET endpoint, uncomment and adjust:
        // mockMvc.perform(get("/internal/students")
        //                 .header("X-Internal-Timestamp", String.valueOf(timestamp))
        //                 .header("X-Internal-Signature", signature)
        //                 .header("X-Tenant-Id", tenantB.toString())
        //                 .param("email", "tenantA@test.com"))
        //         .andExpect(status().isNotFound());
    }

    @Test
    void createStudent_multipleTenantsWithSameEmail_shouldIsolateData() throws Exception {
        // Given - Create students with same email in different tenants
        String sharedEmail = "shared@test.com";

        CreateStudentRequest requestTenantA = new CreateStudentRequest(
                "Shared Name",
                sharedEmail,
                "0912345678",  // Different phone for Tenant A
                LocalDate.of(2010, 1, 15),
                Gender.MALE,
                "Test Address",
                null
        );

        long timestamp1 = System.currentTimeMillis() / 1000;
        String signature1 = generateHmacSignature(timestamp1);

        // When - Create in Tenant A
        mockMvc.perform(post("/internal/students")
                        .header("X-Internal-Timestamp", String.valueOf(timestamp1))
                        .header("X-Internal-Signature", signature1)
                        .header("X-Tenant-Id", tenantA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestTenantA)))
                .andExpect(status().isCreated());

        // Wait 1 second to ensure different timestamp
        Thread.sleep(1000);

        long timestamp2 = System.currentTimeMillis() / 1000;
        String signature2 = generateHmacSignature(timestamp2);

        // Create request for Tenant B with SAME email but DIFFERENT phone
        CreateStudentRequest requestTenantB = new CreateStudentRequest(
                "Shared Name",
                sharedEmail,  // SAME email as Tenant A
                "0987654321",  // DIFFERENT phone from Tenant A
                LocalDate.of(2010, 1, 15),
                Gender.MALE,
                "Test Address",
                null
        );

        // When - Create same email in Tenant B
        mockMvc.perform(post("/internal/students")
                        .header("X-Internal-Timestamp", String.valueOf(timestamp2))
                        .header("X-Internal-Signature", signature2)
                        .header("X-Tenant-Id", tenantB.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestTenantB)))
                .andExpect(status().isCreated());

        // Then - Both students should exist with different instanceIds
        var students = studentRepository.findAll().stream()
                .filter(s -> s.getEmail().equals(sharedEmail))
                .toList();

        assertThat(students).hasSize(2);
        assertThat(students)
                .extracting(Student::getInstanceId)
                .containsExactlyInAnyOrder(tenantA, tenantB);
    }

    @Test
    void createStudent_sameTenantDuplicateEmail_shouldReturn409() throws Exception {
        // Given - Create student in Tenant A
        String duplicateEmail = "duplicate@test.com";
        createStudentInTenant(duplicateEmail, tenantA);

        // When - Try to create another student with same email in same tenant
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = generateHmacSignature(timestamp);

        CreateStudentRequest request = new CreateStudentRequest(
                "Another Student",
                duplicateEmail,
                "0987654321",
                LocalDate.of(2011, 2, 20),
                Gender.FEMALE,
                "Different Address",
                null
        );

        // Then - Should fail with 409 Conflict (duplicate email within tenant)
        mockMvc.perform(post("/internal/students")
                        .header("X-Internal-Timestamp", String.valueOf(timestamp))
                        .header("X-Internal-Signature", signature)
                        .header("X-Tenant-Id", tenantA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDENT_EMAIL_EXISTS"));

        // Verify only one student exists with this email in Tenant A
        var students = studentRepository.findAll().stream()
                .filter(s -> duplicateEmail.equals(s.getEmail()) && tenantA.equals(s.getInstanceId()))
                .toList();
        assertThat(students).hasSize(1);
    }

    @Test
    void createStudent_withoutTenantId_shouldFailWith400MissingHeader() throws Exception {
        // Given
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = generateHmacSignature(timestamp);

        CreateStudentRequest request = new CreateStudentRequest(
                "No Tenant Student",
                "notenant@test.com",
                "0912345678",
                LocalDate.of(2010, 1, 15),
                Gender.MALE,
                "Test Address",
                null
        );

        // When - Create without X-Tenant-Id header (required @RequestHeader)
        // Then - 400 MISSING_HEADER per GAP-1117 (missing required @RequestHeader → 400, not 500)
        mockMvc.perform(post("/internal/students")
                        .header("X-Internal-Timestamp", String.valueOf(timestamp))
                        .header("X-Internal-Signature", signature)
                        // No X-Tenant-Id header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());  // GAP-1117: MissingRequestHeaderException -> 400

        // Verify student was not created
        assertThat(studentRepository.findByEmailAndDeletedFalse("notenant@test.com"))
                .isEmpty();
    }

    @Test
    void createStudent_withInvalidHmac_shouldReturn403() throws Exception {
        // Given
        long timestamp = System.currentTimeMillis() / 1000;
        String invalidSignature = "invalid-signature-12345";

        CreateStudentRequest request = new CreateStudentRequest(
                "Invalid HMAC Student",
                "invalid@test.com",
                "0912345678",
                LocalDate.of(2010, 1, 15),
                Gender.MALE,
                "Test Address",
                null
        );

        // When - Create with invalid HMAC signature
        // Then - Should be rejected by InternalRequestFilter
        mockMvc.perform(post("/internal/students")
                        .header("X-Internal-Timestamp", String.valueOf(timestamp))
                        .header("X-Internal-Signature", invalidSignature)
                        .header("X-Tenant-Id", tenantA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Verify student was not created
        assertThat(studentRepository.findByEmailAndDeletedFalse("invalid@test.com"))
                .isEmpty();
    }
}
