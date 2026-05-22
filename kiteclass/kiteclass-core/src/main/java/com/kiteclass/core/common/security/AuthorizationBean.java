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
 * <p><strong>Phase 1 BETA scope:</strong> userId from gateway X-User-Id header
 * maps directly to {@code teachers.id} when role is TEACHER, and to
 * {@code parents.id} when role is PARENT (Gateway sets {@code users.reference_id}
 * per V1 schema convention). Platform admin (role PLATFORM_ADMIN) bypasses.
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
        Long userId = UserContext.getCurrentUser();
        if (userId == null) {
            log.debug("authz.hasAccessToClass: deny — no user context (classId={})", classId);
            return false;
        }
        // teacher.id == users.reference_id when role=TEACHER (Gateway convention V1)
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
     * <p>Uses existing {@link ParentStudentLinkRepository#existsByParentIdAndStudentIdAndDeletedFalse(Long, Long)}
     * — no new queries introduced.
     *
     * @param childId target student ID
     * @return true if user is parent of child OR is admin; false otherwise
     */
    public boolean hasAccessToChild(Long childId) {
        if (childId == null) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        Long userId = UserContext.getCurrentUser();
        if (userId == null) {
            log.debug("authz.hasAccessToChild: deny — no user context (childId={})", childId);
            return false;
        }
        // parent.id == users.reference_id when role=PARENT (Gateway convention V1)
        boolean isParent = parentStudentLinkRepository
                .existsByParentIdAndStudentIdAndDeletedFalse(userId, childId);
        if (!isParent) {
            log.warn("authz.hasAccessToChild: deny — user {} not parent of child {}", userId, childId);
        }
        return isParent;
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
