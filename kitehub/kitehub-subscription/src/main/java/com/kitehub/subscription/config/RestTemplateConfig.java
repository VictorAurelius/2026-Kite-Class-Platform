package com.kitehub.subscription.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for {@link RestTemplate} — used by {@code EmailServiceClient},
 * {@code CaptchaService}, {@code VietQRService}, {@code EmailSenderService},
 * and {@code EmailConsumer}.
 *
 * <p>GAP-131 fix: explicit connect + read timeouts so a slow upstream cannot
 * block a Tomcat worker thread indefinitely. Without these, the JVM default is
 * <strong>infinite</strong>, and a single hung dependency saturates the pool
 * (cascade failure — no Resilience4j on these paths yet).
 *
 * <p>Timeout policy (per backend-standards GAP-131):
 * <ul>
 *   <li>Connect timeout: 5 s — fail fast if peer is unreachable</li>
 *   <li>Read timeout: 30 s — bounds worst-case request latency</li>
 * </ul>
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Configuration
public class RestTemplateConfig {

    /** Connect timeout (TCP handshake) — 5 seconds (GAP-131). */
    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    /** Read timeout (per response packet) — 30 seconds (GAP-131). */
    public static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    /**
     * RestTemplate bean for HTTP client operations.
     *
     * @param builder RestTemplate builder
     * @return Configured RestTemplate with explicit connect + read timeouts
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(CONNECT_TIMEOUT)
                .readTimeout(READ_TIMEOUT)
                .build();
    }
}
