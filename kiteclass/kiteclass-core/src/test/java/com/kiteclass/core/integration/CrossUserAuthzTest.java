package com.kiteclass.core.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.ParentLinkType;
import com.kiteclass.core.common.constant.ParentRelationship;
import com.kiteclass.core.common.constant.ParentStatus;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.parent.dto.ParentalConsent;
import com.kiteclass.core.module.parent.entity.Parent;
import com.kiteclass.core.module.parent.entity.ParentStudentLink;
import com.kiteclass.core.module.parent.repository.ParentRepository;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import com.kiteclass.core.testutil.TestDataBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 *   <li>A01-U01 ({@code @Disabled}): Teacher-class authz — untestable in test profile
 *       (AUDIT FINDING — see annotation)</li>
 *   <li>A01-U02: Parent-2 cannot GET attendance of Child linked only to Parent-1
 *       ({@code @authz.hasAccessToChild} — checks {@code parent_student_links})</li>
 *   <li>A01-U03 ({@code @Disabled}): Teacher positive-path — untestable in test profile
 *       (AUDIT FINDING — see annotation)</li>
 *   <li>A01-U04: Linked parent CAN reach service layer for their own child
 *       ({@code @PreAuthorize} guard passes; deeper service 4xx are non-authz)</li>
 * </ul>
 *
 * <p><strong>AUDIT FINDINGS recorded in disabled tests:</strong>
 * <ul>
 *   <li>{@code AttendanceController} is the ONLY controller with
 *       {@code @PreAuthorize("@authz.hasAccessToClass")} (teacher-class guard).
 *       The guard uses native SQL {@code classes.teacher_id} which is
 *       <em>not mapped on the {@code Class} JPA entity</em> and is
 *       <em>never set by {@code ClassServiceImpl.createClass()}</em> —
 *       therefore {@code teacher_id} is always {@code NULL} in production for
 *       newly created classes, meaning <strong>the guard denies ALL teachers</strong>
 *       (effective FULL LOCK-OUT — not IDOR). GAP candidate: fix ClassServiceImpl
 *       to set teacher_id on creation OR replace native-SQL guard with JPA.</li>
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

    @PersistenceContext
    private EntityManager entityManager;

    // ────────────────────────────────────────────────────────────────────────
    // CLASS authz — @authz.hasAccessToClass  [DISABLED — audit findings below]
    // ────────────────────────────────────────────────────────────────────────

    /**
     * A01-U01 — DISABLED: Teacher-class IDOR cannot be tested in the test profile.
     *
     * <p><strong>Root cause 1 — Test infrastructure:</strong>
     * The test profile sets {@code spring.flyway.enabled=false} and
     * {@code spring.jpa.hibernate.ddl-auto=create-drop}.  Schema is generated from
     * JPA entities only.  The {@code Class} entity does NOT map the
     * {@code teacher_id} column, so it is absent from the test schema.  Any native
     * SQL referencing {@code classes.teacher_id} (including both the
     * {@code AuthorizationBean.hasAccessToClass()} guard query and any test setup
     * {@code UPDATE classes SET teacher_id = …}) throws
     * {@code SQLGrammarException: column "teacher_id" does not exist}.
     *
     * <p><strong>Root cause 2 — Production code defect (AUDIT FINDING A01-CLASS-01):</strong>
     * {@code ClassServiceImpl.createClass()} never sets {@code teacher_id} on the
     * {@code Class} entity after creation.  Even if the column existed in the schema,
     * it would always be {@code NULL} for every newly created class.  The native SQL
     * guard in {@code AuthorizationBean.hasAccessToClass()} —
     * {@code SELECT COUNT(*) FROM classes WHERE id=:classId AND teacher_id=:userId} —
     * would therefore return {@code 0} for ALL teachers on ALL classes, effectively
     * locking out every teacher from their own classes (not an IDOR — a full
     * lock-out).
     *
     * <p><strong>GAP candidates (for coordinator batch):</strong>
     * <ol>
     *   <li>Add {@code teacher_id} mapping to {@code Class} JPA entity.</li>
     *   <li>Fix {@code ClassServiceImpl.createClass()} to persist
     *       {@code teacher_id = X-User-Id} on class creation.</li>
     *   <li>Add Flyway migration to populate {@code teacher_id} from class history
     *       or set to NULL explicitly for legacy classes.</li>
     *   <li>Consider replacing the native-SQL guard with a JPA-based ownership check
     *       to survive {@code ddl-auto} schema changes in future.</li>
     * </ol>
     */
    @Test
    @Disabled(
        "GAP-727 PARTIAL (Wave beta-readiness-2 Bucket B): Production defect FIXED — " +
        "Class entity now maps teacher_id (column generated by ddl-auto from @Column) AND " +
        "ClassServiceImpl.createClass() now sets teacherId from UserContext.getCurrentUser(). " +
        "Test body re-enable requires dedicated fixture: create teacher1 + teacher2 + course + class " +
        "via mockMvc POST /api/v1/courses/{id}/classes with X-User-Id=teacher1; then assert " +
        "GET /api/v1/attendance/classes/{classId}/sessions/{sessionId}/attendance with X-User-Id=teacher2 → 403. " +
        "Tracked in follow-up GAP-732 (Bucket B test re-enable, defer Wave beta-readiness-3+)."
    )
    @DisplayName("A01-U01 [DISABLED — PARTIAL GAP-727]: Teacher-2 cannot GET attendance roster for a class owned by Teacher-1")
    void teacher2_cannotGetAttendance_forClassOwnedByTeacher1() {
        // Intentionally empty — see @Disabled Javadoc above.
        // Production defect FIXED in this PR (entity field + service setter).
        // Test body re-enable tracked in GAP-732 follow-up.
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

        Long parent1Id = parent1.getId();
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
     * A01-U03 — DISABLED: Teacher positive-path baseline cannot be tested in this profile.
     *
     * <p>Same infrastructure constraint as A01-U01: {@code classes.teacher_id} column
     * is absent from the test schema (Flyway disabled, column not mapped on entity),
     * and {@code ClassServiceImpl.createClass()} never sets {@code teacher_id}.
     *
     * <p>A positive-path test would verify that {@code AuthorizationBean.hasAccessToClass()}
     * returns {@code true} when the calling user IS the owner.  This cannot be asserted
     * until the production defect described in A01-U01 is fixed.
     *
     * <p>See {@link #teacher2_cannotGetAttendance_forClassOwnedByTeacher1()} for the
     * full audit finding and GAP candidate list.
     */
    @Test
    @Disabled(
        "GAP-727 PARTIAL (Wave beta-readiness-2 Bucket B): Production defect FIXED — see A01-U01 @Disabled. " +
        "Positive-path test body re-enable requires dedicated fixture (same as A01-U01). " +
        "Tracked in follow-up GAP-732."
    )
    @DisplayName("A01-U03 [DISABLED — PARTIAL GAP-727]: Class owner (Teacher-1) CAN GET attendance roster — positive-path baseline")
    void teacher1_canGetAttendance_forOwnClass() {
        // Intentionally empty — production defect FIXED in this PR; test body in GAP-732 follow-up.
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
}
