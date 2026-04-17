package com.kitehub.branding;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Application context test for KiteHub Branding Service.
 *
 * @since 1.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.host=localhost",
        "spring.rabbitmq.port=5672",
        "storage.s3.mock-mode=true"
})
class KiteHubBrandingApplicationTest {

    /**
     * Verify that the Spring application context loads successfully.
     */
    @Test
    void contextLoads() {
        // Context loads successfully if no exception is thrown
    }
}
