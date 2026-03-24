package com.kiteclass.core.testutil;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for repository slice tests.
 *
 * <p>Uses {@link DataJpaTest} for lightweight repository testing:
 * <ul>
 *   <li>Only loads JPA components (repositories, entities)</li>
 *   <li>No full Spring Boot context (faster startup)</li>
 *   <li>No Redis, RabbitMQ, Security, etc.</li>
 *   <li>PostgreSQL test container via Testcontainers</li>
 * </ul>
 *
 * <p>Usage: Extend this class in repository integration tests.
 *
 * <p><strong>Pattern:</strong> Repository Slice Tests use {@code @DataJpaTest}
 * with manual {@code @Container} setup (per MEMORY.md guidelines).
 *
 * @author KiteClass Team
 * @since 2.3.0
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public abstract class IntegrationTestBase {

    /**
     * PostgreSQL container for integration tests.
     *
     * <p>Uses PostgreSQL 15 image.
     * Container is shared across all tests in the same JVM.
     */
    @Container
    @SuppressWarnings("resource")
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
    }
}
