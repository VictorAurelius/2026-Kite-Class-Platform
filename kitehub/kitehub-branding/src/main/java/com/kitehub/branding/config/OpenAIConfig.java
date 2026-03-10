package com.kitehub.branding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Configuration for OpenAI API integration.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "openai")
@Data
public class OpenAIConfig {

    private Api api;
    private Models models;
    private RateLimit rateLimit;
    private Timeout timeout;

    @Data
    public static class Api {
        private String key;
        private String baseUrl;
    }

    @Data
    public static class Models {
        private String vision;
        private String dalle;
        private String text;
    }

    @Data
    public static class RateLimit {
        private int requestsPerMinute;
    }

    @Data
    public static class Timeout {
        private int seconds;
    }

    /**
     * Create WebClient bean for OpenAI API calls.
     *
     * @return Configured WebClient
     */
    @Bean
    public WebClient openAIWebClient() {
        return WebClient.builder()
            .baseUrl(api.getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + api.getKey())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024)) // 10MB for images
            .build();
    }
}
