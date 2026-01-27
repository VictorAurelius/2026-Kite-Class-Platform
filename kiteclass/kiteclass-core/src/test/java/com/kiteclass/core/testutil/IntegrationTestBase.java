package com.kiteclass.core.testutil;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests.
 *
 * <p>Provides:
 * <ul>
 *   <li>PostgreSQL test container via Testcontainers</li>
 *   <li>Spring Boot test context</li>
 *   <li>Database connection configuration</li>
 * </ul>
 *
 * <p>Usage: Extend this class in repository integration tests.
 *
 * @author KiteClass Team
 * @since 2.3.0
 */
@SpringBootTest
@Testcontainers
public abstract class IntegrationTestBase {

    /**
     * PostgreSQL container for integration tests.
     *
     * <p>Uses PostgreSQL 15 image.
     * Container is shared across all tests in the same JVM.
     */
    @Container
    protected static final PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("kiteclass_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    /**
     * Configures Spring properties dynamically from test container.
     *
     * <p>Sets datasource URL, username, and password to point to test container.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);

        // Disable Redis and RabbitMQ for integration tests
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "63790"); // Non-standard port to avoid conflicts
        registry.add("spring.rabbitmq.host", () -> "localhost");
        registry.add("spring.rabbitmq.port", () -> "56720"); // Non-standard port to avoid conflicts

        // Use H2 for simpler cache during tests
        registry.add("spring.cache.type", () -> "simple");
    }
}
