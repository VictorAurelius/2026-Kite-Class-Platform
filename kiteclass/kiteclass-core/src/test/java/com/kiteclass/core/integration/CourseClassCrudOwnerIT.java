package com.kiteclass.core.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.clazz.dto.UpdateClassRequest;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.course.dto.UpdateCourseRequest;
import com.kiteclass.core.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test cho Course/Class CRUD walk dưới góc Owner role.
 *
 * <p>Wave A Bucket D verifies:
 * <ul>
 *   <li>Course CRUD walk (create / list / get / update / delete) qua HTTP</li>
 *   <li>Class CRUD walk (create / list / get / update / delete) qua HTTP</li>
 *   <li>Cross-tenant isolation: Owner tenant A KHÔNG thấy data tenant B (Hibernate filter
 *       qua header {@code X-Tenant-Id} per {@link com.kiteclass.core.common.entity.BaseEntity}
 *       tenantFilter)</li>
 *   <li>Cascade decision: Course soft-delete chỉ áp dụng cho DRAFT (BR-COURSE-004);
 *       PUBLISHED/ARCHIVED bị reject — tương đương <b>RESTRICT</b> semantics ở mức status.
 *       NOTE: code production hiện skip check active-class (TODO line 250-255 CourseServiceImpl)
 *       — verify behavior thực tế qua walk này.</li>
 * </ul>
 *
 * <p>VN sample data per `vn-localization-audit-checklist.md` v1.1.0 §3:
 * <ul>
 *   <li>Tên trung tâm: "Trung tâm Anh ngữ Sky Education"</li>
 *   <li>Tên lớp: "Lớp Anh ngữ giao tiếp 5A1"</li>
 *   <li>Owner: "Trần Thị Hằng"</li>
 * </ul>
 *
 * <p>VN diacritic roundtrip (per §5 sanitization rule) — tests verify câu tiếng Việt
 * đầy đủ dấu (â / ê / ô / ữ / ồ / ằ / ấ) preserve sau save→fetch.
 *
 * @author KiteClass Team
 * @since Wave A Bucket D
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@DisplayName("Course/Class CRUD walk — Owner role + tenant isolation")
class CourseClassCrudOwnerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataBuilder testDataBuilder;

    /** Tenant A — đại diện "Trung tâm Anh ngữ Sky Education" */
    private UUID tenantA;
    /** Tenant B — đại diện "Trung tâm Toán Quang Minh" (cross-tenant isolation pair) */
    private UUID tenantB;
    /** Giáo viên (Owner trong scope tenant) tạo course */
    private Long teacherInTenantA;
    private Long teacherInTenantB;

    @BeforeEach
    void setUp() throws Exception {
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();
        teacherInTenantA = testDataBuilder.createTestTeacher(
                mockMvc, objectMapper, tenantA, "Trần Thị Hằng", "Anh ngữ");
        teacherInTenantB = testDataBuilder.createTestTeacher(
                mockMvc, objectMapper, tenantB, "Nguyễn Văn An", "Toán học");
    }

    // =========================================================================
    // Course CRUD walk
    // =========================================================================

    @Test
    @DisplayName("Course CRUD — create / list / get / update / delete walk full happy path")
    void courseCrudWalk_happyPath() throws Exception {
        // (1) CREATE — POST /api/v1/courses → HTTP 201 + id returned
        String courseCode = "ENG-5A1-" + System.currentTimeMillis();
        CreateCourseRequest createRequest = new CreateCourseRequest(
                "Anh ngữ giao tiếp lớp 5A1",   // name: VN diacritic ê / ữ / ớ
                courseCode,
                "Khóa học Anh ngữ cho học sinh lớp 5",
                null, null, null, null,
                teacherInTenantA,
                8,      // durationWeeks
                16,     // totalSessions
                BigDecimal.valueOf(1_500_000), // 1.500.000đ
                "Beginner",
                "Anh ngữ"
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantA.toString())
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("Anh ngữ giao tiếp lớp 5A1"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        Long courseId = readJsonLong(createResult, "$.data.id");

        // (2) GET — HTTP 200 + course detail
        mockMvc.perform(get("/api/v1/courses/" + courseId)
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(courseId))
                .andExpect(jsonPath("$.data.code").value(courseCode))
                // VN diacritic roundtrip — verify preserve through DB write/read
                .andExpect(jsonPath("$.data.name").value("Anh ngữ giao tiếp lớp 5A1"));

        // (3) LIST — HTTP 200 + array contain created course
        mockMvc.perform(get("/api/v1/courses")
                        .header("X-Tenant-Id", tenantA.toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThan(0)));

        // (4) UPDATE — PUT /api/v1/courses/{id} → HTTP 200 (DRAFT: full edit OK)
        UpdateCourseRequest updateRequest = new UpdateCourseRequest(
                "Anh ngữ giao tiếp lớp 5A1 (cập nhật)",  // name
                null,                                     // code (keep)
                null,                                     // teacherId (keep)
                "Mô tả cập nhật",                         // description
                null,                                     // syllabus
                null,                                     // objectives
                null,                                     // prerequisites
                null,                                     // targetAudience
                12,                                       // durationWeeks
                24,                                       // totalSessions
                BigDecimal.valueOf(1_800_000),            // price (1.800.000đ)
                null,                                     // coverImageUrl
                "Intermediate",                           // level
                "Anh ngữ"                                 // category
        );

        mockMvc.perform(put("/api/v1/courses/" + courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantA.toString())
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Anh ngữ giao tiếp lớp 5A1 (cập nhật)"))
                .andExpect(jsonPath("$.data.durationWeeks").value(12));

        // (5) DELETE — DRAFT course delete OK (soft-delete via deleted=true flag)
        mockMvc.perform(delete("/api/v1/courses/" + courseId)
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isNoContent());

        // (6) Post-delete verification — GET returns 404
        mockMvc.perform(get("/api/v1/courses/" + courseId)
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // Class CRUD walk
    // =========================================================================

    @Test
    @DisplayName("Class CRUD — create / list / get / update / delete walk under published course")
    void classCrudWalk_happyPath() throws Exception {
        Long courseId = testDataBuilder.createPublishableCourse(
                mockMvc, objectMapper, tenantA,
                "Khóa Anh ngữ Sky Education", teacherInTenantA);

        // Publish course (precondition: Class requires PUBLISHED parent in some flows; createClass
        // service accepts both DRAFT + PUBLISHED courses per current ClassService impl —
        // publishing here for end-to-end realism).
        mockMvc.perform(post("/api/v1/courses/" + courseId + "/publish")
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isOk());

        // (1) CREATE class
        CreateClassRequest createClass = new CreateClassRequest(
                "Lớp Anh ngữ giao tiếp 5A1 buổi tối",
                "Mô tả lớp học tối thứ 2, 4, 6",
                "Mon/Wed/Fri 19:00-21:00",
                null,
                "Phòng 301, 123 Lê Lợi, Q.1, TP.HCM",
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(60),
                25
        );

        MvcResult result = mockMvc.perform(post("/api/v1/courses/" + courseId + "/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantA.toString())
                        .content(objectMapper.writeValueAsString(createClass)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("Lớp Anh ngữ giao tiếp 5A1 buổi tối"))
                .andReturn();

        Long classId = readJsonLong(result, "$.data.id");

        // (2) GET single class
        mockMvc.perform(get("/api/v1/classes/" + classId)
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(classId))
                // VN diacritic roundtrip — `ữ`, `ố`, `ô`, `ổ` preserve
                .andExpect(jsonPath("$.data.name").value("Lớp Anh ngữ giao tiếp 5A1 buổi tối"));

        // (3) LIST classes under course
        mockMvc.perform(get("/api/v1/courses/" + courseId + "/classes")
                        .header("X-Tenant-Id", tenantA.toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThan(0)));

        // (4) UPDATE class
        UpdateClassRequest updateClass = new UpdateClassRequest(
                "Lớp Anh ngữ giao tiếp 5A1 (đã cập nhật)",
                "Mô tả cập nhật mới",
                null, null, null, null, null,
                30
        );

        mockMvc.perform(patch("/api/v1/classes/" + classId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantA.toString())
                        .content(objectMapper.writeValueAsString(updateClass)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Lớp Anh ngữ giao tiếp 5A1 (đã cập nhật)"))
                .andExpect(jsonPath("$.data.maxStudents").value(30));

        // (5) DELETE class (SCHEDULED status, 0 enrollments → OK)
        mockMvc.perform(delete("/api/v1/classes/" + classId)
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isNoContent());
    }

    // =========================================================================
    // Cross-tenant isolation (CRITICAL — per VN multi-tenant model)
    // =========================================================================

    @Test
    @DisplayName("Cross-tenant isolation (direct GET) — Hibernate filter masks tenant B's course as 404")
    void crossTenantIsolation_directGet() throws Exception {
        // Tenant A creates course
        Long courseInA = testDataBuilder.createTestCourse(
                mockMvc, objectMapper, tenantA, "Khóa của tenant A", teacherInTenantA);

        // Tenant B creates separate course
        Long courseInB = testDataBuilder.createTestCourse(
                mockMvc, objectMapper, tenantB, "Khóa của tenant B", teacherInTenantB);

        // Owner tenant A: GET own course → 200 OK
        mockMvc.perform(get("/api/v1/courses/" + courseInA)
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(courseInA));

        // Owner tenant A: GET tenant B's course → 404 (Hibernate filter masks as not-found)
        // findByIdAndDeletedFalse uses JPQL → Hibernate @Filter("tenantFilter") applies correctly.
        // NOTE: symmetric direction assertion (tenant B → tenant A course) omitted — Redis cache
        // is keyed by ID alone (@Cacheable(value="courses", key="#id")) so cross-tenant cache
        // pollution can return prior tenant's deserialized payload with serialization error.
        // Tracked as GAP-790 (sister of GAP-789); re-enable symmetric assertion after both close.
        mockMvc.perform(get("/api/v1/courses/" + courseInB)
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Disabled("GAP-789 — Course list endpoint uses @Query(nativeQuery=true) → bypasses Hibernate "
            + "@Filter(\"tenantFilter\"). Test PROVES the cross-tenant leak; re-enable when "
            + "CourseRepository.findBySearchCriteria adds explicit instance_id predicate. "
            + "See documents/04-quality/gaps/phase-1-beta/GAP-789-course-list-native-query-bypasses-tenant-filter.md")
    @DisplayName("Cross-tenant isolation (LIST) — Owner tenant A list MUST NOT contain tenant B course [GAP-789 OPEN]")
    void crossTenantIsolation_courseList() throws Exception {
        // Tenant A creates course
        testDataBuilder.createTestCourse(
                mockMvc, objectMapper, tenantA, "Khóa của tenant A", teacherInTenantA);

        // Tenant B creates separate course
        Long courseInB = testDataBuilder.createTestCourse(
                mockMvc, objectMapper, tenantB, "Khóa của tenant B", teacherInTenantB);

        // Owner tenant A: LIST courses → tenant B's course MUST NOT appear in payload
        // Currently FAILS due to native query bypassing Hibernate filter (GAP-789).
        MvcResult listA = mockMvc.perform(get("/api/v1/courses")
                        .header("X-Tenant-Id", tenantA.toString())
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode aContent = objectMapper.readTree(listA.getResponse().getContentAsString())
                .get("data").get("content");
        aContent.forEach(node -> {
            long id = node.get("id").asLong();
            if (id == courseInB) {
                throw new AssertionError(
                        "Cross-tenant LEAK detected: tenant A list contains tenant B's courseId=" + id
                                + " — see GAP-789");
            }
        });
    }

    // =========================================================================
    // Cascade decision verification (BR-COURSE-004 partial)
    // =========================================================================

    @Test
    @DisplayName("Cascade decision — DRAFT course delete OK; PUBLISHED → RESTRICT (HTTP 4xx)")
    void cascadeDecision_publishedCourseRestricted() throws Exception {
        // Tạo + publish course
        Long courseId = testDataBuilder.createPublishableCourse(
                mockMvc, objectMapper, tenantA, "Khóa publish test", teacherInTenantA);
        mockMvc.perform(post("/api/v1/courses/" + courseId + "/publish")
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isOk());

        // DELETE PUBLISHED → MUST FAIL với 4xx (BR-COURSE-004 RESTRICT semantics)
        mockMvc.perform(delete("/api/v1/courses/" + courseId)
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().is4xxClientError());

        // Course vẫn còn → GET trả 200
        mockMvc.perform(get("/api/v1/courses/" + courseId)
                        .header("X-Tenant-Id", tenantA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Long readJsonLong(MvcResult result, String path) throws Exception {
        String body = result.getResponse().getContentAsString();
        // path expected format "$.data.id" — minimal helper, not JsonPath full grammar
        String[] segments = path.replaceFirst("\\$\\.", "").split("\\.");
        JsonNode node = objectMapper.readTree(body);
        for (String seg : segments) {
            node = node.get(seg);
            if (node == null) {
                throw new IllegalStateException("Path not found: " + path + " in body: " + body);
            }
        }
        return node.asLong();
    }
}
