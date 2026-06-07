package com.kiteclass.core.integration;

import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the onboarding sample-data import (GAP-950 AC#3).
 *
 * <p>Verifies:
 * <ol>
 *   <li>POST /api/v1/onboarding/sample-data seeds 1 teacher + 1 class + 3 students + 3 enrollments</li>
 *   <li>The seeded entities are tenant-scoped (visible in that tenant's list endpoints)</li>
 *   <li>Re-calling is idempotent — no duplicates, {@code alreadyImported=true}</li>
 *   <li>A different tenant starts empty (no cross-tenant leak)</li>
 * </ol>
 *
 * <p>Uses Testcontainers for a real PostgreSQL DB and MockMvc for HTTP requests.
 *
 * @author KiteClass Team
 * @since 3.17.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
class OnboardingSampleDataIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Import sample data seeds demo set, is tenant-scoped, and is idempotent")
    void shouldImportSampleDataIdempotently() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String tenant = tenantId.toString();

        // Step 1: First import → fresh import with full counts.
        mockMvc.perform(post("/api/v1/onboarding/sample-data")
                        .header("X-Tenant-Id", tenant))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.alreadyImported").value(false))
                .andExpect(jsonPath("$.data.teachersCreated").value(1))
                .andExpect(jsonPath("$.data.coursesCreated").value(1))
                .andExpect(jsonPath("$.data.classesCreated").value(1))
                .andExpect(jsonPath("$.data.studentsCreated").value(3))
                .andExpect(jsonPath("$.data.enrollmentsCreated").value(3));

        // Step 2: Verify the entities exist for this tenant via the list endpoints.
        mockMvc.perform(get("/api/v1/teachers")
                        .header("X-Tenant-Id", tenant)
                        .param("page", "0").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/v1/students")
                        .header("X-Tenant-Id", tenant)
                        .param("page", "0").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3));
        // Note: the class is implicitly verified by the import response (classesCreated=1)
        // and by the 3 enrollments succeeding — enrollment requires a real enrollable class.
        // There is no flat /api/v1/classes list endpoint (classes nest under courses).

        // Step 3: Re-import → idempotent no-op, no duplicates.
        mockMvc.perform(post("/api/v1/onboarding/sample-data")
                        .header("X-Tenant-Id", tenant))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.alreadyImported").value(true))
                .andExpect(jsonPath("$.data.teachersCreated").value(0))
                .andExpect(jsonPath("$.data.studentsCreated").value(0));

        // Counts unchanged after re-import (no duplication).
        mockMvc.perform(get("/api/v1/students")
                        .header("X-Tenant-Id", tenant)
                        .param("page", "0").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    @Test
    @DisplayName("A different tenant is unaffected by another tenant's sample import")
    void shouldKeepSampleDataTenantScoped() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/onboarding/sample-data")
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.alreadyImported").value(false));

        // Tenant B saw no creation — its student list is empty.
        mockMvc.perform(get("/api/v1/students")
                        .header("X-Tenant-Id", tenantB.toString())
                        .param("page", "0").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }
}
