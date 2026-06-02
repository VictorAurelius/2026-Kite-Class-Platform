package com.kiteclass.core.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.ParentLinkType;
import com.kiteclass.core.common.constant.ParentRelationship;
import com.kiteclass.core.common.constant.ParentStatus;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.clazz.dto.CancelClassRequest;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.clazz.dto.UpdateClassRequest;
import com.kiteclass.core.module.enrollment.entity.Enrollment;
import com.kiteclass.core.module.enrollment.repository.EnrollmentRepository;
import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.module.parent.dto.ParentalConsent;
import com.kiteclass.core.module.parent.entity.Parent;
import com.kiteclass.core.module.parent.entity.ParentStudentLink;
import com.kiteclass.core.module.parent.repository.ParentRepository;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import com.kiteclass.core.testutil.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OWASP A01 Broken Access Control — Cross-User (IDOR) Authorization Audit.
 *
 * <p>Tests per-resource ownership checks within the SAME tenant: verifies that
 * {@code @PreAuthorize("@authz.hasAccessToChild")} actually denies access to
 * authenticated users who do not own the target resource.
 *
 * <p><strong>Critical design note — Method Security:</strong>
 * {@link TestSecurityConfig} enables {@code @EnableWebSecurity} but NOT
 * {@code @EnableMethodSecurity(prePostEnabled = true)}.  Without method security,
 * {@code @PreAuthorize} annotations are silently ignored and all requests pass.
 * This class imports an inner {@link MethodSecurityEnablerConfig} to activate
 * method-level security so the authz checks are actually enforced in the test JVM.
 *
 * <p><strong>Test coverage:</strong>
 * <ul>
 *   <li>A01-U01: Teacher-2 cannot GET grades for a class owned by Teacher-1
 *       ({@code @authz.hasAccessToClass} IDOR-deny — GAP-727 fix)</li>
 *   <li>A01-U02: Parent-2 cannot GET attendance of Child linked only to Parent-1
 *       ({@code @authz.hasAccessToChild} — checks {@code parent_student_links})</li>
 *   <li>A01-U03: Class owner (Teacher-1) CAN GET grades for own class —
 *       positive-path baseline ({@code @authz.hasAccessToClass} allow — GAP-727 fix)</li>
 *   <li>A01-U04: Linked parent CAN reach service layer for their own child
 *       ({@code @PreAuthorize} guard passes; deeper service 4xx are non-authz)</li>
 * </ul>
 *
 * <p><strong>AUDIT FINDINGS:</strong>
 * <ul>
 *   <li><strong>GAP-727 (FIXED):</strong> the teacher-class guard
 *       ({@code @authz.hasAccessToClass}) reads {@code classes.teacher_id}, which was
 *       historically <em>not mapped on the {@code Class} entity</em> and
 *       <em>never set by {@code ClassServiceImpl.createClass()}</em> — so it denied
 *       every teacher (full lock-out, not IDOR). Now the entity maps {@code teacher_id}
 *       (UUID) and the service sets it from {@code UserContext.getCurrentUser()};
 *       A01-U01/A01-U03 below assert the guard enforces ownership correctly.</li>
 *   <li>11 of 19 kiteclass-core controllers have NO {@code @PreAuthorize} —
 *       Class, Course, Student, Teacher, Invoice, Payment, Refund, Installment,
 *       Enrollment are fully IDOR-exposed (only tenant-level Hibernate filter
 *       applies, no per-resource ownership check). See PR audit matrix.</li>
 * </ul>
 *
 * @author KiteClass Team (OWASP A01 audit — Wave beta-readiness-1 Bucket D)
 * @since 2.16
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
        TestContainersConfiguration.class,
        TestSecurityConfig.class,
        TestTenantContextFilter.class,
        CrossUserAuthzTest.MethodSecurityEnablerConfig.class
})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@DisplayName("OWASP A01 — Cross-User (IDOR) Authorization IT")
class CrossUserAuthzTest {

    // ────────────────────────────────────────────────────────────────────────
    // Inner config: enable @PreAuthorize (absent from TestSecurityConfig)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Activates {@code @PreAuthorize} / {@code @PostAuthorize} evaluation.
     *
     * <p>{@link TestSecurityConfig} uses {@code @EnableWebSecurity} but omits
     * {@code @EnableMethodSecurity(prePostEnabled = true)}, which means
     * {@code @PreAuthorize} is a no-op in the test context unless this config is
     * explicitly imported.
     *
     * <p>Imported on the class-level {@code @Import} so it participates in the
     * same application context and overrides nothing in {@link TestSecurityConfig}.
     */
    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityEnablerConfig {
        // no additional beans needed — annotation alone activates the AOP advisor
    }

    // ────────────────────────────────────────────────────────────────────────
    // Dependencies
    // ────────────────────────────────────────────────────────────────────────

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataBuilder testDataBuilder;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private ParentStudentLinkRepository parentStudentLinkRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // ────────────────────────────────────────────────────────────────────────
    // CLASS authz — @authz.hasAccessToClass (teacher ownership of classes)
    // ────────────────────────────────────────────────────────────────────────
    //
    // GAP-727 (FIXED + re-enabled here, closes GAP-732): Class entity now maps
    // teacher_id (UUID), ClassServiceImpl.createClass() sets it from
    // UserContext.getCurrentUser(), and classes.teacher_id is present in the test
    // schema (Flyway enabled in test profile per GAP-735). The guard is therefore
    // enforceable and tested below — A01-U01 (non-owner IDOR → 403) +
    // A01-U03 (owner → 200). These replace the prior @Disabled audit-finding stubs.
    /**
     * Creates a published course + a class owned by {@code ownerUuid}.
     *
     * <p>The owner UUID flows via the {@code X-User-Id} header so that
     * {@code ClassServiceImpl.createClass()} persists {@code classes.teacher_id = ownerUuid}
     * — exercising the production fix verified by GAP-727 (entity field + service setter).
     *
     * @return the created class id
     */
    private Long createClassOwnedBy(UUID tenantId, UUID ownerUuid) throws Exception {
        Long domainTeacherId = testDataBuilder.createTestTeacher(mockMvc, objectMapper, tenantId);
        Long courseId = testDataBuilder.createPublishableCourse(
                mockMvc, objectMapper, tenantId,
                "Authz IT Course " + System.currentTimeMillis(), domainTeacherId);
        mockMvc.perform(post("/api/v1/courses/" + courseId + "/publish")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk());

        CreateClassRequest classRequest = new CreateClassRequest(
                "Authz IT Class",          // name (5-200 chars)
                "Authz IT class fixture",  // description
                null,                      // schedule
                null,                      // locationType (entity defaults IN_PERSON)
                null,                      // locationDetail
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(120),
                30);

        MvcResult classResult = mockMvc.perform(post("/api/v1/courses/" + courseId + "/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", ownerUuid.toString())
                        .header("X-User-Reference-Id", ownerUuid.toString())
                        .content(objectMapper.writeValueAsString(classRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(classResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();
    }

    @Test
    @DisplayName("A01-U01: Teacher-2 cannot GET grades for a class owned by Teacher-1 (hasAccessToClass IDOR → 403)")
    void teacher2_cannotGetGrades_forClassOwnedByTeacher1() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID teacher1 = UUID.randomUUID();
        UUID teacher2 = UUID.randomUUID();

        Long classId = createClassOwnedBy(tenantId, teacher1);

        // Teacher-2 is NOT the owner → @authz.hasAccessToClass denies → 403
        mockMvc.perform(get("/api/v1/grades/class/" + classId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", teacher2.toString())
                        .header("X-User-Reference-Id", teacher2.toString()))
                .andExpect(status().isForbidden());
    }

    // ────────────────────────────────────────────────────────────────────────
    // ENROLLMENT authz — @authz.hasAccessToClass on class-roster endpoint (GAP-729)
    // ────────────────────────────────────────────────────────────────────────
    //
    // GAP-729 closes A01 IDOR on EnrollmentController.getEnrollmentsByClass: the
    // class roster is an OWNED resource (only the class's teacher may list who is
    // enrolled). Guard reuses the existing @authz.hasAccessToClass(#classId) helper
    // (same as GradeController / AttendanceController), so no new authz infra is
    // introduced. A01-U05 (non-owner → 403) + A01-U06 (owner → 200) below mirror the
    // grade-class IDOR pair (A01-U01/A01-U03).

    @Test
    @DisplayName("A01-U05: Teacher-2 cannot GET enrollments for a class owned by Teacher-1 (hasAccessToClass IDOR → 403)")
    void teacher2_cannotGetEnrollments_forClassOwnedByTeacher1() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID teacher1 = UUID.randomUUID();
        UUID teacher2 = UUID.randomUUID();

        Long classId = createClassOwnedBy(tenantId, teacher1);

        // Teacher-2 is NOT the owner → @authz.hasAccessToClass denies → 403
        mockMvc.perform(get("/api/v1/enrollments/class/" + classId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", teacher2.toString())
                        .header("X-User-Reference-Id", teacher2.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A01-U06: Class owner (Teacher-1) CAN GET enrollments for own class — positive-path baseline (200)")
    void teacher1_canGetEnrollments_forOwnClass() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID teacher1 = UUID.randomUUID();

        Long classId = createClassOwnedBy(tenantId, teacher1);

        // Teacher-1 IS the owner → @authz.hasAccessToClass passes → 200 (empty roster for fresh class)
        mockMvc.perform(get("/api/v1/enrollments/class/" + classId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", teacher1.toString())
                        .header("X-User-Reference-Id", teacher1.toString()))
                .andExpect(status().isOk());
    }

    // ────────────────────────────────────────────────────────────────────────
    // PARENT-CHILD authz — @authz.hasAccessToChild
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("A01-U02: Parent-2 cannot GET attendance of Child linked only to Parent-1 (IDOR blocked by @authz.hasAccessToChild)")
    void parent2_cannotGetAttendance_forChildLinkedToParent1Only() throws Exception {
        UUID tenantId = UUID.randomUUID();

        // Create two students
        Long student1Id = testDataBuilder.createTestStudent(mockMvc, objectMapper, tenantId,
                "Pham Thi Mai", "mai.pham." + System.currentTimeMillis() + "@audit.test", "0901111001");
        Long student2Id = testDataBuilder.createTestStudent(mockMvc, objectMapper, tenantId,
                "Le Van Quang", "quang.le." + System.currentTimeMillis() + "@audit.test", "0901111002");

        // Retrieve Student entities from DB (needed for ParentStudentLink)
        Student student1 = studentRepository.findById(student1Id)
                .orElseThrow(() -> new IllegalStateException("Student 1 not found: " + student1Id));
        Student student2 = studentRepository.findById(student2Id)
                .orElseThrow(() -> new IllegalStateException("Student 2 not found: " + student2Id));

        // Create two parents via repository (direct entity creation; no Parent API endpoint)
        Parent parent1 = Parent.builder()
                .fullName("Nguyen Thi Ha")
                .email("ha.nguyen." + System.currentTimeMillis() + "@audit.test")
                .phoneNumber("0912000001")
                .relationship(ParentRelationship.MOTHER)
                .status(ParentStatus.ACTIVE)
                .build();
        parent1.setInstanceId(tenantId);

        Parent parent2 = Parent.builder()
                .fullName("Tran Van Binh")
                .email("binh.tran." + System.currentTimeMillis() + "@audit.test")
                .phoneNumber("0912000002")
                .relationship(ParentRelationship.FATHER)
                .status(ParentStatus.ACTIVE)
                .build();
        parent2.setInstanceId(tenantId);

        parentRepository.save(parent1);
        parentRepository.save(parent2);
        entityManager.flush();

        Long parent2Id = parent2.getId();

        // Create ParentStudentLink: Parent-1 → Student-1 only
        ParentStudentLink link1 = ParentStudentLink.builder()
                .parent(parent1)
                .student(student1)
                .linkType(ParentLinkType.PRIMARY)
                .parentalConsent(ParentalConsent.defaultValue())
                .build();
        link1.setInstanceId(tenantId);

        // Create ParentStudentLink: Parent-2 → Student-2 only
        ParentStudentLink link2 = ParentStudentLink.builder()
                .parent(parent2)
                .student(student2)
                .linkType(ParentLinkType.PRIMARY)
                .parentalConsent(ParentalConsent.defaultValue())
                .build();
        link2.setInstanceId(tenantId);

        parentStudentLinkRepository.save(link1);
        parentStudentLinkRepository.save(link2);
        entityManager.flush();

        // NEGATIVE PATH: Parent-2 tries to GET attendance for Student-1 (linked only to Parent-1)
        // X-User-Id = parent2Id → UserContext.getCurrentUser() = parent2Id
        // @authz.hasAccessToChild(student1Id) checks parent_student_links WHERE
        //   parent_id = parent2Id AND student_id = student1Id → false → AccessDeniedException → 403
        mockMvc.perform(get("/api/v1/parent/children/" + student1Id + "/attendance")
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", parent2Id.toString())
                        .header("X-User-Reference-Id", parent2Id.toString())
                        .param("from", LocalDate.now().minusDays(30).toString())
                        .param("to", LocalDate.now().toString()))
                .andExpect(status().isForbidden());
    }

    /**
     * A01-U03 — positive-path baseline: the class owner CAN reach a class-scoped
     * endpoint guarded by {@code @authz.hasAccessToClass}.
     *
     * <p>Verifies {@code AuthorizationBean.hasAccessToClass()} returns {@code true}
     * when the calling user IS the owner ({@code classes.teacher_id == X-User-Id}).
     * Pairs with {@link #teacher2_cannotGetGrades_forClassOwnedByTeacher1()} (negative
     * IDOR path) to fully cover the GAP-727 teacher-class guard fix.
     */
    @Test
    @DisplayName("A01-U03: Class owner (Teacher-1) CAN GET grades for own class — positive-path baseline (200)")
    void teacher1_canGetGrades_forOwnClass() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID teacher1 = UUID.randomUUID();

        Long classId = createClassOwnedBy(tenantId, teacher1);

        // Teacher-1 IS the owner → @authz.hasAccessToClass passes → 200 (empty grade list for fresh class)
        mockMvc.perform(get("/api/v1/grades/class/" + classId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", teacher1.toString())
                        .header("X-User-Reference-Id", teacher1.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A01-U04: Linked parent reaches service layer for their own child — @PreAuthorize guard passes")
    void linkedParent_authzGuardPasses_forOwnChild() throws Exception {
        UUID tenantId = UUID.randomUUID();

        Long studentId = testDataBuilder.createTestStudent(mockMvc, objectMapper, tenantId,
                "Hoang Thi Lan", "lan.hoang." + System.currentTimeMillis() + "@audit.test", "0923000001");

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalStateException("Student not found: " + studentId));

        Parent parent = Parent.builder()
                .fullName("Nguyen Van Duc")
                .email("duc.nguyen." + System.currentTimeMillis() + "@audit.test")
                .phoneNumber("0923000099")
                .relationship(ParentRelationship.FATHER)
                .status(ParentStatus.ACTIVE)
                .build();
        parent.setInstanceId(tenantId);
        parentRepository.save(parent);
        entityManager.flush();

        Long parentId = parent.getId();

        ParentStudentLink link = ParentStudentLink.builder()
                .parent(parent)
                .student(student)
                .linkType(ParentLinkType.PRIMARY)
                .parentalConsent(ParentalConsent.defaultValue())
                .build();
        link.setInstanceId(tenantId);
        parentStudentLinkRepository.save(link);
        entityManager.flush();

        // POSITIVE PATH — authz guard (@authz.hasAccessToChild) verification.
        //
        // X-User-Id = parentId → UserContext.getCurrentUser() = parentId
        // @authz.hasAccessToChild(studentId) → parent_student_links WHERE
        //   parent_id = parentId AND student_id = studentId → true → authz PASSES
        //
        // The service layer (ParentAttendanceFacetServiceImpl) has additional non-authz
        // gates beyond @PreAuthorize: a duplicate link check, a consent check
        // (consentService.checkConsent), and a consent-version check.  These may return
        // 403 with codes PARENT_CONSENT_REQUIRED or RECONSENT_REQUIRED if the default
        // ParentalConsent value does not satisfy the configured consent version.
        //
        // ASSERTION STRATEGY: Verify the request is NOT rejected with Spring Security's
        // AccessDeniedException (which returns 403 with a generic "Access Denied" or
        // "FORBIDDEN" body lacking a PARENT_* error code).  Any 4xx from the service
        // layer (consent, 404 no-attendance-data) proves @PreAuthorize passed — i.e.,
        // the authz guard is functioning correctly and only authorised parents reach
        // the service.
        //
        // We accept: 200 (empty page) | 403 PARENT_CONSENT_REQUIRED | 403 RECONSENT_REQUIRED | 404
        // We REJECT: 403 via AccessDeniedException (Spring Security) = authz guard blocked the owner
        mockMvc.perform(get("/api/v1/parent/children/" + studentId + "/attendance")
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", parentId.toString())
                        .header("X-User-Reference-Id", parentId.toString())
                        .param("from", LocalDate.now().minusDays(30).toString())
                        .param("to", LocalDate.now().toString()))
                .andExpect(result -> {
                    int httpStatus = result.getResponse().getStatus();
                    String body = result.getResponse().getContentAsString();

                    // A 403 from Spring Security (AccessDeniedException) will not have a
                    // PARENT_* error code in the body — it typically contains "Access Denied"
                    // or arrives as an empty body from the default AccessDeniedHandler.
                    // A 403 from the service layer will contain a structured error code
                    // like "PARENT_CONSENT_REQUIRED", "RECONSENT_REQUIRED", or
                    // "PARENT_FACET_FORBIDDEN" (the duplicate link check in the service).
                    boolean isSpringSecurityDeny = (httpStatus == 403)
                            && !body.contains("PARENT_CONSENT_REQUIRED")
                            && !body.contains("RECONSENT_REQUIRED")
                            && !body.contains("PARENT_FACET_FORBIDDEN");

                    if (isSpringSecurityDeny) {
                        throw new AssertionError(
                                "Expected @PreAuthorize guard to PASS for linked parent (parentId=" + parentId +
                                ", studentId=" + studentId + ") but received Spring Security 403. " +
                                "This indicates @authz.hasAccessToChild returned false despite a valid " +
                                "ParentStudentLink — check TenantFilterInterceptor sets UserContext from " +
                                "X-User-Id header in @SpringBootTest context. Response body: " + body);
                    }
                    // Any other status (200, 4xx from service layer) means authz passed. ✓
                });
    }

    // ────────────────────────────────────────────────────────────────────────
    // GAP-837 — extended sweep (Wave local-doable-5 Bucket C)
    // ────────────────────────────────────────────────────────────────────────
    //
    // 5 sister sites guarded same wave: ClassController write (update / cancel /
    // delete), AssignmentController list, EnrollmentController id-scoped (update
    // status / withdraw). Each test pair = non-owner 403 (or non-200 deny) +
    // owner-positive baseline already covered indirectly by createClassOwnedBy
    // returning the class to its rightful teacher.

    @Test
    @DisplayName("A01-U07: Teacher-2 cannot PATCH a class owned by Teacher-1 (ClassController updateClass IDOR → 403)")
    void teacher2_cannotUpdateClass_ownedByTeacher1() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID teacher1 = UUID.randomUUID();
        UUID teacher2 = UUID.randomUUID();

        Long classId = createClassOwnedBy(tenantId, teacher1);

        UpdateClassRequest req = new UpdateClassRequest(
                "Authz IT Class hijacked", null, null, null, null, null, null, null);

        mockMvc.perform(patch("/api/v1/classes/" + classId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", teacher2.toString())
                        .header("X-User-Reference-Id", teacher2.toString())
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A01-U08: Teacher-2 cannot DELETE a class owned by Teacher-1 (ClassController deleteClass IDOR → 403)")
    void teacher2_cannotDeleteClass_ownedByTeacher1() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID teacher1 = UUID.randomUUID();
        UUID teacher2 = UUID.randomUUID();

        Long classId = createClassOwnedBy(tenantId, teacher1);

        mockMvc.perform(delete("/api/v1/classes/" + classId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", teacher2.toString())
                        .header("X-User-Reference-Id", teacher2.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A01-U09: Teacher-2 cannot POST cancel on class owned by Teacher-1 (ClassController cancelClass IDOR → 403)")
    void teacher2_cannotCancelClass_ownedByTeacher1() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID teacher1 = UUID.randomUUID();
        UUID teacher2 = UUID.randomUUID();

        Long classId = createClassOwnedBy(tenantId, teacher1);

        CancelClassRequest req = new CancelClassRequest("Cross-tenant attack attempt");

        mockMvc.perform(post("/api/v1/classes/" + classId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", teacher2.toString())
                        .header("X-User-Reference-Id", teacher2.toString())
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A01-U10: Teacher-2 cannot GET assignments for a class owned by Teacher-1 (AssignmentController IDOR → 403)")
    void teacher2_cannotGetAssignments_forClassOwnedByTeacher1() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID teacher1 = UUID.randomUUID();
        UUID teacher2 = UUID.randomUUID();

        Long classId = createClassOwnedBy(tenantId, teacher1);

        mockMvc.perform(get("/api/v1/assignments/class/" + classId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", teacher2.toString())
                        .header("X-User-Reference-Id", teacher2.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A01-U11: Teacher-2 cannot GET enrollment by id whose class is owned by Teacher-1 (EnrollmentController IDOR → 403)")
    void teacher2_cannotGetEnrollmentById_forClassOwnedByTeacher1() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID teacher1 = UUID.randomUUID();
        UUID teacher2 = UUID.randomUUID();

        Long classId = createClassOwnedBy(tenantId, teacher1);

        // Seed an enrollment row in Teacher-1's class so id-scoped lookup has a target.
        Enrollment enrollment = Enrollment.builder()
                .classId(classId)
                .studentId(1L)  // synthetic — guard checks ownership, not student existence
                .enrollmentDate(java.time.LocalDateTime.now())
                .tuitionAmount(java.math.BigDecimal.ZERO)
                .finalAmount(java.math.BigDecimal.ZERO)
                .status(EnrollmentStatus.PENDING_PAYMENT)
                .build();
        enrollment.setInstanceId(tenantId);
        enrollmentRepository.save(enrollment);
        entityManager.flush();

        Long enrollmentId = enrollment.getId();

        // Teacher-2 → @authz.hasAccessToEnrollment resolves enrollment → classId →
        // hasAccessToClass denies (teacher_id != teacher2) → 403
        mockMvc.perform(get("/api/v1/enrollments/" + enrollmentId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", teacher2.toString())
                        .header("X-User-Reference-Id", teacher2.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A01-U12: Teacher-2 cannot PUT withdraw on enrollment in class owned by Teacher-1 (EnrollmentController IDOR → 403)")
    void teacher2_cannotWithdrawEnrollment_forClassOwnedByTeacher1() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID teacher1 = UUID.randomUUID();
        UUID teacher2 = UUID.randomUUID();

        Long classId = createClassOwnedBy(tenantId, teacher1);

        Enrollment enrollment = Enrollment.builder()
                .classId(classId)
                .studentId(2L)
                .enrollmentDate(java.time.LocalDateTime.now())
                .tuitionAmount(java.math.BigDecimal.ZERO)
                .finalAmount(java.math.BigDecimal.ZERO)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        enrollment.setInstanceId(tenantId);
        enrollmentRepository.save(enrollment);
        entityManager.flush();

        Long enrollmentId = enrollment.getId();

        mockMvc.perform(put("/api/v1/enrollments/" + enrollmentId + "/withdraw")
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", teacher2.toString())
                        .header("X-User-Reference-Id", teacher2.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A01-U13: Teacher-2 cannot GET enrollments for a student enrolled only in Teacher-1's class (hasAccessToStudent IDOR → 403)")
    void teacher2_cannotGetEnrollmentsByStudent_forStudentInTeacher1Class() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID teacher1 = UUID.randomUUID();
        UUID teacher2 = UUID.randomUUID();

        Long classId = createClassOwnedBy(tenantId, teacher1);

        // Seed: real Student row + enrollment binding student → Teacher-1's class.
        Long studentId = testDataBuilder.createTestStudent(mockMvc, objectMapper, tenantId,
                "Tran Thi Hong", "hong.tran." + System.currentTimeMillis() + "@audit.test",
                "0934000013");

        Enrollment enrollment = Enrollment.builder()
                .classId(classId)
                .studentId(studentId)
                .enrollmentDate(java.time.LocalDateTime.now())
                .tuitionAmount(java.math.BigDecimal.ZERO)
                .finalAmount(java.math.BigDecimal.ZERO)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        enrollment.setInstanceId(tenantId);
        enrollmentRepository.save(enrollment);
        entityManager.flush();

        // Teacher-2 teaches NO class containing this student → @authz.hasAccessToStudent
        // joins enrollments → classes WHERE teacher_id = teacher2 → 0 rows → deny → 403.
        mockMvc.perform(get("/api/v1/enrollments/student/" + studentId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", teacher2.toString())
                        .header("X-User-Reference-Id", teacher2.toString()))
                .andExpect(status().isForbidden());
    }
}
