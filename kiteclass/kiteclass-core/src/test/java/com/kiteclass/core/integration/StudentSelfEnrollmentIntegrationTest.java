package com.kiteclass.core.integration;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the student-self enrollment endpoint
 * {@code GET /api/v1/enrollments/me} (GAP-1285).
 *
 * <p>Verifies the security-critical contract: a STUDENT actor (resolved from the
 * {@code X-User-Reference-Id} header) sees ONLY their own enrollments — never
 * another student's, never other classes in the tenant — and that each row is
 * enriched with {@code className} + {@code courseId} + {@code courseName}.
 *
 * <p>Named {@code *IntegrationTest} so it runs under surefire (the CI
 * {@code ./mvnw test} path) like {@code EnrollmentIntegrationTest}, not failsafe.
 * Uses Testcontainers for a real Postgres (Flyway-migrated) + Hibernate
 * {@code tenantFilter} so the cross-student isolation assertion exercises the
 * production query path, not an H2 shortcut.
 *
 * @author KiteClass Team
 * @since GAP-1285 (Wave rbac-lms-gap-1285)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@DisplayName("Student self-enrollment (/api/v1/enrollments/me) IT")
class StudentSelfEnrollmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataBuilder testDataBuilder;

    private UUID tenantId;
    private Long teacherId;

    @BeforeEach
    void setUp() throws Exception {
        tenantId = UUID.randomUUID();
        teacherId = testDataBuilder.createTestTeacher(mockMvc, objectMapper, tenantId);
    }

    @Test
    @DisplayName("Student A sees ONLY A's enrollments (not B's, not other tenant classes) + enriched names")
    void myEnrollments_isSelfScopedAndEnriched() throws Exception {
        // --- Two students in the same tenant ---
        Long studentA = testDataBuilder.createTestStudent(
                mockMvc, objectMapper, tenantId,
                "Trần Thị Hồng",
                "hong.a." + System.currentTimeMillis() + "@kiteclass.test",
                "0901112233");
        Long studentB = testDataBuilder.createTestStudent(
                mockMvc, objectMapper, tenantId,
                "Nguyễn Văn An",
                "an.b." + System.currentTimeMillis() + "@kiteclass.test",
                "0902223344");

        // --- Two published courses, each with its own class ---
        Long courseAId = testDataBuilder.createPublishableCourse(
                mockMvc, objectMapper, tenantId, "Anh ngữ B1", teacherId);
        mockMvc.perform(post("/api/v1/courses/" + courseAId + "/publish")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk());
        Long classAId = createClass(courseAId, "Lớp Anh ngữ B1 - Tối");

        Long courseBId = testDataBuilder.createPublishableCourse(
                mockMvc, objectMapper, tenantId, "Toán 9", teacherId);
        mockMvc.perform(post("/api/v1/courses/" + courseBId + "/publish")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk());
        Long classBId = createClass(courseBId, "Lớp Toán 9 - Sáng");

        // --- A enrolls in class A; B enrolls in class B ---
        enroll(studentA, classAId);
        enroll(studentB, classBId);

        // --- A calls /me → ONLY A's enrollment, enriched, scoped ---
        mockMvc.perform(get("/api/v1/enrollments/me")
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", studentA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].studentId").value(studentA))
                .andExpect(jsonPath("$.data.content[0].classId").value(classAId))
                .andExpect(jsonPath("$.data.content[0].className").value("Lớp Anh ngữ B1 - Tối"))
                .andExpect(jsonPath("$.data.content[0].courseId").value(courseAId))
                .andExpect(jsonPath("$.data.content[0].courseName").value("Anh ngữ B1"))
                // Isolation: B's enrollment + B's class must NOT appear in A's list.
                .andExpect(jsonPath("$.data.content[?(@.studentId == " + studentB + ")]").doesNotExist())
                .andExpect(jsonPath("$.data.content[?(@.classId == " + classBId + ")]").doesNotExist());

        // --- B calls /me → ONLY B's enrollment (symmetric isolation) ---
        mockMvc.perform(get("/api/v1/enrollments/me")
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", studentB.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].studentId").value(studentB))
                .andExpect(jsonPath("$.data.content[0].classId").value(classBId))
                .andExpect(jsonPath("$.data.content[0].courseName").value("Toán 9"))
                .andExpect(jsonPath("$.data.content[?(@.studentId == " + studentA + ")]").doesNotExist());
    }

    @Test
    @DisplayName("Missing X-User-Reference-Id → 401 AUTH_REQUIRED")
    void myEnrollments_withoutReferenceId_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/me")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Student with no enrollments → empty page, 200")
    void myEnrollments_noEnrollments_returnsEmptyPage() throws Exception {
        Long student = testDataBuilder.createTestStudent(
                mockMvc, objectMapper, tenantId,
                "Phạm Thị Mai",
                "mai." + System.currentTimeMillis() + "@kiteclass.test",
                "0903334455");

        mockMvc.perform(get("/api/v1/enrollments/me")
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Reference-Id", student.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    // --- helpers ---

    private Long createClass(Long courseId, String className) throws Exception {
        CreateClassRequest classRequest = new CreateClassRequest(
                className,
                "Spring 2026",
                null,
                null,
                null,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(120),
                30);
        MvcResult result = mockMvc.perform(post("/api/v1/courses/" + courseId + "/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(classRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("id").asLong();
    }

    private void enroll(Long studentId, Long classId) throws Exception {
        CreateEnrollmentRequest enrollRequest = CreateEnrollmentRequest.builder()
                .studentId(studentId)
                .classId(classId)
                .tuitionAmount(BigDecimal.valueOf(1500000))
                .build();
        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(enrollRequest)))
                .andExpect(status().isCreated());
    }
}
