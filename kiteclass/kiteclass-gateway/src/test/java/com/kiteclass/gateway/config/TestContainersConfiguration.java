package com.kiteclass.gateway.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

/**
 * Testcontainers configuration for integration tests.
 * Provides PostgreSQL container for tests that require full database schema.
 *
 * <p>Configures both:
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
     * @return configured PostgreSQL container with reuse enabled
     */
    @Bean
    PostgreSQLContainer<?> postgresContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        container.start();
        return container;
    }

    /**
     * JDBC DataSource for Flyway migrations.
     *
     * @param container PostgreSQL Testcontainer
     * @return HikariCP DataSource connected to test database
     */
    @Bean
    @Primary
    DataSource dataSource(PostgreSQLContainer<?> container) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(container.getJdbcUrl());
        config.setUsername(container.getUsername());
        config.setPassword(container.getPassword());
        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }

    /**
     * R2DBC ConnectionFactory for reactive database access in tests.
     *
     * @param container PostgreSQL Testcontainer
     * @return R2DBC ConnectionFactory connected to test database
     */
    @Bean
    @Primary
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
