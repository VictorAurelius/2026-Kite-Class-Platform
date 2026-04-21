package com.kitehub.admin;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Application context test for KiteHub Admin Service.
 * Uses H2 in PostgreSQL compatibility mode via application-test.yml.
 *
 * <p>GAP-147: admin scans {@code com.kitehub.subscription}, which pulls in
 * {@code EmailServiceClient} (requires {@link RabbitTemplate}) even when the
 * queue is disabled via {@code kitehub.email.use-queue=false}. RabbitMQ
 * auto-configuration is excluded from the admin test profile (no broker
 * available) so we mock the template to let the context come up.
 *
 * @since 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class KiteHubAdminApplicationTest {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    /**
     * Verify that the Spring application context loads successfully —
     * regression guard for GAP-147 bean-conflict + test-profile wiring.
     */
    @Test
    void contextLoads() {
        // Context loads successfully if no exception is thrown
    }
}
