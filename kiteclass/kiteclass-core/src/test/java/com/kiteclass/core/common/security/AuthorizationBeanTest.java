package com.kiteclass.core.common.security;

import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Wave 105 Bucket E0 Bug 5 (failure-mode matrix C3/D3) — verify
 * {@link AuthorizationBean} per-resource access matrix.
 *
 * <p>Per {@code pre-launch-owasp-rest-hardening-checklist.md} §2.1 A01
 * Broken Access Control + {@code pre-handoff-self-test-completeness.md} §2.7
 * multi-tenant per-resource scope check.
 *
 * @since Wave 105 Bucket E0
 */
@DisplayName("AuthorizationBean (@authz) per-resource access matrix (Bug 5)")
class AuthorizationBeanTest {

    private static final UUID USER_42 = UUID.fromString("00000000-0000-0000-0000-000000000042");
    private static final UUID USER_99 = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID USER_50 = UUID.fromString("00000000-0000-0000-0000-000000000050");
    private static final UUID USER_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private ParentStudentLinkRepository parentStudentLinkRepository;
    private EntityManager entityManager;
    private Query nativeQuery;
    private AuthorizationBean authz;

    @BeforeEach
    void setUp() {
        parentStudentLinkRepository = mock(ParentStudentLinkRepository.class);
        entityManager = mock(EntityManager.class);
        nativeQuery = mock(Query.class);
        when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(nativeQuery);
        when(nativeQuery.setParameter(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any())).thenReturn(nativeQuery);

        authz = new AuthorizationBean(parentStudentLinkRepository);
        ReflectionTestUtils.setField(authz, "entityManager", entityManager);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("hasAccessToClass: null classId → false")
    void hasAccessToClassNullDenies() {
        assertThat(authz.hasAccessToClass(null)).isFalse();
    }

    @Test
    @DisplayName("hasAccessToClass: no UserContext + no admin → false")
    void hasAccessToClassNoContextDenies() {
        assertThat(authz.hasAccessToClass(100L)).isFalse();
    }

    @Test
    @DisplayName("hasAccessToClass: teacher of class → true")
    void hasAccessToClassTeacherAllowed() {
        UserContext.setCurrentUser(USER_42);
        when(nativeQuery.getSingleResult()).thenReturn(1L);

        assertThat(authz.hasAccessToClass(100L)).isTrue();
    }

    @Test
    @DisplayName("hasAccessToClass: NOT teacher of class → false")
    void hasAccessToClassNonTeacherDenied() {
        UserContext.setCurrentUser(USER_99);
        when(nativeQuery.getSingleResult()).thenReturn(0L);

        assertThat(authz.hasAccessToClass(100L)).isFalse();
    }

    @Test
    @DisplayName("hasAccessToClass: PLATFORM_ADMIN bypass → true (no DB query)")
    void hasAccessToClassAdminBypass() {
        UserContext.setCurrentUser(USER_1);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "creds",
                        List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))));

        assertThat(authz.hasAccessToClass(100L)).isTrue();
        // Admin path bypasses DB query
        org.mockito.Mockito.verifyNoInteractions(entityManager);
    }

    @Test
    @DisplayName("hasAccessToChild: null childId → false")
    void hasAccessToChildNullDenies() {
        assertThat(authz.hasAccessToChild(null)).isFalse();
    }

    @Test
    @DisplayName("hasAccessToChild: non-admin actor → false (GAP-795 PARTIAL — actor UUID has no parents.id bridge)")
    void hasAccessToChildNonAdminDeniedUntilBridge() {
        // GAP-795: actor identity is a UUID; parent_student_links.parent_id is a numeric
        // parents.id with no actor-UUID bridge → ownership cannot be evaluated → fail closed.
        UserContext.setCurrentUser(USER_50);

        assertThat(authz.hasAccessToChild(200L)).isFalse();
        // Ownership query is NOT reachable for a non-admin actor under the PARTIAL state.
        org.mockito.Mockito.verifyNoInteractions(parentStudentLinkRepository);
    }

    @Test
    @DisplayName("hasAccessToChild: PLATFORM_ADMIN bypass → true")
    void hasAccessToChildAdminBypass() {
        UserContext.setCurrentUser(USER_1);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "creds",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        assertThat(authz.hasAccessToChild(200L)).isTrue();
        org.mockito.Mockito.verifyNoInteractions(parentStudentLinkRepository);
    }

    // ---------------------------------------------------------------------
    // GAP-1139 — ROLE_OWNER (school owner) is the highest tenant-scoped role
    // and must be treated as tenant-admin. Before the fix, isAdmin() only
    // matched ROLE_PLATFORM_ADMIN / ROLE_ADMIN, so an OWNER who was not also
    // a class teacher hit 403 on enrollment/attendance/grade/report endpoints.
    // These tests lock the OWNER-inclusive bypass as a CI regression guard;
    // the matching @WebMvcTest controller slice is ReportControllerAuthzTest.
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("isAdmin: ROLE_OWNER (school owner) → true (GAP-1139 tenant-admin)")
    void isAdminOwnerRecognized() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner", "creds",
                        List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));

        assertThat(authz.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("isAdmin: non-admin / non-owner (ROLE_STUDENT) → false")
    void isAdminNonOwnerDenied() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student", "creds",
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));

        assertThat(authz.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("isAdmin: no authentication → false (deny by default)")
    void isAdminUnauthenticatedDenied() {
        assertThat(authz.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("hasAccessToClass: OWNER bypass → true even when NOT the class teacher (no DB query)")
    void hasAccessToClassOwnerBypass() {
        // USER_99 is deliberately NOT the teacher of class 100 — an OWNER must
        // still be granted access within their own tenant (roster/attendance load).
        UserContext.setCurrentUser(USER_99);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner", "creds",
                        List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));

        assertThat(authz.hasAccessToClass(100L)).isTrue();
        // OWNER path short-circuits via isAdmin() — no teacher-ownership DB query.
        org.mockito.Mockito.verifyNoInteractions(entityManager);
    }

    @Test
    @DisplayName("hasAccessToEnrollment: OWNER bypass → true (no DB query) — GET /enrollments/{id} authority")
    void hasAccessToEnrollmentOwnerBypass() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner", "creds",
                        List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));

        assertThat(authz.hasAccessToEnrollment(500L)).isTrue();
        org.mockito.Mockito.verifyNoInteractions(entityManager);
    }

    @Test
    @DisplayName("hasAccessToStudent: OWNER bypass → true (no DB query) — GET /enrollments/student/{id} authority")
    void hasAccessToStudentOwnerBypass() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner", "creds",
                        List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));

        assertThat(authz.hasAccessToStudent(300L)).isTrue();
        org.mockito.Mockito.verifyNoInteractions(entityManager);
    }

    @Test
    @DisplayName("tenant isolation preserved: non-owner non-teacher → hasAccessToClass false (deny)")
    void hasAccessToClassNonOwnerNonTeacherStillDenied() {
        // GAP-1139 widened isAdmin() to OWNER but must NOT relax the per-resource
        // check for ordinary roles: a non-owner who is not the teacher is denied.
        UserContext.setCurrentUser(USER_99);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("teacher", "creds",
                        List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));
        when(nativeQuery.getSingleResult()).thenReturn(0L);

        assertThat(authz.hasAccessToClass(100L)).isFalse();
    }
}
