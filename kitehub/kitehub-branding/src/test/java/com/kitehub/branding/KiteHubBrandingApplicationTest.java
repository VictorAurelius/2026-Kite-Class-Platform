package com.kitehub.branding;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Application context test for KiteHub Branding Service.
 *
 * <p>Uses the shared {@code test} profile ({@code application-test.yml}: H2 mem
 * {@code testdb} as {@code sa}/empty password, Flyway off, mock AI/S3, Redis
 * autoconfig excluded) so this full-context test shares the SAME H2 datasource
 * credentials as the {@code *IntegrationTest} classes.</p>
 *
 * <p><b>Why the profile matters:</b> previously this test set
 * {@code spring.datasource.url=jdbc:h2:mem:testdb} via {@code @TestPropertySource}
 * WITHOUT activating the {@code test} profile, so it inherited the main
 * {@code application.yml} credentials ({@code kitehub}/{@code kitehub_dev_password}).
 * Running first in the {@code mvn test} suite, it created the shared in-memory
 * {@code testdb} with that admin user, locking out the {@code sa}/empty
 * integration-test contexts that reuse the same DB name
 * ({@code JdbcSQLInvalidAuthorizationSpecException: Wrong user name or password [28000]}).
 * Aligning to the {@code test} profile keeps every full-context test on identical
 * datasource credentials, eliminating the cross-context auth collision.</p>
 *
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
class KiteHubBrandingApplicationTest {

    /**
     * Verify that the Spring application context loads successfully.
     */
    @Test
    void contextLoads() {
        // Context loads successfully if no exception is thrown
    }
}
