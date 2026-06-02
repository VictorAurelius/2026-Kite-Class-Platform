package com.kiteclass.core.common.security;

import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Per-resource authorization bean for SpEL references in {@code @PreAuthorize}.
 *
 * <p>Closes OWASP A01 (Broken Access Control) per-resource authz gap surfaced
 * Wave 105 failure-mode matrix audit (C3/D3 — tenant isolation ≠ per-resource).
 * Bean name {@code "authz"} chosen short so SpEL refs read naturally:
 *
 * <pre>{@code
 *   @PreAuthorize("@authz.hasAccessToClass(#classId)")
 *   @PreAuthorize("@authz.hasAccessToChild(#childId)")
 * }</pre>
 *
 * <p><strong>GAP-795 — actor identity is a UUID.</strong> X-User-Id carries the JWT
 * {@code sub} claim, a {@link UUID} ({@link UserContext#getCurrentUser()} returns UUID).
 * There is no numeric user id in the token. Class ownership ({@code hasAccessToClass})
 * now compares the actor UUID against {@code classes.teacher_id} (also migrated to UUID
 * in V73, written from the same {@code UserContext}) — consistent UUID == UUID.
 *
 * <p><strong>Known limitation — parent ownership ({@code hasAccessToChild}) PARTIAL:</strong>
 * {@code parent_student_links.parent_id} references {@code parents.id} (numeric BIGINT
 * domain PK). There is NO bridge column linking the auth-user UUID to {@code parents.id},
 * so the actor UUID cannot be matched against the parent domain row. The check therefore
 * fails closed (deny for non-admin) until an actor-UUID → {@code parents.id} bridge is
 * designed. See {@link #hasAccessToChild(Long)} TODO. Platform admin bypasses both checks.
 *
 * <p><strong>Bypass for admin:</strong> if current authentication has role
 * {@code ROLE_PLATFORM_ADMIN} or {@code ROLE_ADMIN}, all access checks return
 * {@code true} (admin can read/write any tenant resource).
 *
 * <p><strong>No userId in context:</strong> if {@link UserContext#getCurrentUser()}
 * returns null (unauthenticated request), returns {@code false} (deny by default).
 *
 * @since Wave 105 — Bucket E0 (GAP from failure-mode matrix C3/D3)
 */
@Component("authz")
@RequiredArgsConstructor
@Slf4j
public class AuthorizationBean {

    private final ParentStudentLinkRepository parentStudentLinkRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Check if the current authenticated user (via {@link UserContext}) is the
     * teacher of the class with the given ID, OR a platform admin.
     *
     * <p>Tenant isolation already enforced by {@code TenantFilterInterceptor}
     * + Hibernate {@code @Filter}; this check ADDS per-resource ownership
     * verification.
     *
     * <p>Query: native JPQL since {@code teacher_id} on {@code classes} table
     * is not surfaced on the {@link com.kiteclass.core.module.clazz.entity.Class}
     * JPA entity (Phase 1 BETA scope — avoid entity restructure).
     *
     * @param classId target class ID
     * @return true if user owns the class OR is admin; false otherwise
     */
    public boolean hasAccessToClass(Long classId) {
        if (classId == null) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        UUID userId = UserContext.getCurrentUser();
        if (userId == null) {
            log.debug("authz.hasAccessToClass: deny — no user context (classId={})", classId);
            return false;
        }
        // classes.teacher_id stores the actor X-User-Id UUID (V73, GAP-795) — compare UUID == UUID.
        Object count = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM classes c " +
                        "WHERE c.id = :classId AND c.teacher_id = :userId AND c.deleted = false")
                .setParameter("classId", classId)
                .setParameter("userId", userId)
                .getSingleResult();
        boolean owns = ((Number) count).longValue() > 0;
        if (!owns) {
            log.warn("authz.hasAccessToClass: deny — user {} not teacher of class {}", userId, classId);
        }
        return owns;
    }

    /**
     * Check if the current authenticated user (via {@link UserContext}) is a
     * parent of the child (student) with the given ID, OR a platform admin.
     *
     * <p><strong>GAP-798 — reference-id bridge.</strong> {@code parent_student_links.parent_id}
     * references the numeric {@code parents.id} domain PK (= {@code users.reference_id} per the
     * Gateway V1 convention). The actor's numeric reference id arrives on the
     * {@code X-User-Reference-Id} header ({@link UserContext#getCurrentReferenceId()}); audit
     * (created_by) separately uses the UUID {@code X-User-Id} (GAP-795). Ownership is therefore
     * {@code parents.id == actor reference-id} AND a link to {@code childId}. No actor-UUID →
     * parents.id bridge column is needed: {@code users.reference_id} already IS the bridge.
     *
     * @param childId target student ID
     * @return true if admin OR the actor (by reference-id) is a linked parent of the child; false otherwise
     */
    public boolean hasAccessToChild(Long childId) {
        if (childId == null) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        Long parentRefId = UserContext.getCurrentReferenceId();
        if (parentRefId == null) {
            log.debug("authz.hasAccessToChild: deny — no reference-id context (childId={})", childId);
            return false;
        }
        // parent_student_links.parent_id == parents.id == actor X-User-Reference-Id (numeric).
        boolean owns = parentStudentLinkRepository
                .existsByParentIdAndStudentIdAndDeletedFalse(parentRefId, childId);
        if (!owns) {
            log.warn("authz.hasAccessToChild: deny — parent ref-id {} not linked to child {}",
                    parentRefId, childId);
        }
        return owns;
    }

    /**
     * Check if the current authenticated user owns the class to which the given
     * enrollment belongs, OR is a platform admin.
     *
     * <p>Resolves the enrollment row → its {@code classId} → delegates to
     * {@link #hasAccessToClass(Long)} (which checks {@code classes.teacher_id} ==
     * actor UUID). Wave local-doable-5 Bucket C (GAP-837) introduces this helper
     * to guard id-scoped enrollment endpoints (GET / PUT / withdraw) without
     * duplicating ownership logic.
     *
     * <p>Returns {@code false} when enrollment row not found (soft-deleted /
     * cross-tenant) — TenantFilterInterceptor already enforces tenant scope on
     * the lookup native query.
     *
     * @param enrollmentId target enrollment ID
     * @return true if user owns the class the enrollment belongs to OR is admin
     */
    public boolean hasAccessToEnrollment(Long enrollmentId) {
        if (enrollmentId == null) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        // Resolve enrollment → classId. enrollments table has tenant_id; native
        // query is read-only and uses parameterized binding (no injection).
        Object classIdObj;
        try {
            classIdObj = entityManager.createNativeQuery(
                    "SELECT class_id FROM enrollments WHERE id = :enrollmentId AND deleted = false")
                    .setParameter("enrollmentId", enrollmentId)
                    .getSingleResult();
        } catch (jakarta.persistence.NoResultException ex) {
            log.warn("authz.hasAccessToEnrollment: deny — enrollment {} not found (or soft-deleted)",
                    enrollmentId);
            return false;
        }
        if (classIdObj == null) {
            return false;
        }
        Long classId = ((Number) classIdObj).longValue();
        return hasAccessToClass(classId);
    }

    /**
     * Check if the current authenticated user owns at least one class the given
     * student is enrolled in, OR is a platform admin.
     *
     * <p>A student may be enrolled in multiple classes taught by different
     * teachers; access to the student's enrollment list (e.g.,
     * {@code GET /api/v1/enrollments/student/{studentId}}) is granted when the
     * actor teaches at least one of those classes. Wave local-doable-6 Bucket G
     * (GAP-729 / GAP-837 residual) introduces this helper to close the last
     * id-resolution endpoint without inferring student-level ownership through
     * the parent module (parent-child uses a separate
     * {@link #hasAccessToChild(Long)} check).
     *
     * <p>Query: native JOIN of {@code enrollments} → {@code classes} filtering
     * by {@code student_id}, soft-delete flags, and
     * {@code classes.teacher_id = actor UUID} — same teacher_id semantics as
     * {@link #hasAccessToClass(Long)} (V73, GAP-795 UUID actor). Returns
     * {@code false} when student has no enrollments OR no enrollment maps to a
     * class owned by the actor.
     *
     * @param studentId target student ID
     * @return true if admin OR actor teaches at least one class the student
     *         is enrolled in; false otherwise
     */
    public boolean hasAccessToStudent(Long studentId) {
        if (studentId == null) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        UUID userId = UserContext.getCurrentUser();
        if (userId == null) {
            log.debug("authz.hasAccessToStudent: deny — no user context (studentId={})", studentId);
            return false;
        }
        // Native query joins enrollments → classes; teacher_id on classes carries the
        // actor X-User-Id UUID (V73, GAP-795). Soft-delete flags filter both tables.
        // Parameterized binding (no injection); read-only.
        Object count = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM enrollments e " +
                        "JOIN classes c ON c.id = e.class_id " +
                        "WHERE e.student_id = :studentId " +
                        "AND e.deleted = false " +
                        "AND c.deleted = false " +
                        "AND c.teacher_id = :userId")
                .setParameter("studentId", studentId)
                .setParameter("userId", userId)
                .getSingleResult();
        boolean owns = ((Number) count).longValue() > 0;
        if (!owns) {
            log.warn("authz.hasAccessToStudent: deny — user {} does not teach any class containing student {}",
                    userId, studentId);
        }
        return owns;
    }

    /**
     * Check Spring Security context for admin role (bypass).
     *
     * @return true if current authentication holds {@code ROLE_PLATFORM_ADMIN}
     *         or {@code ROLE_ADMIN}
     */
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        List<String> roles = auth.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .toList();
        return roles.contains("ROLE_PLATFORM_ADMIN") || roles.contains("ROLE_ADMIN");
    }
}
