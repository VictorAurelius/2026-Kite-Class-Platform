package com.kiteclass.gateway.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for internal API calls to Core Service.
 *
 * <p>This configuration automatically adds HMAC-SHA256 security headers to all
 * Feign client requests targeting Core Service internal endpoints.
 *
 * <p><strong>Security Headers Added:</strong>
 * <ul>
 *   <li>{@code X-Internal-Signature}: HMAC-SHA256(secret, timestamp)</li>
 *   <li>{@code X-Internal-Timestamp}: Unix timestamp in seconds</li>
 * </ul>
 *
 * <p>The Core Service validates these headers using {@code InternalRequestFilter}
 * to ensure requests are authentic and not replayed.
 *
 * <p><strong>Configuration:</strong>
 * <pre>
 * internal:
 *   api:
 *     secret: ${INTERNAL_API_SECRET}  # Must match Core Service secret
 * </pre>
 *
 * @author KiteClass Team
 * @since 2.4.0
 * @see com.kiteclass.gateway.client.CoreServiceClient
 */
@Slf4j
@Configuration
public class InternalApiConfig {

    /**
     * Shared secret for HMAC signature generation.
     * Must match the secret configured in Core Service.
     */
    @Value("${internal.api.secret}")
    private String internalApiSecret;

    /**
     * Request interceptor that adds HMAC signature headers to Feign requests.
     *
     * <p>This interceptor:
     * <ul>
     *   <li>Generates current Unix timestamp (seconds)</li>
     *   <li>Calculates HMAC-SHA256(secret, timestamp)</li>
     *   <li>Adds X-Internal-Signature and X-Internal-Timestamp headers</li>
     * </ul>
     *
     * @return configured RequestInterceptor
     */
    @Bean
    public RequestInterceptor internalApiInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // Generate timestamp (Unix seconds)
                long timestamp = System.currentTimeMillis() / 1000;
                String timestampStr = String.valueOf(timestamp);

                // Calculate HMAC-SHA256 signature
                String signature = new HmacUtils("HmacSHA256", internalApiSecret)
                        .hmacHex(timestampStr);

                // Add security headers
                template.header("X-Internal-Signature", signature);
                template.header("X-Internal-Timestamp", timestampStr);

                log.debug("Added internal API security headers: timestamp={}, signature={}",
                        timestampStr, signature.substring(0, 8) + "...");
            }
        };
    }
}
