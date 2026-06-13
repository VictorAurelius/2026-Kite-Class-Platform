package com.kiteclass.core.module.role.service;

import com.kiteclass.core.module.role.constant.SystemRoleTemplate;
import com.kiteclass.core.module.role.dto.RoleTemplateResponse;
import com.kiteclass.core.module.role.entity.Role;
import com.kiteclass.core.module.role.repository.RoleRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RoleSeederService} (5 system role templates, idempotent).
 *
 * @since GAP-1119 RBAC Bucket D
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleSeederService Tests")
class RoleSeederServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleSeederService roleSeederService;

    @Test
    @DisplayName("seedSystemRoles creates all 5 templates when none exist")
    void seed_createsAll5_whenAbsent() {
        when(roleRepository.findByNameAndDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Role> seeded = roleSeederService.seedSystemRoles();

        assertThat(seeded).hasSize(5);
        assertThat(seeded).allMatch(Role::getIsSystem);
        verify(roleRepository, times(5)).save(any(Role.class));
    }

    @Test
    @DisplayName("seedSystemRoles is idempotent — no save when all exist")
    void seed_idempotent_whenAllExist() {
        when(roleRepository.findByNameAndDeletedFalse(anyString()))
                .thenAnswer(inv -> Optional.of(Role.builder().name(inv.getArgument(0)).build()));

        List<Role> seeded = roleSeederService.seedSystemRoles();

        assertThat(seeded).hasSize(5);
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("getTemplates returns 5 templates with seed-state")
    void getTemplates_returns5() {
        when(roleRepository.findByNameAndDeletedFalse(anyString())).thenReturn(Optional.empty());

        List<RoleTemplateResponse> templates = roleSeederService.getTemplates();

        assertThat(templates).hasSize(SystemRoleTemplate.values().length).hasSize(5);
        assertThat(templates).allMatch(t -> !t.seeded() && t.roleId() == null);
        assertThat(templates).extracting(RoleTemplateResponse::name)
                .containsExactlyInAnyOrder("OWNER", "STAFF", "TEACHER", "PARENT", "STUDENT");
    }
}
