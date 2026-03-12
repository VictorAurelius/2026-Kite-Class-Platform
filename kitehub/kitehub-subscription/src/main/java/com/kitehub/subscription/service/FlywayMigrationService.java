package com.kitehub.subscription.service;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.stereotype.Service;

/**
 * Service for running Flyway database migrations on tenant databases.
 * Applies KiteClass schema to newly provisioned databases.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class FlywayMigrationService {

    private static final String MIGRATIONS_LOCATION = "classpath:db/migration/kiteclass";

    /**
     * Run Flyway migrations on a tenant database.
     * Applies all KiteClass schema migrations to initialize the database.
     *
     * @param databaseUrl JDBC URL of the target database
     * @param username Database username
     * @param password Database password (plain text)
     * @return Number of migrations applied
     * @throws RuntimeException if migration fails
     */
    public int runMigrations(String databaseUrl, String username, String password) {
        log.info("Running Flyway migrations for: {}", databaseUrl);

        try {
            Flyway flyway = Flyway.configure()
                .dataSource(databaseUrl, username, password)
                .locations(MIGRATIONS_LOCATION)
                .baselineOnMigrate(true) // Allow migrating existing databases
                .baselineVersion("0") // Start from version 0
                .load();

            MigrateResult result = flyway.migrate();

            log.info("Migrations completed for {}: {} migrations applied, target version {}",
                databaseUrl,
                result.migrationsExecuted,
                result.targetSchemaVersion);

            return result.migrationsExecuted;

        } catch (Exception e) {
            log.error("Migration failed for {}: {}", databaseUrl, e.getMessage(), e);
            throw new RuntimeException("Database migration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check migration status without applying changes.
     *
     * @param databaseUrl JDBC URL of the target database
     * @param username Database username
     * @param password Database password (plain text)
     * @return Current schema version
     */
    public String checkMigrationStatus(String databaseUrl, String username, String password) {
        try {
            Flyway flyway = Flyway.configure()
                .dataSource(databaseUrl, username, password)
                .locations(MIGRATIONS_LOCATION)
                .load();

            var info = flyway.info();
            var current = info.current();

            if (current == null) {
                return "No migrations applied";
            }

            return current.getVersion().getVersion();

        } catch (Exception e) {
            log.error("Failed to check migration status: {}", e.getMessage());
            return "Unknown";
        }
    }

    /**
     * Validate migrations without applying them.
     *
     * @param databaseUrl JDBC URL of the target database
     * @param username Database username
     * @param password Database password (plain text)
     * @return true if validation passes, false otherwise
     */
    public boolean validateMigrations(String databaseUrl, String username, String password) {
        try {
            Flyway flyway = Flyway.configure()
                .dataSource(databaseUrl, username, password)
                .locations(MIGRATIONS_LOCATION)
                .load();

            flyway.validate();
            return true;

        } catch (Exception e) {
            log.error("Migration validation failed: {}", e.getMessage());
            return false;
        }
    }
}
