package com.kitehub.subscription.seed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Verifies {@link SystemConfigSeedDao} issues an INSERT … ON CONFLICT DO NOTHING
 * statement against the {@code system_config} table created by V27 migration.
 *
 * <p>This is the contract relied on by {@link ProductionSeedRunner} for
 * idempotent re-runs (per GAP-376 AC: "Idempotent re-run test (run twice → no
 * duplicates)"). A full Flyway migration round-trip would require Testcontainers
 * Postgres which the {@code application-test.yml} profile intentionally avoids
 * (H2 + Flyway disabled — see ConsentControllerIT comment); this unit test
 * therefore guards the SQL contract instead of the migration outcome.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SystemConfigSeedDao — V27 idempotency contract")
class SystemConfigSeedDaoTest {

    private JdbcTemplate jdbcTemplate;
    private SystemConfigSeedDao dao;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        dao = new SystemConfigSeedDao(jdbcTemplate);
    }

    @Test
    @DisplayName("upsert() issues INSERT ... ON CONFLICT DO NOTHING against system_config")
    void usesIdempotentUpsert() {
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);

        dao.upsert("default_tier", "FREE", "Default pricing tier for new instances");

        verify(jdbcTemplate).update(sql.capture(),
                eq("default_tier"),
                eq("FREE"),
                eq("Default pricing tier for new instances"));

        String issued = sql.getValue();
        assertThat(issued)
                .containsIgnoringCase("INSERT INTO system_config")
                .containsIgnoringCase("ON CONFLICT (config_key)")
                .containsIgnoringCase("DO NOTHING");
    }

    @Test
    @DisplayName("Repeated upsert() with same key passes parameters unchanged each call")
    void repeatedUpsertsAreParameterStable() {
        dao.upsert("currency", "VND", "ISO-4217");
        dao.upsert("currency", "VND", "ISO-4217");
        dao.upsert("currency", "VND", "ISO-4217");

        verify(jdbcTemplate, times(3)).update(
                org.mockito.ArgumentMatchers.anyString(),
                eq("currency"),
                eq("VND"),
                eq("ISO-4217"));
    }
}
