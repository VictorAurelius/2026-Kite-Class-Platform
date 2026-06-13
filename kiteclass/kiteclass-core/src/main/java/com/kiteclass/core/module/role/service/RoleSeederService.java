package com.kiteclass.core.module.role.service;

import com.kiteclass.core.module.role.constant.SystemRoleTemplate;
import com.kiteclass.core.module.role.dto.RoleTemplateResponse;
import com.kiteclass.core.module.role.entity.Role;
import com.kiteclass.core.module.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * RoleSeederService — provisions the 5 fixed-curated system role templates
 * (OWNER/STAFF/TEACHER/PARENT/STUDENT) for the current tenant (GAP-1119 Bucket D).
 *
 * <p>Idempotent: a template is created only when a same-named row does not already
 * exist in the tenant (UK {@code (instance_id, name)}). The {@code instance_id} is
 * stamped automatically by {@code EntityPersistenceListener} from {@code TenantContext},
 * so seeding is per-tenant without any explicit tenant argument.
 *
 * @author KiteClass Team
 * @since GAP-1119 (RBAC Bucket D)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleSeederService {

    private final RoleRepository roleRepository;

    /**
     * Seed all 5 system role templates for the current tenant (idempotent).
     *
     * @return the resolved (existing-or-created) role rows, in template order
     */
    @Transactional
    public List<Role> seedSystemRoles() {
        return Arrays.stream(SystemRoleTemplate.values())
                .map(this::getOrCreateSystemRole)
                .toList();
    }

    /**
     * Resolve a single system role template for the current tenant, creating it if absent.
     *
     * @param template the template to resolve
     * @return the existing or newly-created {@link Role}
     */
    @Transactional
    public Role getOrCreateSystemRole(SystemRoleTemplate template) {
        return roleRepository.findByNameAndDeletedFalse(template.name())
                .orElseGet(() -> {
                    log.info("Seeding system role template '{}' (level={}) for current tenant",
                            template.name(), template.getLevel());
                    return roleRepository.save(Role.builder()
                            .name(template.name())
                            .description(template.getDescription())
                            .level(template.getLevel())
                            .isSystem(true)
                            .build());
                });
    }

    /**
     * List the 5 templates with their current seed-state in the tenant (does not create).
     *
     * @return template responses with seeded flag + roleId where present
     */
    @Transactional(readOnly = true)
    public List<RoleTemplateResponse> getTemplates() {
        return Arrays.stream(SystemRoleTemplate.values())
                .map(template -> {
                    Optional<Role> existing = roleRepository.findByNameAndDeletedFalse(template.name());
                    return new RoleTemplateResponse(
                            template.name(),
                            template.getLevel(),
                            template.getDescription(),
                            existing.map(Role::getId).orElse(null),
                            existing.isPresent());
                })
                .toList();
    }
}
