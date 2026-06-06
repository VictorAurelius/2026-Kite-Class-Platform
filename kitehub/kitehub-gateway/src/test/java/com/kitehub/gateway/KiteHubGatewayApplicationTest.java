package com.kitehub.gateway;

import com.kitehub.gateway.repository.InstanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

/**
 * Application context test for KiteHub Gateway.
 *
 * @since 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.gateway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
        // GAP-604 (Wave 89 Bucket A): JwtAuthenticationGatewayFilter fail-fast tại
        // startup nếu JWT_SECRET không set hoặc <64 bytes (HS512 — GAP-1012). Test secret
        // ≥512 bits để khớp HS512 access-token verification.
        "jwt.secret=test-secret-64-bytes-minimum-for-hs512-access-token-key-abcdefghij"
})
class KiteHubGatewayApplicationTest {

    @MockitoBean
    private InstanceRepository instanceRepository;

    /**
     * Verify that the Spring application context loads successfully.
     */
    @Test
    void contextLoads() {
        // Context loads successfully if no exception is thrown
    }
}
