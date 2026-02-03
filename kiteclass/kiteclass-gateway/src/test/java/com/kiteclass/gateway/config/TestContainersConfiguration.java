package com.kiteclass.gateway.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.r2dbc.connectionfactory.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import redis.embedded.RedisServer;

import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.io.IOException;

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

    private RedisServer redisServer;

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

    /**
     * R2DBC TransactionManager for reactive transactions.
     * <p>Marked as @Primary to resolve ambiguity when both JDBC (for Flyway)
     * and R2DBC (for application) transaction managers are present.
     *
     * @param connectionFactory R2DBC ConnectionFactory
     * @return ReactiveTransactionManager for R2DBC
     */
    @Bean
    @Primary
    ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    /**
     * Embedded Redis server for testing.
     * Starts on port 6370 to avoid conflicts with production Redis.
     *
     * @return LettuceConnectionFactory connected to embedded Redis
     * @throws IOException if Redis server fails to start
     */
    @Bean
    @Primary
    LettuceConnectionFactory redisConnectionFactory() throws IOException {
        // Start embedded Redis on non-standard port
        redisServer = new RedisServer(6370);
        redisServer.start();

        // Configure connection to embedded Redis
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6370);
        return new LettuceConnectionFactory(config);
    }

    /**
     * Cleanup: stop embedded Redis server when context closes.
     * <p>This method is automatically called by Spring when the ApplicationContext
     * is being closed, ensuring that the embedded Redis server is properly shut down.
     *
     * @since 1.1.0
     */
    @PreDestroy
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }
}
