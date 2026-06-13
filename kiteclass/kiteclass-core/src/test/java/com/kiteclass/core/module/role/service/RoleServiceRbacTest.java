package com.kiteclass.core.module.role.service;

import com.kiteclass.core.module.role.dto.UserRoleAssignmentResponse;
import com.kiteclass.core.module.role.entity.Role;
import com.kiteclass.core.module.role.entity.UserRole;
import com.kiteclass.core.module.role.repository.PermissionRepository;
import com.kiteclass.core.module.role.repository.RoleRepository;
import com.kiteclass.core.module.role.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the RBAC Bucket D owner-shell methods on {@link RoleService}:
 * listAssignments, getAssignmentForUser, revokeTemplateRoleByName.
 *
 * @since GAP-1119 RBAC Bucket D
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService RBAC Bucket D Tests")
class RoleServiceRbacTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private UserRoleRepository userRoleRepository;

    @InjectMocks
    private RoleService roleService;

    private Role role(long id, String name) {
        Role r = Role.builder().name(name).level(5).build();
        r.setId(id);
        return r;
    }

    private UserRole assignment(long userId, Role role) {
        return UserRole.builder().userId(userId).role(role).build();
    }

    @Test
    @DisplayName("listAssignments groups roles per user")
    void listAssignments_groupsByUser() {
        Role teacher = role(5L, "TEACHER");
        Role owner = role(1L, "OWNER");
        Role student = role(9L, "STUDENT");
        when(userRoleRepository.findByDeletedFalse()).thenReturn(List.of(
                assignment(100L, teacher), assignment(100L, owner), assignment(200L, student)));

        List<UserRoleAssignmentResponse> result = roleService.listAssignments();

        assertThat(result).hasSize(2);
        var u100 = result.stream().filter(a -> a.userId().equals(100L)).findFirst().orElseThrow();
        assertThat(u100.roles()).containsExactlyInAnyOrder("TEACHER", "OWNER");
        var u200 = result.stream().filter(a -> a.userId().equals(200L)).findFirst().orElseThrow();
        assertThat(u200.roles()).containsExactly("STUDENT");
    }

    @Test
    @DisplayName("getAssignmentForUser returns role names for a single user")
    void getAssignmentForUser_returnsRoles() {
        when(userRoleRepository.findByUserIdAndDeletedFalse(100L)).thenReturn(List.of(
                assignment(100L, role(5L, "TEACHER")), assignment(100L, role(1L, "OWNER"))));

        UserRoleAssignmentResponse result = roleService.getAssignmentForUser(100L);

        assertThat(result.userId()).isEqualTo(100L);
        assertThat(result.roles()).containsExactlyInAnyOrder("TEACHER", "OWNER");
    }

    @Test
    @DisplayName("revokeTemplateRoleByName soft-deletes when role + assignment exist")
    void revokeByName_softDeletes() {
        Role teacher = role(5L, "TEACHER");
        UserRole ur = assignment(100L, teacher);
        when(roleRepository.findByNameAndDeletedFalse("TEACHER")).thenReturn(Optional.of(teacher));
        when(userRoleRepository.findByUserIdAndRoleIdAndDeletedFalse(100L, 5L)).thenReturn(Optional.of(ur));

        roleService.revokeTemplateRoleByName(100L, "TEACHER");

        assertThat(ur.getDeleted()).isTrue();
        verify(userRoleRepository).save(ur);
    }

    @Test
    @DisplayName("revokeTemplateRoleByName is a no-op when role not seeded")
    void revokeByName_noopWhenRoleAbsent() {
        when(roleRepository.findByNameAndDeletedFalse("TEACHER")).thenReturn(Optional.empty());

        roleService.revokeTemplateRoleByName(100L, "TEACHER");

        verify(userRoleRepository, never()).save(any());
    }
}
