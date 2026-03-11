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
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.gateway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration"
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
