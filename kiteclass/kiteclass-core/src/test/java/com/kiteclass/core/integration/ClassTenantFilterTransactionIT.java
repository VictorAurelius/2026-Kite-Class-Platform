package com.kiteclass.core.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.clazz.entity.Class.LocationType;
import com.kiteclass.core.testutil.TestDataBuilder;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reproduction test for GAP-983: cross-tenant data leak on {@code @Transactional}
 * read paths because the Hibernate {@code tenantFilter} is enabled by the request
 * interceptor on the interceptor-time Hibernate session, NOT on the new
 * transaction-bound session that a {@code @Transactional} service method opens
 * (with {@code spring.jpa.open-in-view: false}).
 *
 * <p><b>CRITICAL — why this test is NOT class-level {@code @Transactional}:</b>
 * the sister {@link TenantIsolationIT} IS class-level {@code @Transactional}, which
 * makes every MockMvc request execute inside the single test transaction. That shares
 * one Hibernate session across the interceptor and the service method, so the filter
 * enabled by {@link TestTenantContextFilter} sticks — masking the production bug.
 * This test deliberately omits {@code @Transactional} so each request opens its own
 * transaction-bound session, faithfully reproducing the production session-per-request
 * model where the bug manifests.
 *
 * <p>The leak is exercised via {@code GET /api/v1/classes/{id}} which routes to
 * {@code ClassServiceImpl.getClass} — a {@code @Transactional(readOnly = true)} method
 * that calls {@code classRepository.findByIdAndDeletedFalse}. Before the fix, tenant B
 * could read tenant A's class (HTTP 200 LEAK); after the fix it must return 404.
 *
 * <p>Note: the test profile disables Flyway and uses {@code ddl-auto: create-drop}
 * (see {@code application-test.yml}), so Postgres Row-Level Security (V58+) is NOT
 * present. The ONLY tenant-isolation mechanism active here is the Hibernate
 * {@code @Filter} — which is exactly what this test isolates and the fix repairs.
 *
 * @author KiteClass Team
 * @since GAP-983 (Wave security-1)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
class ClassTenantFilterTransactionIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataBuilder testDataBuilder;

    @Test
    @DisplayName("GAP-983: tenant B reading tenant A's class by id returns 404 (not leaked 200)")
    void shouldNotLeakClassByIdAcrossTenants() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        Long classIdA = createClassForTenant(tenantA, "GAP-983 Tenant A Class");

        // Tenant B reads tenant A's class by id -> MUST be 404, not 200 leak.
        mockMvc.perform(get("/api/v1/classes/" + classIdA)
                        .header("X-Tenant-Id", tenantB.toString()))
                .andExpect(status().isNotFound());

        // Tenant A reads its own class -> 200 (positive own-access control).
        mockMvc.perform(get("/api/v1/classes/" + classIdA)
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(classIdA))
                .andExpect(jsonPath("$.data.name").value("GAP-983 Tenant A Class"));
    }

    @Test
    @DisplayName("GAP-983: tenant B reading tenant A's class sessions returns 404 (not leaked)")
    void shouldNotLeakClassSessionsAcrossTenants() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        Long classIdA = createClassForTenant(tenantA, "GAP-983 Sessions Class A");

        // listSessions(classId) first findClassOrThrow(classId) — cross-tenant must 404.
        mockMvc.perform(get("/api/v1/classes/" + classIdA + "/sessions")
                        .header("X-Tenant-Id", tenantB.toString()))
                .andExpect(status().isNotFound());

        // Tenant A own access succeeds.
        mockMvc.perform(get("/api/v1/classes/" + classIdA + "/sessions")
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GAP-983: tenant B reading tenant A's teacher by id returns 404 (not leaked)")
    void shouldNotLeakTeacherByIdAcrossTenants() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        Long teacherIdA = testDataBuilder.createTestTeacher(mockMvc, objectMapper, tenantA);

        mockMvc.perform(get("/api/v1/teachers/" + teacherIdA)
                        .header("X-Tenant-Id", tenantB.toString()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/teachers/" + teacherIdA)
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isOk());
    }

    /**
     * Creates a teacher + course + class for the given tenant and returns the class id.
     * Uses the public HTTP API exactly like a real caller so the full request-per-tenant
     * session model is exercised.
     */
    private Long createClassForTenant(UUID tenantId, String className) throws Exception {
        Long teacherId = testDataBuilder.createTestTeacher(mockMvc, objectMapper, tenantId);
        Long courseId = testDataBuilder.createTestCourse(
                mockMvc, objectMapper, tenantId, "GAP-983 Course " + System.nanoTime(), teacherId);

        CreateClassRequest request = new CreateClassRequest(
                className,                 // name (5-200 chars)
                "GAP-983 repro class",     // description
                "Mon/Wed 18:00-20:00",     // schedule
                LocationType.IN_PERSON,    // locationType
                "Room 101",                // locationDetail
                LocalDate.now().plusDays(7),   // startDate
                LocalDate.now().plusDays(60),  // endDate
                30                         // maxStudents (1-500)
        );

        MvcResult result = mockMvc.perform(post("/api/v1/courses/" + courseId + "/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data")
                .get("id")
                .asLong();
    }
}
