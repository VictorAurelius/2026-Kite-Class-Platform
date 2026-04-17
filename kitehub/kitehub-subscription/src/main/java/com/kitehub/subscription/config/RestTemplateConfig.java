package com.kitehub.subscription.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate bean for HTTP client operations.
     *
     * @param builder RestTemplate builder
     * @return Configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
