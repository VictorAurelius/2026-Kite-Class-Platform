package com.kiteclass.core.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.testutil.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test to verify tenant data isolation via Hibernate tenant filter.
 *
 * <p>Proves that:
 * 1. Tenant A's data is invisible to Tenant B
 * 2. Tenant B's data is invisible to Tenant A
 * 3. Each tenant only sees their own data in list queries
 *
 * <p>This test validates the core multi-tenancy mechanism:
 * {@code @FilterDef(name = "tenantFilter")} on entities and
 * {@code TestTenantContextFilter} enabling the Hibernate filter per request.
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
class TenantIsolationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataBuilder testDataBuilder;

    @Test
    @DisplayName("Tenant A creates student -> Tenant B cannot see it -> Tenant A can see it")
    void shouldIsolateStudentDataBetweenTenants() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        // Step 1: Tenant A creates a student
        Long studentIdA = testDataBuilder.createTestStudent(
                mockMvc, objectMapper, tenantA,
                "Tenant A Student",
                "tenant.a." + System.currentTimeMillis() + "@kiteclass.test",
                "0905556677"
        );

        // Step 2: Tenant B lists students -> should get empty list (no students for Tenant B)
        mockMvc.perform(get("/api/v1/students")
                        .header("X-Tenant-Id", tenantB.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty());

        // Step 3: Tenant B tries to access Tenant A's student by ID -> should get 404
        mockMvc.perform(get("/api/v1/students/" + studentIdA)
                        .header("X-Tenant-Id", tenantB.toString()))
                .andExpect(status().isNotFound());

        // Step 4: Tenant A lists students -> should see their student
        mockMvc.perform(get("/api/v1/students")
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[?(@.id == " + studentIdA + ")]").exists())
                .andExpect(jsonPath("$.data.content[?(@.name == 'Tenant A Student')]").exists());

        // Step 5: Tenant A gets student by ID -> should succeed
        mockMvc.perform(get("/api/v1/students/" + studentIdA)
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Tenant A Student"));
    }

    @Test
    @DisplayName("Two tenants create students -> each sees only their own")
    void shouldIsolateBothTenantsData() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        // Tenant A creates student
        Long studentIdA = testDataBuilder.createTestStudent(
                mockMvc, objectMapper, tenantA,
                "Student From A",
                "from.a." + System.currentTimeMillis() + "@kiteclass.test",
                "0906667788"
        );

        // Tenant B creates student
        Long studentIdB = testDataBuilder.createTestStudent(
                mockMvc, objectMapper, tenantB,
                "Student From B",
                "from.b." + System.currentTimeMillis() + "@kiteclass.test",
                "0907778899"
        );

        // Tenant A lists students -> sees only "Student From A"
        mockMvc.perform(get("/api/v1/students")
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.name == 'Student From A')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.name == 'Student From B')]").doesNotExist());

        // Tenant B lists students -> sees only "Student From B"
        mockMvc.perform(get("/api/v1/students")
                        .header("X-Tenant-Id", tenantB.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.name == 'Student From B')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.name == 'Student From A')]").doesNotExist());

        // Cross-access: Tenant A cannot access Tenant B's student
        mockMvc.perform(get("/api/v1/students/" + studentIdB)
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isNotFound());

        // Cross-access: Tenant B cannot access Tenant A's student
        mockMvc.perform(get("/api/v1/students/" + studentIdA)
                        .header("X-Tenant-Id", tenantB.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tenant isolation works for courses as well")
    void shouldIsolateCourseDataBetweenTenants() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        // Tenant A creates a teacher and course
        Long teacherA = testDataBuilder.createTestTeacher(mockMvc, objectMapper, tenantA);
        Long courseIdA = testDataBuilder.createTestCourse(
                mockMvc, objectMapper, tenantA, "Tenant A Course", teacherA);

        // Tenant B lists courses -> should not see Tenant A's course
        mockMvc.perform(get("/api/v1/courses")
                        .header("X-Tenant-Id", tenantB.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[?(@.name == 'Tenant A Course')]").doesNotExist());

        // Tenant A lists courses -> should see their course
        mockMvc.perform(get("/api/v1/courses")
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[?(@.id == " + courseIdA + ")]").exists());

        // GAP-983 / GAP-362 coverage gap close: the original test only checked LIST isolation,
        // which let the by-id read leak through. Assert by-id isolation too.
        // Cross-access: Tenant B cannot read Tenant A's course by id.
        mockMvc.perform(get("/api/v1/courses/" + courseIdA)
                        .header("X-Tenant-Id", tenantB.toString()))
                .andExpect(status().isNotFound());

        // Own access: Tenant A can read its own course by id.
        mockMvc.perform(get("/api/v1/courses/" + courseIdA)
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(courseIdA));
    }
}
