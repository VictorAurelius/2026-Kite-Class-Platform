package com.kiteclass.core.common.security;

import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test cho {@link AuthorizationBean#hasAccessToClass(Long)} chạy trên real Postgres
 * (Testcontainers) để verify multi-tenant boundary thực sự — closes GAP-727 (Wave beta-prep-1 Bucket D).
 *
 * <p><strong>Context:</strong> Trước Wave beta-readiness-2 Bucket B, {@link Class} entity không map
 * cột {@code teacher_id} + {@code ClassServiceImpl.createClass()} không set value → mọi teacher
 * locked-out OWN classes của họ (NOT IDOR — full lock-out). Bucket B đã fix production defect
 * (entity field + service setter); test này verify lại empirically tại fix-time per
 * {@code audit-to-gap-pipeline.md} §2.8 (state-check at fix pick-up).
 *
 * <p><strong>Why this IT exists alongside {@link AuthorizationBeanTest}:</strong>
 * Unit tests trong {@code AuthorizationBeanTest} dùng Mockito mock của {@code EntityManager} —
 * SQL không thực sự execute, chỉ verify code path. IT này chạy real Postgres
 * (Testcontainers) + real {@link ClassRepository} fixture → empirically verifies:
 * <ul>
 *   <li>{@code @Column(name = "teacher_id")} entity mapping persists đúng database column</li>
 *   <li>Native SQL guard query {@code WHERE c.id = :classId AND c.teacher_id = :userId}
 *       returns đúng kết quả trên real schema</li>
 *   <li>Multi-tenant boundary: teacher A không thể access class của teacher B
 *       (IDOR cross-tenant negative test)</li>
 * </ul>
 *
 * <p><strong>Test coverage (multi-tenant matrix):</strong>
 * <ul>
 *   <li>POSITIVE: teacher A truy cập class do teacher A sở hữu → {@code true}
 *       (A01-U03 positive-path baseline)</li>
 *   <li>NEGATIVE: teacher B truy cập class do teacher A sở hữu → {@code false}
 *       (A01-U01 IDOR cross-teacher denial)</li>
 *   <li>NEGATIVE: teacher truy cập class không tồn tại → {@code false}
 *       (edge case — null classId / missing class)</li>
 *   <li>NEGATIVE: class có {@code teacher_id == null} (legacy / admin-created) → {@code false}
 *       cho mọi teacher (edge case — null teacher_id column value)</li>
 *   <li>NEGATIVE: class đã soft-delete ({@code deleted = true}) → {@code false}
 *       ngay cả với owner đúng (edge case — soft-delete filter)</li>
 * </ul>
 *
 * <p><strong>Related rules applied:</strong>
 * <ul>
 *   <li>{@code postgres-specific-type-testcontainers.md} — real Postgres mandatory cho entity
 *       binding verification (H2 + Mockito unit tests insufficient cho boundary semantic)</li>
 *   <li>{@code pre-launch-owasp-rest-hardening-checklist.md} §2.1 A01 Broken Access Control —
 *       per-resource authorization explicit verification</li>
 *   <li>{@code pre-handoff-self-test-completeness.md} §2.7 multi-tenant tenant-switch —
 *       data isolation across tenant boundary verified</li>
 *   <li>{@code release-fix-retry-budget.md} v1.2.0 §3.5 — investigation phase mandate:
 *       state-check confirmed production defect already fixed; remaining work = re-enable
 *       previously-disabled @Disabled tests trong {@code CrossUserAuthzTest}</li>
 * </ul>
 *
 * @author KiteClass Team (Wave beta-prep-1 Bucket D — GAP-727 closure)
 * @since Wave beta-prep-1
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({
        TestContainersConfiguration.class,
        TestSecurityConfig.class,
        TestTenantContextFilter.class
})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@DisplayName("GAP-727: AuthorizationBean#hasAccessToClass multi-tenant boundary IT")
class AuthorizationBeanHasAccessToClassIT {

    @Autowired
    private AuthorizationBean authz;

    @Autowired
    private ClassRepository classRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private UUID tenantId;
    private UUID teacherAId;
    private UUID teacherBId;

    @BeforeEach
    void setUp() {
        // Sample VN-friendly data per dev-readable-doc-language.md §3.
        // GAP-795: teacher_id stores the actor X-User-Id UUID (JWT sub), not a numeric teachers.id.
        tenantId = UUID.randomUUID();
        teacherAId = UUID.fromString("00000000-0000-0000-0000-000000010001"); // Cô Trần Thị Hồng — teacher A
        teacherBId = UUID.fromString("00000000-0000-0000-0000-000000010002"); // Thầy Nguyễn Văn An — teacher B

        TenantContext.setCurrentTenant(tenantId);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    /**
     * Builds + persists a {@link Class} entity với explicit teacherId.
     * Bypass ClassServiceImpl để control teacher_id assignment chính xác cho test fixture.
     */
    private Class createClassOwnedBy(UUID teacherId, String name) {
        Class clazz = Class.builder()
                .courseId(99999L) // dummy course ID — không enforce FK trong test schema
                .teacherId(teacherId)
                .name(name)
                .maxStudents(30)
                .currentEnrolled(0)
                .locationType(Class.LocationType.IN_PERSON)
                .status(ClassStatus.SCHEDULED)
                .build();
        clazz.setInstanceId(tenantId);
        Class saved = classRepository.save(clazz);
        entityManager.flush(); // force SQL execute để catch binding errors per postgres-specific-type-testcontainers.md §4
        return saved;
    }

    // ────────────────────────────────────────────────────────────────────────
    // POSITIVE path — owner access
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("A01-U03 POSITIVE: Teacher A có thể access class do Teacher A sở hữu → true")
    void teacherA_canAccess_ownClass() {
        Class ownedClass = createClassOwnedBy(teacherAId, "Lớp Anh ngữ 5A1");
        UserContext.setCurrentUser(teacherAId);

        boolean result = authz.hasAccessToClass(ownedClass.getId());

        assertThat(result)
                .as("Teacher A là owner của class — guard PHẢI return true")
                .isTrue();
    }

    // ────────────────────────────────────────────────────────────────────────
    // NEGATIVE paths — multi-tenant IDOR boundary
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("A01-U01 NEGATIVE: Teacher B KHÔNG thể access class do Teacher A sở hữu → false (IDOR blocked)")
    void teacherB_cannotAccess_classOwnedByTeacherA() {
        Class teacherAClass = createClassOwnedBy(teacherAId, "Lớp Toán 9B");
        UserContext.setCurrentUser(teacherBId);

        boolean result = authz.hasAccessToClass(teacherAClass.getId());

        assertThat(result)
                .as("Teacher B không phải owner — guard PHẢI return false (IDOR cross-teacher denial)")
                .isFalse();
    }

    @Test
    @DisplayName("EDGE: Teacher truy cập classId không tồn tại → false")
    void teacher_cannotAccess_nonExistentClass() {
        UserContext.setCurrentUser(teacherAId);

        boolean result = authz.hasAccessToClass(999999999L);

        assertThat(result)
                .as("Class ID không tồn tại — guard PHẢI return false (deny by default)")
                .isFalse();
    }

    @Test
    @DisplayName("EDGE: Class có teacher_id = null (legacy / admin-created) → mọi teacher bị deny")
    void teacher_cannotAccess_classWithNullTeacherId() {
        // Simulate legacy class hoặc class do admin tạo (chưa assign teacher)
        Class legacyClass = Class.builder()
                .courseId(99999L)
                .teacherId(null) // explicit null — legacy / admin scope
                .name("Lớp IELTS 7.0 Buổi tối")
                .maxStudents(30)
                .currentEnrolled(0)
                .locationType(Class.LocationType.IN_PERSON)
                .status(ClassStatus.SCHEDULED)
                .build();
        legacyClass.setInstanceId(tenantId);
        Class saved = classRepository.save(legacyClass);
        entityManager.flush();

        UserContext.setCurrentUser(teacherAId);

        boolean result = authz.hasAccessToClass(saved.getId());

        assertThat(result)
                .as("Class teacher_id = null — không teacher nào claim ownership được, guard PHẢI return false")
                .isFalse();
    }

    @Test
    @DisplayName("EDGE: Soft-deleted class (deleted=true) → owner cũng bị deny")
    void teacher_cannotAccess_softDeletedClass() {
        Class ownedClass = createClassOwnedBy(teacherAId, "Lớp Lập trình Python K12");

        // Soft-delete via native SQL (BaseEntity uses @Filter để hide deleted; native query bypass)
        entityManager.createNativeQuery(
                "UPDATE classes SET deleted = true WHERE id = :id")
                .setParameter("id", ownedClass.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        UserContext.setCurrentUser(teacherAId);

        boolean result = authz.hasAccessToClass(ownedClass.getId());

        assertThat(result)
                .as("Class đã soft-delete — guard PHẢI return false (filter deleted=false trong query)")
                .isFalse();
    }

    @Test
    @DisplayName("EDGE: null classId → false (defensive deny)")
    void teacher_nullClassId_denied() {
        UserContext.setCurrentUser(teacherAId);

        boolean result = authz.hasAccessToClass(null);

        assertThat(result)
                .as("null classId — defensive deny PHẢI return false")
                .isFalse();
    }

    @Test
    @DisplayName("EDGE: Không có UserContext (anonymous) → false")
    void noUserContext_denied() {
        UserContext.clear(); // explicit clear — simulate anonymous request
        Class ownedClass = createClassOwnedBy(teacherAId, "Lớp Anh ngữ 5A1");

        boolean result = authz.hasAccessToClass(ownedClass.getId());

        assertThat(result)
                .as("Anonymous request (no UserContext) — guard PHẢI return false (deny by default)")
                .isFalse();
    }
}
