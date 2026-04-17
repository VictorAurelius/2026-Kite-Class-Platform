package com.kitehub.subscription.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Service for managing PostgreSQL admin connections.
 * Provides connections to the master PostgreSQL instance for database lifecycle operations.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class DatabaseConnectionService {

    @Value("${database.admin.url:jdbc:postgresql://localhost:5433/postgres}")
    private String adminUrl;

    @Value("${database.admin.username:postgres}")
    private String adminUsername;

    @Value("${database.admin.password:}")
    private String adminPassword;

    /**
     * Get admin connection to PostgreSQL master instance.
     * Used for CREATE DATABASE, DROP DATABASE, and user management operations.
     *
     * @return Connection to master PostgreSQL instance
     * @throws SQLException if connection fails
     */
    public Connection getAdminConnection() throws SQLException {
        log.debug("Connecting to PostgreSQL admin: {}", adminUrl);

        if (adminPassword == null || adminPassword.isEmpty()) {
            log.warn("Admin password not configured - using empty password");
        }

        return DriverManager.getConnection(adminUrl, adminUsername, adminPassword);
    }

    /**
     * Test admin connection availability.
     *
     * @return true if admin connection is available, false otherwise
     */
    public boolean testAdminConnection() {
        try (Connection conn = getAdminConnection()) {
            return conn.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            log.error("Admin connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get connection to a specific tenant database.
     *
     * @param databaseUrl Database JDBC URL
     * @param username Database username
     * @param password Database password (plain text)
     * @return Connection to tenant database
     * @throws SQLException if connection fails
     */
    public Connection getTenantConnection(String databaseUrl, String username, String password) throws SQLException {
        log.debug("Connecting to tenant database: {}", databaseUrl);
        return DriverManager.getConnection(databaseUrl, username, password);
    }
}
