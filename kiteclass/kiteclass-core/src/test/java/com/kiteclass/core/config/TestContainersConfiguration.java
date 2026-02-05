package com.kiteclass.core.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers configuration for Core Service integration tests.
 *
 * <p>Provides PostgreSQL container for all @SpringBootTest tests.
 * Container is automatically started and datasource properties configured.
 *
 * <p>Usage in tests:
 * <pre>{@code
 * @SpringBootTest
 * @Import(TestContainersConfiguration.class)
 * @ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
 * class MyIntegrationTest {
 *     // Tests here will use PostgreSQL from Testcontainers
 * }
 * }</pre>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

    private static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withReuse(true);

    static {
        postgres.start();
    }

    /**
     * PostgreSQL container bean.
     * Container is reused across all tests for performance.
     *
     * @return configured PostgreSQL container
     */
    @Bean
    public PostgreSQLContainer<?> postgresContainer() {
        return postgres;
    }

    /**
     * Context initializer that configures datasource properties from Testcontainers.
     */
    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        /**
         * Configures Spring datasource properties from Testcontainers PostgreSQL.
         *
         * @param applicationContext the application context to initialize
         */
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                "spring.datasource.url=" + postgres.getJdbcUrl(),
                "spring.datasource.username=" + postgres.getUsername(),
                "spring.datasource.password=" + postgres.getPassword()
            ).applyTo(applicationContext.getEnvironment());
        }
    }
}
