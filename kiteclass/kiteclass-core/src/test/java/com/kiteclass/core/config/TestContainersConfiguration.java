package com.kiteclass.core.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers configuration for Core Service integration tests.
 *
 * <p>Provides PostgreSQL, Redis, and MinIO containers for all @SpringBootTest tests.
 * Containers are automatically started and properties configured.
 *
 * <p>Usage in tests:
 * <pre>{@code
 * @SpringBootTest
 * @Import(TestContainersConfiguration.class)
 * @ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
 * class MyIntegrationTest {
 *     // Tests here will use PostgreSQL, Redis, and MinIO from Testcontainers
 * }
 * }</pre>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

    @Container
    private static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            // GAP-1393: raise max_connections far above Postgres's default (~100) on
            // the SINGLE shared Testcontainers Postgres. The whole kiteclass-core suite
            // (69 @SpringBootTest + 11 @DataJpaTest contexts) shares THIS one container,
            // but each cached Spring context opens its OWN HikariCP pool. Under CI
            // resource pressure, overlapping context build/teardown could exhaust the
            // ~100-connection ceiling → the late, uniquely-keyed RANDOM_PORT
            // OpenApiSpecExportTest context could not open a JDBC connection for
            // Hibernate metadata ("Unable to determine Dialect without JDBC metadata"),
            // cascading to a context-load failure and (escalation 2026-06-21) a hung
            // forked JVM that blocked "Test Core Service" ~93 min. A high ceiling removes
            // the cap entirely; idle Postgres backends are cheap (Hikari minimum-idle:0
            // in application-test.yml already prevents idle pools from pinning slots).
            .withCommand("postgres", "-c", "max_connections=500");

    @Container
    @SuppressWarnings("resource") // Lifecycle managed by @Container + Testcontainers JVM shutdown hook
    private static final GenericContainer<?> redis =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    private static final MinIOContainer minio =
        new MinIOContainer(DockerImageName.parse("minio/minio:latest"));

    static {
        postgres.start();
        redis.start();
        minio.start();
    }

    /**
     * PostgreSQL container bean.
     * Container is created fresh for each test run to ensure clean state.
     *
     * @return configured PostgreSQL container
     */
    @Bean
    public PostgreSQLContainer<?> postgresContainer() {
        return postgres;
    }

    /**
     * Redis container bean.
     * Container is created fresh for each test run to ensure clean state.
     *
     * @return configured Redis container
     */
    @Bean
    public GenericContainer<?> redisContainer() {
        return redis;
    }

    /**
     * MinIO container bean (S3-compatible storage).
     * Container is created fresh for each test run to ensure clean state.
     *
     * @return configured MinIO container
     */
    @Bean
    public MinIOContainer minioContainer() {
        return minio;
    }

    /**
     * Context initializer that configures datasource and Redis properties from Testcontainers.
     */
    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        /**
         * Configures Spring datasource, Redis, and MinIO properties from Testcontainers.
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
                "spring.data.redis.port=" + redis.getMappedPort(6379),
                "storage.s3.endpoint=" + minio.getS3URL(),
                "storage.s3.access-key-id=" + minio.getUserName(),
                "storage.s3.secret-access-key=" + minio.getPassword(),
                "storage.s3.bucket-name=test-bucket"
            ).applyTo(applicationContext.getEnvironment());
        }
    }
}
