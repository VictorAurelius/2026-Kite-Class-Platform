package com.kiteclass.core.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers configuration for Core Service integration tests.
 *
 * <p>Provides PostgreSQL and Redis containers for all @SpringBootTest tests.
 * Containers are automatically started and properties configured.
 *
 * <p>Usage in tests:
 * <pre>{@code
 * @SpringBootTest
 * @Import(TestContainersConfiguration.class)
 * @ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
 * class MyIntegrationTest {
 *     // Tests here will use PostgreSQL and Redis from Testcontainers
 * }
 * }</pre>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

    @SuppressWarnings("resource") // Container is reused and managed by Testcontainers framework
    private static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withReuse(true);

    @SuppressWarnings("resource") // Container is reused and managed by Testcontainers framework
    private static final GenericContainer<?> redis =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    static {
        postgres.start();
        redis.start();
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
     * Redis container bean.
     * Container is reused across all tests for performance.
     *
     * @return configured Redis container
     */
    @Bean
    public GenericContainer<?> redisContainer() {
        return redis;
    }

    /**
     * Context initializer that configures datasource and Redis properties from Testcontainers.
     */
    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        /**
         * Configures Spring datasource and Redis properties from Testcontainers.
         *
         * @param applicationContext the application context to initialize
         */
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                "spring.datasource.url=" + postgres.getJdbcUrl(),
                "spring.datasource.username=" + postgres.getUsername(),
                "spring.datasource.password=" + postgres.getPassword(),
                "spring.data.redis.host=" + redis.getHost(),
                "spring.data.redis.port=" + redis.getMappedPort(6379)
            ).applyTo(applicationContext.getEnvironment());
        }
    }
}
