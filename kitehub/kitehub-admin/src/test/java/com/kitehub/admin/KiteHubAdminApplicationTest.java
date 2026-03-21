package com.kitehub.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Application context test for KiteHub Admin Service.
 * Uses H2 in PostgreSQL compatibility mode via application-test.yml.
 *
 * @since 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class KiteHubAdminApplicationTest {

    /**
     * Verify that the Spring application context loads successfully.
     */
    @Test
    void contextLoads() {
        // Context loads successfully if no exception is thrown
    }
}
