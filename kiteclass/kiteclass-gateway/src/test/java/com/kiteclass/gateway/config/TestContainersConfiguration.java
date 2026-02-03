package com.kiteclass.gateway.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers configuration for integration tests.
 * Provides PostgreSQL container for tests that require full database schema.
 *
 * <p>@ServiceConnection automatically configures:
 * <ul>
 *   <li>JDBC DataSource for Flyway migrations</li>
 *   <li>R2DBC ConnectionFactory for reactive repositories</li>
 * </ul>
 *
 * <p>This configuration is automatically used by tests with @SpringBootTest
 * when Docker is available (e.g., in CI/CD environments).
 *
 * <p>Tests will be skipped if Docker is not available (e.g., WSL without Docker Desktop).
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

    /**
     * PostgreSQL container for integration tests.
     *
     * <p>@ServiceConnection automatically configures both JDBC (for Flyway) and
     * R2DBC (for repositories) from this single container bean.
     *
     * @return configured PostgreSQL container with reuse enabled
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true); // Reuse container across tests for faster execution
    }
}
