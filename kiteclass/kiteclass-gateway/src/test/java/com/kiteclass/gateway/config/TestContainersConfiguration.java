package com.kiteclass.gateway.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
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
 * <p>Disables Redis and Mail auto-configuration as they are not needed for integration tests.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@TestConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration(exclude = {
    RedisAutoConfiguration.class,
    RedisReactiveAutoConfiguration.class,
    MailSenderAutoConfiguration.class
})
public class TestContainersConfiguration {

    /**
     * PostgreSQL container for integration tests.
     * Spring Boot 3.1+ automatically configures R2DBC datasource from this container.
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
