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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
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
        UserContext.setCurrentUser(42L);
        when(nativeQuery.getSingleResult()).thenReturn(1L);

        assertThat(authz.hasAccessToClass(100L)).isTrue();
    }

    @Test
    @DisplayName("hasAccessToClass: NOT teacher of class → false")
    void hasAccessToClassNonTeacherDenied() {
        UserContext.setCurrentUser(99L);
        when(nativeQuery.getSingleResult()).thenReturn(0L);

        assertThat(authz.hasAccessToClass(100L)).isFalse();
    }

    @Test
    @DisplayName("hasAccessToClass: PLATFORM_ADMIN bypass → true (no DB query)")
    void hasAccessToClassAdminBypass() {
        UserContext.setCurrentUser(1L);
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
    @DisplayName("hasAccessToChild: parent of child → true")
    void hasAccessToChildParentAllowed() {
        UserContext.setCurrentUser(50L);
        when(parentStudentLinkRepository.existsByParentIdAndStudentIdAndDeletedFalse(eq(50L), eq(200L)))
                .thenReturn(true);

        assertThat(authz.hasAccessToChild(200L)).isTrue();
    }

    @Test
    @DisplayName("hasAccessToChild: NOT parent of child → false (cross-child leak prevented)")
    void hasAccessToChildNonParentDenied() {
        UserContext.setCurrentUser(50L);
        // D3 cross-child leak scenario: user has child A, requests child B
        when(parentStudentLinkRepository.existsByParentIdAndStudentIdAndDeletedFalse(eq(50L), eq(999L)))
                .thenReturn(false);

        assertThat(authz.hasAccessToChild(999L)).isFalse();
    }

    @Test
    @DisplayName("hasAccessToChild: PLATFORM_ADMIN bypass → true")
    void hasAccessToChildAdminBypass() {
        UserContext.setCurrentUser(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "creds",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        assertThat(authz.hasAccessToChild(200L)).isTrue();
        org.mockito.Mockito.verifyNoInteractions(parentStudentLinkRepository);
    }
}
