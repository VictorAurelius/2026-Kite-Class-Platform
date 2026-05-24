package com.kiteclass.core.module.enrollment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Wave beta-readiness-2 Bucket A — Idempotency IT for {@code POST /api/v1/enrollments} (GAP-730).
 *
 * <p>Verifies that submitting the same enrollment payload twice with the
 * same {@code Idempotency-Key} header produces:
 * <ul>
 *   <li>First request — 201 Created + enrollment row inserted</li>
 *   <li>Second request — 201 OK envelope from cache + NO second row</li>
 *   <li>Header {@code X-Idempotent-Replay: true} on the cached response</li>
 * </ul>
 *
 * <p>Sister test of Wave 105 Bucket D parent-payment idempotency (proven
 * precedent {@code PaymentIdempotencyService}); generalized here for the
 * shared {@code IdempotencyService} introduced V66.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
class EnrollmentIdempotencyIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataBuilder testDataBuilder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private Long studentId;
    private Long classId;

    @BeforeEach
    void setUp() throws Exception {
        tenantId = UUID.randomUUID();

        Long teacherId = testDataBuilder.createTestTeacher(mockMvc, objectMapper, tenantId);
        studentId = testDataBuilder.createTestStudent(
                mockMvc, objectMapper, tenantId,
                "Idem Student",
                "idem.student." + System.currentTimeMillis() + "@kiteclass.test",
                "0903334455"
        );

        Long courseId = testDataBuilder.createPublishableCourse(
                mockMvc, objectMapper, tenantId, "Idem Course", teacherId);
        mockMvc.perform(post("/api/v1/courses/" + courseId + "/publish")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk());

        CreateClassRequest classRequest = new CreateClassRequest(
                "Idem Class - Spring 2026",
                "Spring 2026",
                null,
                null,
                null,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(120),
                30
        );

        MvcResult classResult = mockMvc.perform(post("/api/v1/courses/" + courseId + "/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(classRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        classId = objectMapper.readTree(classResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();
    }

    @Test
    @DisplayName("Same Idempotency-Key replayed → 1 enrollment row, replay header set")
    void replayedKeyReturnsCachedResponseWithoutSecondInsert() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        CreateEnrollmentRequest request = CreateEnrollmentRequest.builder()
                .studentId(studentId)
                .classId(classId)
                .tuitionAmount(BigDecimal.valueOf(5000000))
                .build();
        String body = objectMapper.writeValueAsString(request);

        // First request — 201 Created.
        MvcResult firstResult = mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("Idempotency-Key", idempotencyKey)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Idempotent-Replay", "false"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value(studentId))
                .andExpect(jsonPath("$.data.classId").value(classId))
                .andReturn();

        JsonNode firstBody = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        long firstEnrollmentId = firstBody.get("data").get("id").asLong();

        // Replay — same key, same body — must be cache hit.
        MvcResult secondResult = mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("Idempotency-Key", idempotencyKey)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Idempotent-Replay", "true"))
                .andReturn();

        JsonNode secondBody = objectMapper.readTree(secondResult.getResponse().getContentAsString());
        long secondEnrollmentId = secondBody.get("data").get("id").asLong();

        // Same enrollment id returned both times.
        assertThat(secondEnrollmentId).isEqualTo(firstEnrollmentId);

        // Database has exactly 1 enrollment row for this (student, class).
        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM enrollments WHERE student_id = ? AND class_id = ?",
                Integer.class, studentId, classId);
        assertThat(rowCount).isEqualTo(1);

        // Database has exactly 1 idempotency row for this scope.
        Integer idemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM idempotency_keys " +
                        "WHERE tenant_id = ? AND idempotency_key = ? AND scope = 'ENROLLMENT'",
                Integer.class, tenantId, idempotencyKey);
        assertThat(idemCount).isEqualTo(1);
    }

    @Test
    @DisplayName("No Idempotency-Key header → legacy behavior, no idempotency row")
    void absentKeyFallsBackToLegacyHandlerWithNoIdempotencyRow() throws Exception {
        CreateEnrollmentRequest request = CreateEnrollmentRequest.builder()
                .studentId(studentId)
                .classId(classId)
                .tuitionAmount(BigDecimal.valueOf(5000000))
                .build();

        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        Integer idemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM idempotency_keys WHERE tenant_id = ?",
                Integer.class, tenantId);
        assertThat(idemCount).isEqualTo(0);
    }
}
