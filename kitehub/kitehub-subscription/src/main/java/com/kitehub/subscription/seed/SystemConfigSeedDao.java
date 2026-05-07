package com.kitehub.subscription.seed;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lightweight DAO that upserts rows into the {@code system_config} table created by
 * Flyway migration {@code V27__seed_admin_system_config.sql}.
 *
 * <p>Uses raw {@link JdbcTemplate} (not JPA) intentionally — system_config is a
 * flat key/value store consumed by infrastructure callers; introducing an entity
 * + repository per `meta-gap-priority.md` YAGNI is overkill.</p>
 *
 * <p>Idempotency relies on the table's {@code config_key} unique constraint plus
 * {@code ON CONFLICT (config_key) DO NOTHING}.</p>
 *
 * @since 1.0.0
 */
@Component
public class SystemConfigSeedDao {

    private static final String UPSERT_SQL =
            "INSERT INTO system_config (config_key, config_value, description) "
          + "VALUES (?, ?, ?) "
          + "ON CONFLICT (config_key) DO NOTHING";

    private final JdbcTemplate jdbcTemplate;

    public SystemConfigSeedDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Insert a system_config row only if {@code key} is not already present.
     *
     * @param key         unique config key (e.g. {@code default_tier})
     * @param value       config value
     * @param description human-readable description for the row
     */
    public void upsert(String key, String value, String description) {
        jdbcTemplate.update(UPSERT_SQL, key, value, description);
    }
}
