package com.kiteclass.core.module.role;

import com.kiteclass.core.module.role.entity.Permission;
import com.kiteclass.core.module.role.entity.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleEntityTest {

    @Test
    void grantPermission_adds_to_set() {
        Role role = Role.builder().name("ADMIN").build();
        Permission p1 = Permission.builder().name("USER_MANAGE").build();

        role.grantPermission(p1);

        assertThat(role.getPermissions()).containsExactly(p1);
    }

    @Test
    void grantPermission_idempotent() {
        Role role = Role.builder().name("ADMIN").build();
        Permission p1 = Permission.builder().name("USER_MANAGE").build();

        role.grantPermission(p1);
        role.grantPermission(p1);

        assertThat(role.getPermissions()).hasSize(1);
    }

    @Test
    void revokePermission_removes() {
        Role role = Role.builder().name("ADMIN").build();
        Permission p1 = Permission.builder().name("USER_MANAGE").build();
        role.grantPermission(p1);

        role.revokePermission(p1);

        assertThat(role.getPermissions()).isEmpty();
    }

    @Test
    void hasPermission_true_when_granted() {
        Role role = Role.builder().name("ADMIN").build();
        Permission p1 = Permission.builder().name("USER_MANAGE").build();
        role.grantPermission(p1);

        assertThat(role.hasPermission("USER_MANAGE")).isTrue();
        assertThat(role.hasPermission("ROLE_ASSIGN")).isFalse();
    }

    @Test
    void hasPermission_false_when_no_permissions() {
        Role role = Role.builder().name("VIEWER").build();

        assertThat(role.hasPermission("USER_MANAGE")).isFalse();
    }
}
