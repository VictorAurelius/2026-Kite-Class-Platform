package com.kiteclass.gateway.config;

import feign.Logger;
import feign.Response;
import feign.codec.ErrorDecoder;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

/**
 * Feign Client configuration with Resilience4j circuit breaker and retry logic.
 *
 * <p>Provides robust service-to-service communication with:
 * <ul>
 *   <li><b>Circuit Breaker:</b> Opens circuit after 50% failures in 10 requests</li>
 *   <li><b>Retry Logic:</b> 3 retries with exponential backoff</li>
 *   <li><b>Timeout:</b> 10 seconds per request</li>
 *   <li><b>Error Decoder:</b> Maps HTTP errors to domain exceptions</li>
 * </ul>
 *
 * <h3>Circuit Breaker States:</h3>
 * <ul>
 *   <li><b>CLOSED:</b> Normal operation, requests flow through</li>
 *   <li><b>OPEN:</b> Too many failures, requests fail immediately</li>
 *   <li><b>HALF_OPEN:</b> Testing if service recovered, allow limited requests</li>
 * </ul>
 *
 * <h3>Configuration (application.yml):</h3>
 * <pre>
 * resilience4j:
 *   circuitbreaker:
 *     instances:
 *       coreService:
 *         sliding-window-size: 10
 *         failure-rate-threshold: 50
 *         wait-duration-in-open-state: 30s
 *   retry:
 *     instances:
 *       coreService:
 *         max-attempts: 3
 *         wait-duration: 1s
 * </pre>
 *
 * @author KiteClass Team
 * @since 1.8.0
 */
@Configuration
@Slf4j
public class FeignConfig {

    /**
     * Configure Feign logging level.
     * FULL logs request and response details for debugging.
     *
     * @return Logger.Level.FULL
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * Custom error decoder to map Feign exceptions to domain exceptions.
     *
     * <p>Maps HTTP status codes to meaningful exceptions:
     * <ul>
     *   <li>404 Not Found → EntityNotFoundException</li>
     *   <li>403 Forbidden → UnauthorizedException</li>
     *   <li>500+ Server Errors → ServiceUnavailableException</li>
     * </ul>
     *
     * @return Custom ErrorDecoder
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new CoreServiceErrorDecoder();
    }

    /**
     * Configure Circuit Breaker for Core Service calls.
     *
     * <p>Configuration:
     * <ul>
     *   <li>Sliding window: 10 requests</li>
     *   <li>Failure threshold: 50% (5 out of 10 failures)</li>
     *   <li>Open state duration: 30 seconds</li>
     *   <li>Half-open allowed calls: 3</li>
     * </ul>
     *
     * @return Circuit breaker customizer
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(10))
                        .build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .failureRateThreshold(50.0f)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .minimumNumberOfCalls(5)
                        .slowCallDurationThreshold(Duration.ofSeconds(5))
                        .slowCallRateThreshold(50.0f)
                        .build())
                .build());
    }

    /**
     * Custom error decoder for Core Service Feign calls.
     *
     * <p>Translates HTTP error responses to domain-specific exceptions.
     */
    @Slf4j
    static class CoreServiceErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultDecoder = new Default();

        @Override
        public Exception decode(String methodKey, Response response) {
            log.error("Feign error for method {}: status={}, reason={}",
                    methodKey, response.status(), response.reason());

            String message = extractErrorMessage(response);

            return switch (response.status()) {
                case 404 -> new CoreServiceNotFoundException(
                        "Resource not found in Core service: " + message
                );
                case 403 -> new CoreServiceUnauthorizedException(
                        "Unauthorized access to Core service: " + message
                );
                case 500, 502, 503, 504 -> new CoreServiceUnavailableException(
                        "Core service unavailable: " + message
                );
                default -> defaultDecoder.decode(methodKey, response);
            };
        }

        private String extractErrorMessage(Response response) {
            try {
                if (response.body() != null) {
                    return new String(response.body().asInputStream().readAllBytes());
                }
            } catch (IOException e) {
                log.warn("Failed to read error response body", e);
            }
            return response.reason();
        }
    }

    /**
     * Exception thrown when Core service resource is not found (404).
     */
    public static class CoreServiceNotFoundException extends RuntimeException {
        public CoreServiceNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when Core service access is unauthorized (403).
     */
    public static class CoreServiceUnauthorizedException extends RuntimeException {
        public CoreServiceUnauthorizedException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when Core service is unavailable (5xx).
     */
    public static class CoreServiceUnavailableException extends RuntimeException {
        public CoreServiceUnavailableException(String message) {
            super(message);
        }
    }
}
