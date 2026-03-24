package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.dto.DatabaseCredentials;
import com.kitehub.subscription.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.UUID;

/**
 * Service for provisioning isolated databases for KiteClass instances.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseProvisioningService {

    private final InstanceRepository instanceRepository;
    private final EncryptionService encryptionService;
    private final DatabaseConnectionService connectionService;
    private final FlywayMigrationService migrationService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PASSWORD_LENGTH = 32;

    @Value("${database.master.host:localhost}")
    private String masterHost;

    @Value("${database.master.port:5433}")
    private String masterPort;

    @Value("${database.lifecycle.enabled:false}")
    private boolean lifecycleEnabled;

    /**
     * Provision a new database for the instance.
     * Creates database, user, and runs initial migrations.
     *
     * @param instanceId UUID of the instance
     * @return Database credentials
     * @throws IllegalArgumentException if instance not found
     */
    @Transactional
    public DatabaseCredentials provisionDatabase(UUID instanceId) {
        log.info("Starting database provisioning for instance: {}", instanceId);

        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        // Check if database is already provisioned (skip placeholder "pending" value)
        if (instance.getDatabaseUrl() != null
            && !instance.getDatabaseUrl().isEmpty()
            && !"pending".equals(instance.getDatabaseUrl())) {
            log.warn("Database already provisioned for instance: {}", instanceId);
            return DatabaseCredentials.fromInstance(instance, encryptionService);
        }

        // Generate database name and credentials
        String dbName = generateDatabaseName(instanceId);
        String username = generateUsername(instanceId);
        String password = generateSecurePassword();

        // Create database URL
        String databaseUrl = buildDatabaseUrl(dbName);

        // Create physical database (if lifecycle enabled)
        if (lifecycleEnabled) {
            createPhysicalDatabase(dbName, username, password);
            migrationService.runMigrations(databaseUrl, username, password);
            log.info("Physical database created and migrated: {}", dbName);
        } else {
            log.info("Simulating database creation (lifecycle disabled): {}", dbName);
        }

        // Update instance with database credentials (password encrypted using AES-256-GCM)
        instance.setDatabaseUrl(databaseUrl);
        instance.setDatabaseUsername(username);
        instance.setDatabasePassword(encryptPassword(password));

        instanceRepository.save(instance);

        log.info("Database provisioned successfully for instance: {}", instanceId);

        return DatabaseCredentials.builder()
            .databaseUrl(databaseUrl)
            .username(username)
            .password(password) // Return plain password for initial setup
            .build();
    }

    /**
     * Delete instance database and revoke user permissions.
     *
     * @param instanceId UUID of the instance
     */
    @Transactional
    public void deleteDatabase(UUID instanceId) {
        log.info("Starting database deletion for instance: {}", instanceId);

        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        if (instance.getDatabaseUrl() == null || instance.getDatabaseUrl().isEmpty()) {
            log.warn("No database to delete for instance: {}", instanceId);
            return;
        }

        String dbName = extractDatabaseName(instance.getDatabaseUrl());

        // Drop physical database (if lifecycle enabled)
        if (lifecycleEnabled) {
            // Backup before deletion is deferred until S3 infrastructure is available.
            // See DatabaseBackupScheduler for the backup strategy.

            dropPhysicalDatabase(dbName, instance.getDatabaseUsername());
            log.info("Physical database dropped: {}", dbName);
        } else {
            log.info("Simulating database deletion (lifecycle disabled): {}", dbName);
        }

        log.info("Database deleted successfully for instance: {}", instanceId);
    }

    /**
     * Check database health status.
     *
     * @param instanceId UUID of the instance
     * @return true if database is accessible, false otherwise
     */
    public boolean checkDatabaseHealth(UUID instanceId) {
        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        if (instance.getDatabaseUrl() == null || "pending".equals(instance.getDatabaseUrl())) {
            return false;
        }

        // Actual health check (if lifecycle enabled)
        if (lifecycleEnabled) {
            try {
                String decryptedPassword = encryptionService.decrypt(instance.getDatabasePassword());
                try (Connection conn = connectionService.getTenantConnection(
                        instance.getDatabaseUrl(),
                        instance.getDatabaseUsername(),
                        decryptedPassword);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1")) {

                    boolean healthy = rs.next();
                    log.info("Database health check for instance {}: {}", instanceId, healthy ? "HEALTHY" : "UNHEALTHY");
                    return healthy;
                }
            } catch (SQLException e) {
                log.error("Health check failed for instance {}: {}", instanceId, e.getMessage());
                return false;
            }
        } else {
            log.info("Simulating database health check (lifecycle disabled) for instance: {}", instanceId);
            return true; // Simulate healthy database
        }
    }

    /**
     * Generate unique database name for instance.
     * Format: kiteclass_{uuid_short}
     *
     * @param instanceId UUID of the instance
     * @return Database name
     */
    private String generateDatabaseName(UUID instanceId) {
        // Use first 8 characters of UUID for readability
        String uuidShort = instanceId.toString().replace("-", "").substring(0, 8);
        return "kiteclass_" + uuidShort;
    }

    /**
     * Generate database username.
     * Format: kiteclass_{uuid_short}_user
     *
     * @param instanceId UUID of the instance
     * @return Username
     */
    private String generateUsername(UUID instanceId) {
        String uuidShort = instanceId.toString().replace("-", "").substring(0, 8);
        return "kiteclass_" + uuidShort + "_user";
    }

    /**
     * Generate cryptographically secure random password.
     *
     * @return Random password (32 characters)
     */
    private String generateSecurePassword() {
        byte[] randomBytes = new byte[PASSWORD_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Build PostgreSQL connection URL.
     *
     * @param dbName Database name
     * @return JDBC URL
     */
    private String buildDatabaseUrl(String dbName) {
        return String.format("jdbc:postgresql://%s:%s/%s",
            masterHost, masterPort, dbName);
    }

    /**
     * Extract database name from JDBC URL.
     *
     * @param databaseUrl JDBC URL
     * @return Database name
     */
    private String extractDatabaseName(String databaseUrl) {
        // Extract from jdbc:postgresql://host:port/dbname
        String[] parts = databaseUrl.split("/");
        return parts[parts.length - 1];
    }

    /**
     * Create physical PostgreSQL database and user.
     *
     * @param dbName Database name
     * @param username Database username
     * @param password Database password (plain text)
     * @throws RuntimeException if database creation fails
     */
    private void createPhysicalDatabase(String dbName, String username, String password) {
        log.info("Creating physical database: {}", dbName);

        try (Connection conn = connectionService.getAdminConnection();
             Statement stmt = conn.createStatement()) {

            // Create user with password
            String createUserSql = String.format(
                "CREATE USER %s WITH PASSWORD '%s'",
                sanitizeIdentifier(username),
                sanitizePassword(password)
            );
            stmt.execute(createUserSql);
            log.debug("Created user: {}", username);

            // Create database owned by user
            String createDbSql = String.format(
                "CREATE DATABASE %s OWNER %s ENCODING 'UTF8'",
                sanitizeIdentifier(dbName),
                sanitizeIdentifier(username)
            );
            stmt.execute(createDbSql);
            log.debug("Created database: {}", dbName);

            // Grant all privileges
            String grantSql = String.format(
                "GRANT ALL PRIVILEGES ON DATABASE %s TO %s",
                sanitizeIdentifier(dbName),
                sanitizeIdentifier(username)
            );
            stmt.execute(grantSql);
            log.debug("Granted privileges on {} to {}", dbName, username);

            log.info("Physical database created successfully: {}", dbName);

        } catch (SQLException e) {
            log.error("Failed to create database {}: {}", dbName, e.getMessage(), e);
            throw new RuntimeException("Database creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Drop physical PostgreSQL database and user.
     *
     * @param dbName Database name
     * @param username Database username
     * @throws RuntimeException if database deletion fails
     */
    private void dropPhysicalDatabase(String dbName, String username) {
        log.info("Dropping physical database: {}", dbName);

        try (Connection conn = connectionService.getAdminConnection();
             Statement stmt = conn.createStatement()) {

            // Terminate all connections to the database
            String terminateSql = String.format(
                "SELECT pg_terminate_backend(pid) FROM pg_stat_activity " +
                "WHERE datname = '%s' AND pid <> pg_backend_pid()",
                sanitizeIdentifier(dbName)
            );
            stmt.execute(terminateSql);
            log.debug("Terminated connections to database: {}", dbName);

            // Drop database
            String dropDbSql = String.format(
                "DROP DATABASE IF EXISTS %s",
                sanitizeIdentifier(dbName)
            );
            stmt.execute(dropDbSql);
            log.debug("Dropped database: {}", dbName);

            // Drop user
            String dropUserSql = String.format(
                "DROP USER IF EXISTS %s",
                sanitizeIdentifier(username)
            );
            stmt.execute(dropUserSql);
            log.debug("Dropped user: {}", username);

            log.info("Physical database dropped successfully: {}", dbName);

        } catch (SQLException e) {
            log.error("Failed to drop database {}: {}", dbName, e.getMessage(), e);
            throw new RuntimeException("Database deletion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Sanitize SQL identifier to prevent SQL injection.
     * Validates that identifier contains only alphanumeric characters and underscores.
     *
     * @param identifier SQL identifier (database name, username, etc.)
     * @return Sanitized identifier
     * @throws IllegalArgumentException if identifier contains invalid characters
     */
    private String sanitizeIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        if (!identifier.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid identifier: " + identifier);
        }
        return identifier;
    }

    /**
     * Sanitize password by escaping single quotes.
     *
     * @param password Plain text password
     * @return Sanitized password
     */
    private String sanitizePassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        // Escape single quotes by doubling them
        return password.replace("'", "''");
    }

    /**
     * Encrypt password for storage using AES-256-GCM.
     *
     * @param plainPassword Plain text password
     * @return Encrypted password (Base64-encoded)
     */
    private String encryptPassword(String plainPassword) {
        return encryptionService.encrypt(plainPassword);
    }

    /**
     * Decrypt password from storage using AES-256-GCM.
     *
     * @param encryptedPassword Encrypted password (Base64-encoded)
     * @return Plain text password
     */
    public String decryptPassword(String encryptedPassword) {
        return encryptionService.decrypt(encryptedPassword);
    }
}
