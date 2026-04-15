package com.kiteclass.core.module.branding.controller;

import com.kiteclass.core.module.branding.service.CachingBrandingPackageProxy;
import org.apache.commons.codec.digest.HmacUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalWebhookController.class)
@AutoConfigureMockMvc
@Import({InternalWebhookControllerTest.TestSecurityConfig.class, InternalWebhookControllerTest.MockConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {"internal.api.secret=test-secret-for-hmac"})
class InternalWebhookControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        public CachingBrandingPackageProxy proxy() {
            return Mockito.mock(CachingBrandingPackageProxy.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CachingBrandingPackageProxy proxy;

    @Value("${internal.api.secret}")
    private String internalApiSecret;

    private String[] hmacHeaders() {
        long timestamp = System.currentTimeMillis() / 1000;
        String ts = String.valueOf(timestamp);
        String sig = new HmacUtils("HmacSHA256", internalApiSecret).hmacHex(ts);
        return new String[]{ts, sig};
    }

    @Test
    void instance_deployed_evicts_cache() throws Exception {
        String[] hmac = hmacHeaders();

        mockMvc.perform(post("/internal/notify/instance-deployed")
                        .param("instanceId", "42")
                        .header("X-Internal-Timestamp", hmac[0])
                        .header("X-Internal-Signature", hmac[1]))
                .andExpect(status().isOk());

        verify(proxy).evict(42L);
    }

    @Test
    void instance_deployed_rejects_without_hmac_headers() throws Exception {
        mockMvc.perform(post("/internal/notify/instance-deployed")
                        .param("instanceId", "42"))
                .andExpect(status().is4xxClientError());
    }
}
