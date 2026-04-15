package com.kiteclass.core.integration;

import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test validating that Flyway migrations V28-V32 (Wave 2) apply cleanly
 * to a fresh PostgreSQL instance and produce the expected schema.
 *
 * <p>Boots a real Postgres via Testcontainers — not mocked.
 *
 * <p>Covers:
 * <ul>
 *   <li>V28: academic_years, semesters, holidays</li>
 *   <li>V29: homeroom_classes, subject_sections, curricula, subject_grades</li>
 *   <li>V30: permissions, roles, role_permissions, user_roles</li>
 *   <li>V31: frontend_instances</li>
 *   <li>V32: branding_resources</li>
 * </ul>
 *
 * @since Wave 2 Sub-PR follow-up (quality-audit 2026-04-14)
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
class Wave02MigrationsTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("V28 creates academic_years / semesters / holidays tables")
    void v28_academic_year_tables_present() {
        assertTableExists("academic_years");
        assertTableExists("semesters");
        assertTableExists("holidays");
    }

    @Test
    @DisplayName("V29 creates K-12 tables: homeroom_classes / subject_sections / curricula / subject_grades")
    void v29_k12_tables_present() {
        assertTableExists("homeroom_classes");
        assertTableExists("subject_sections");
        assertTableExists("curricula");
        assertTableExists("subject_grades");
    }

    @Test
    @DisplayName("V30 creates role hierarchy tables: permissions / roles / role_permissions / user_roles")
    void v30_role_hierarchy_tables_present() {
        assertTableExists("permissions");
        assertTableExists("roles");
        assertTableExists("role_permissions");
        assertTableExists("user_roles");
    }

    @Test
    @DisplayName("V31 creates frontend_instances with status CHECK constraint")
    void v31_frontend_instances_status_check() {
        assertTableExists("frontend_instances");

        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO frontend_instances(instance_id, tenant_id, slug, status) "
                                + "VALUES (gen_random_uuid(), 't1', 'bad-status', 'INVALID_STATUS')"
                )
        ).hasMessageContaining("chk_frontend_instance_status");
    }

    @Test
    @DisplayName("V30 role level CHECK rejects values outside 1..10")
    void v30_role_level_check_enforced() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO roles(instance_id, name, level, is_system, deleted) "
                                + "VALUES (gen_random_uuid(), 'BAD', 99, false, false)"
                )
        ).hasMessageContaining("chk_role_level");
    }

    @Test
    @DisplayName("V32 creates branding_resources with category + FK CHECK constraints")
    void v32_branding_resources_category_checks() {
        assertTableExists("branding_resources");

        // STATIC with templateId must fail
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO branding_resources(instance_id, type, category, template_id) "
                                + "VALUES (gen_random_uuid(), 'LOGO', 'STATIC', 42)"
                )
        ).hasMessageContaining("chk_branding_resource_static_no_fk");

        // TEMPLATE without templateId must fail
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO branding_resources(instance_id, type, category) "
                                + "VALUES (gen_random_uuid(), 'BANNER', 'TEMPLATE')"
                )
        ).hasMessageContaining("chk_branding_resource_template_fk");
    }

    @Test
    @DisplayName("Flyway schema history contains V28..V32 entries all marked success")
    void all_wave2_migrations_succeeded() {
        Integer successCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history "
                        + "WHERE version IN ('28','29','30','31','32') AND success = true",
                Integer.class
        );
        assertThat(successCount).isEqualTo(5);
    }

    private void assertTableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = ?",
                Integer.class, tableName
        );
        assertThat(count).as("table %s should exist", tableName).isEqualTo(1);
    }
}
