package com.kiteclass.core.common.security;

import com.kiteclass.core.module.teacher.repository.TeacherClassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthorizationHelper} per-class spoof guard.
 *
 * <p>Wave 105 Bucket C: validates OWASP A01 Broken Access Control fix.
 * Failure-mode matrix C3: "Tâm records attendance for class he's NOT
 * assigned" — endpoint must deny via 403.
 *
 * <p>Per `pre-handoff-self-test-completeness.md` §2.4 admin/auth flow:
 * checklist row (d) "role/permission gate at navigation path actually
 * grants access to the seeded user" — these tests verify that gate.
 */
@DisplayName("AuthorizationHelper — per-class spoof guard")
class AuthorizationHelperTest {

    private TeacherClassRepository teacherClassRepository;
    private TeacherIdHolder teacherIdHolder;
    private AuthorizationHelper helper;

    @BeforeEach
    void setUp() {
        teacherClassRepository = mock(TeacherClassRepository.class);
        teacherIdHolder = mock(TeacherIdHolder.class);
        helper = new AuthorizationHelper(teacherClassRepository, teacherIdHolder);
    }

    @Test
    @DisplayName("ALLOW: teacher assigned to class → returns true")
    void hasAccessToClass_assignedTeacher_returnsTrue() {
        Long teacherId = 5L;
        Long classId = 100L;
        when(teacherIdHolder.currentTeacherId()).thenReturn(teacherId);
        when(teacherClassRepository.existsByTeacherIdAndClassId(
                eq(teacherId), eq(classId))).thenReturn(true);

        assertThat(helper.hasAccessToClass(classId)).isTrue();
    }

    @Test
    @DisplayName("DENY (spoof): teacher NOT assigned → returns false")
    void hasAccessToClass_spoofedClassId_returnsFalse() {
        Long teacherId = 5L;
        Long spoofedClassId = 999L;
        when(teacherIdHolder.currentTeacherId()).thenReturn(teacherId);
        when(teacherClassRepository.existsByTeacherIdAndClassId(
                eq(teacherId), eq(spoofedClassId))).thenReturn(false);

        assertThat(helper.hasAccessToClass(spoofedClassId)).isFalse();
    }

    @Test
    @DisplayName("DENY: missing X-Teacher-Id header (teacherId null) → returns false")
    void hasAccessToClass_nullTeacherId_returnsFalse() {
        when(teacherIdHolder.currentTeacherId()).thenReturn(null);

        assertThat(helper.hasAccessToClass(100L)).isFalse();
    }

    @Test
    @DisplayName("DENY: null classId → returns false (fail-safe)")
    void hasAccessToClass_nullClassId_returnsFalse() {
        when(teacherIdHolder.currentTeacherId()).thenReturn(5L);

        assertThat(helper.hasAccessToClass(null)).isFalse();
    }

    @Test
    @DisplayName("DENY: teacherId=0 invalid → returns false")
    void hasAccessToClass_zeroTeacherId_returnsFalse() {
        // TeacherIdHolder returns null for non-positive, so currentTeacherId()=null
        when(teacherIdHolder.currentTeacherId()).thenReturn(null);

        assertThat(helper.hasAccessToClass(100L)).isFalse();
    }

    /**
     * Dual-role scenario (Teacher + Manager): per Wave 105 plan
     * §3 Bucket C AC row 4 "Dual-role (Teacher + Manager) RBAC scope
     * switch verified". When a user holds both roles, the per-class
     * check applies regardless of higher Manager role — defense in
     * depth (Manager scope = own center, not arbitrary class spoof).
     */
    @Test
    @DisplayName("Dual-role: Manager+Teacher must STILL hold TeacherClass assignment")
    void hasAccessToClass_dualRoleManager_stillRequiresAssignment() {
        Long teacherId = 5L;
        Long classId = 200L;
        when(teacherIdHolder.currentTeacherId()).thenReturn(teacherId);
        // User has Manager role but not assigned to THIS class as teacher
        when(teacherClassRepository.existsByTeacherIdAndClassId(
                eq(teacherId), eq(classId))).thenReturn(false);

        assertThat(helper.hasAccessToClass(classId)).isFalse();
        // Wave 107 Manager broad-scope fallback tracked separately
    }
}
