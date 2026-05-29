package com.kiteclass.core.module.role;

import com.kiteclass.core.module.role.entity.Permission;
import com.kiteclass.core.module.role.entity.Role;
import com.kiteclass.core.module.role.entity.UserRole;
import com.kiteclass.core.module.role.repository.PermissionRepository;
import com.kiteclass.core.module.role.repository.RoleRepository;
import com.kiteclass.core.module.role.repository.UserRoleRepository;
import com.kiteclass.core.module.role.service.RoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private RoleService service;

    @Test
    void createRole_succeeds() {
        when(roleRepository.existsByNameAndDeletedFalse("ADMIN")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));

        Role result = service.createRole("ADMIN", "admin role", 2, null, null);

        assertThat(result.getName()).isEqualTo("ADMIN");
        assertThat(result.getLevel()).isEqualTo(2);
        assertThat(result.getParent()).isNull();
    }

    @Test
    void createRole_throws_on_duplicate_name() {
        when(roleRepository.existsByNameAndDeletedFalse("ADMIN")).thenReturn(true);

        assertThatThrownBy(() -> service.createRole("ADMIN", "x", 2, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createRole_throws_on_invalid_level() {
        when(roleRepository.existsByNameAndDeletedFalse("BAD")).thenReturn(false);

        assertThatThrownBy(() -> service.createRole("BAD", "x", 0, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("level must be between 1 and 10");

        assertThatThrownBy(() -> service.createRole("BAD", "x", 11, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRole_with_parent_links_correctly() {
        Role parent = Role.builder().name("OWNER").level(1).build();
        when(roleRepository.existsByNameAndDeletedFalse("ADMIN")).thenReturn(false);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));

        Role result = service.createRole("ADMIN", "x", 2, 1L, null);

        assertThat(result.getParent()).isEqualTo(parent);
    }

    @Test
    void createRole_with_permissions_attaches_them() {
        Permission p1 = Permission.builder().name("USER_MANAGE").build();
        when(roleRepository.existsByNameAndDeletedFalse("ADMIN")).thenReturn(false);
        when(permissionRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(p1));
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));

        Role result = service.createRole("ADMIN", "x", 2, null, Set.of(1L, 2L));

        assertThat(result.getPermissions()).containsExactly(p1);
    }

    @Test
    void assignRoleToUser_creates_assignment() {
        Role role = Role.builder().name("TEACHER").build();
        when(userRoleRepository.existsByUserIdAndRoleIdAndDeletedFalse(100L, 5L)).thenReturn(false);
        when(roleRepository.findById(5L)).thenReturn(Optional.of(role));
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(i -> i.getArgument(0));

        UserRole result = service.assignRoleToUser(100L, 5L);

        assertThat(result.getUserId()).isEqualTo(100L);
        assertThat(result.getRole()).isEqualTo(role);
    }

    @Test
    void assignRoleToUser_idempotent_when_already_assigned() {
        UserRole existing = UserRole.builder().userId(100L).build();
        when(userRoleRepository.existsByUserIdAndRoleIdAndDeletedFalse(100L, 5L)).thenReturn(true);
        when(userRoleRepository.findByUserIdAndRoleIdAndDeletedFalse(100L, 5L))
                .thenReturn(Optional.of(existing));

        UserRole result = service.assignRoleToUser(100L, 5L);

        assertThat(result).isEqualTo(existing);
        verify(userRoleRepository, never()).save(any());
    }

    /**
     * Wave beta-prep-1 Bucket E — Path 5 race hardening verification.
     *
     * <p>Simulates the race-loser branch: concurrent grant where this thread's
     * {@code existsBy} pre-check returned false, but another transaction inserted
     * the row between our check and our save. DB partial UNIQUE index fires →
     * {@link org.springframework.dao.DataIntegrityViolationException}. We catch
     * and return the winner's row to honor the idempotency contract.</p>
     */
    @Test
    void assignRoleToUser_recovers_from_race_via_DataIntegrityViolation() {
        Role role = Role.builder().name("TEACHER").build();
        UserRole winner = UserRole.builder().userId(100L).role(role).build();

        // First exists-check: false (we think we're the winner)
        when(userRoleRepository.existsByUserIdAndRoleIdAndDeletedFalse(100L, 5L)).thenReturn(false);
        when(roleRepository.findById(5L)).thenReturn(Optional.of(role));
        // Save throws (race-loser hits DB UNIQUE)
        when(userRoleRepository.save(any(UserRole.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                        "duplicate key value violates unique constraint idx_ur_user_role"));
        // Recovery findBy returns the winner's row
        when(userRoleRepository.findByUserIdAndRoleIdAndDeletedFalse(100L, 5L))
                .thenReturn(Optional.of(winner));

        UserRole result = service.assignRoleToUser(100L, 5L);

        assertThat(result)
                .as("idempotent recovery — returns winner's row instead of bubbling 500")
                .isEqualTo(winner);
    }

    @Test
    void revokeRoleFromUser_soft_deletes() {
        UserRole existing = UserRole.builder().userId(100L).build();
        when(userRoleRepository.findByUserIdAndRoleIdAndDeletedFalse(100L, 5L))
                .thenReturn(Optional.of(existing));

        service.revokeRoleFromUser(100L, 5L);

        assertThat(existing.getDeleted()).isTrue();
        verify(userRoleRepository).save(existing);
    }

    @Test
    void getUserPermissionNames_returns_union_across_roles() {
        Permission p1 = Permission.builder().name("USER_MANAGE").build();
        Permission p2 = Permission.builder().name("GRADE_EDIT").build();

        Role role1 = Role.builder().name("ADMIN").permissions(new HashSet<>(Set.of(p1))).build();
        Role role2 = Role.builder().name("TEACHER").permissions(new HashSet<>(Set.of(p2))).build();

        UserRole ur1 = UserRole.builder().userId(100L).role(role1).build();
        UserRole ur2 = UserRole.builder().userId(100L).role(role2).build();

        when(userRoleRepository.findByUserIdAndDeletedFalse(100L)).thenReturn(List.of(ur1, ur2));

        Set<String> result = service.getUserPermissionNames(100L);

        assertThat(result).containsExactlyInAnyOrder("USER_MANAGE", "GRADE_EDIT");
    }

    @Test
    void userHasPermission_true_when_any_role_has_it() {
        Permission p = Permission.builder().name("USER_MANAGE").build();
        Role role = Role.builder().name("ADMIN").permissions(new HashSet<>(Set.of(p))).build();
        UserRole ur = UserRole.builder().userId(100L).role(role).build();
        when(userRoleRepository.findByUserIdAndDeletedFalse(100L)).thenReturn(List.of(ur));

        assertThat(service.userHasPermission(100L, "USER_MANAGE")).isTrue();
        assertThat(service.userHasPermission(100L, "UNKNOWN")).isFalse();
    }
}
