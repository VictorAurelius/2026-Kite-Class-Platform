package com.kiteclass.gateway.config;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers configuration for integration tests.
 * Provides PostgreSQL container for tests that require full database schema.
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
     * Spring Boot 3.1+ automatically configures JDBC datasource from this container for Flyway.
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

    /**
     * R2DBC ConnectionFactory for reactive database access in tests.
     * Explicitly configured from PostgreSQL Testcontainer.
     *
     * @param container PostgreSQL Testcontainer
     * @return R2DBC ConnectionFactory connected to test database
     */
    @Bean
    @DependsOn("postgresContainer")
    ConnectionFactory connectionFactory(PostgreSQLContainer<?> container) {
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, "postgresql")
                .option(ConnectionFactoryOptions.HOST, container.getHost())
                .option(ConnectionFactoryOptions.PORT, container.getFirstMappedPort())
                .option(ConnectionFactoryOptions.USER, container.getUsername())
                .option(ConnectionFactoryOptions.PASSWORD, container.getPassword())
                .option(ConnectionFactoryOptions.DATABASE, container.getDatabaseName())
                .build();

        return ConnectionFactories.get(options);
    }
}
