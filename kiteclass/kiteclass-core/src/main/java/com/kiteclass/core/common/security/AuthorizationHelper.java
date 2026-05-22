package com.kiteclass.core.common.security;

import com.kiteclass.core.module.teacher.repository.TeacherClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Per-resource authorization helper bean for SpEL expressions in
 * {@code @PreAuthorize} annotations.
 *
 * <p>Registered as Spring bean {@code "authz"} so SpEL references
 * resolve like {@code @authz.hasAccessToClass(#classId)}.
 *
 * <p>Wave 105 Bucket C (GAP-718 — per-class authz check for teacher
 * attendance/grade endpoints; failure-mode matrix C3 / P0 OWASP A01).
 *
 * <p>Phase 1 BETA scope: class-level check via {@link TeacherClassRepository}
 * existence query. Full RBAC (per-action permission matrix per
 * {@code TeacherClassRole} MAIN_TEACHER vs ASSISTANT) tracked Wave 107.
 *
 * <p>The teacher principal id is read from the {@code X-Teacher-Id}
 * request header through {@link TeacherIdHolder} (request-scoped). Test
 * code can stub the holder to drive SpEL without a full request context.
 *
 * @author KiteClass Team — Wave 105 Bucket C
 * @since 2026-05-22
 */
@Slf4j
@Component("authz")
@RequiredArgsConstructor
public class AuthorizationHelper {

    private final TeacherClassRepository teacherClassRepository;
    private final TeacherIdHolder teacherIdHolder;

    /**
     * Returns true iff the calling teacher is assigned to the given class.
     *
     * <p>Cross-class spoof guard — answers OWASP A01 Broken Access
     * Control concern flagged in Wave 105 failure-mode matrix row C3.
     *
     * <p>Decision: when teacherId is {@code null} (unauthenticated or
     * missing header), return {@code false} — deny by default; the
     * controller layer maps this to HTTP 403 via Spring Security's
     * {@code AccessDeniedException}.
     *
     * @param classId target class ID from URL path parameter
     * @return {@code true} when an active TeacherClass row exists for
     *         (teacherId, classId); {@code false} otherwise
     */
    public boolean hasAccessToClass(Long classId) {
        Long teacherId = teacherIdHolder.currentTeacherId();
        if (teacherId == null || classId == null) {
            log.debug("authz.hasAccessToClass denied — teacherId={} classId={}",
                    teacherId, classId);
            return false;
        }
        boolean allowed = teacherClassRepository
                .existsByTeacherIdAndClassId(teacherId, classId);
        if (!allowed) {
            log.info("authz.hasAccessToClass DENIED teacherId={} classId={}",
                    teacherId, classId);
        }
        return allowed;
    }
}
