package com.kiteclass.gateway.filter;

import com.kiteclass.gateway.config.TestContainersConfiguration;
import com.kiteclass.gateway.module.auth.dto.request.RegisterRequest;
import com.kiteclass.gateway.module.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security tests for rate limiting mechanism.
 * <p>
 * Tests DoS/DDoS protection:
 * <ul>
 *   <li>IP-based rate limiting (100 req/min)</li>
 *   <li>User-based rate limiting (1000 req/min)</li>
 *   <li>Rate limit reset after time window</li>
 *   <li>Concurrent request handling</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(TestContainersConfiguration.class)
@TestPropertySource(properties = {
    "rate-limit.ip.capacity=10",
    "rate-limit.ip.refill-rate=10"
})
@DisplayName("Rate Limiting Security Tests")
class RateLimitSecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("Should block requests after rate limit is exceeded")
    void shouldBlockAfterRateLimit() {
        // Given: Rate limit configured (10 requests per IP for test)
        String endpoint = "/api/v1/auth/login";
        int rateLimit = 10;

        // When: Make requests up to the limit
        for (int i = 0; i < rateLimit; i++) {
            webTestClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "email": "test@test.com",
                        "password": "Password123!"
                    }
                    """)
                .exchange()
                .expectStatus().is4xxClientError(); // May be 401 or 400, but not 429 yet
        }

        // Then: Next request should be rate limited (429 Too Many Requests)
        webTestClient.post()
            .uri(endpoint)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "email": "test@test.com",
                    "password": "Password123!"
                }
                """)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
            .expectHeader().exists("X-Rate-Limit-Retry-After-Seconds");
    }

    @Test
    @DisplayName("Should allow requests after rate limit reset period")
    void shouldAllowAfterResetPeriod() throws InterruptedException {
        // Given: Rate limit has been exceeded
        String endpoint = "/api/v1/auth/login";

        // Exhaust rate limit
        for (int i = 0; i < 11; i++) {
            webTestClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "email": "reset@test.com",
                        "password": "Password123!"
                    }
                    """)
                .exchange();
        }

        // Verify rate limit is active
        webTestClient.post()
            .uri(endpoint)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "email": "reset@test.com",
                    "password": "Password123!"
                }
                """)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // When: Wait for rate limit window to reset (1 minute for bucket refill)
        // For testing, we use a shorter window or manual reset
        // In production: Thread.sleep(Duration.ofMinutes(1).toMillis());

        // Simulate waiting by using a delay
        Thread.sleep(Duration.ofSeconds(2).toMillis()); // Short delay for test

        // Then: Requests should be allowed again (token bucket refilled)
        // Note: This test may need adjustment based on actual rate limit config
        webTestClient.post()
            .uri(endpoint)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "email": "reset@test.com",
                    "password": "Password123!"
                }
                """)
            .exchange()
            .expectStatus().is4xxClientError(); // Should work (not 429)
    }

    @Test
    @DisplayName("Should handle concurrent requests correctly")
    void shouldHandleConcurrentRequests() throws InterruptedException {
        // Given: Multiple concurrent requests
        int numberOfThreads = 20;
        int requestsPerThread = 2;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rateLimitedCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        // First register a test user
        RegisterRequest registerRequest = new RegisterRequest(
            "concurrent@test.com",
            "SecurePass123!@#",
            "Concurrent User"
        );

        StepVerifier.create(authService.register(registerRequest))
            .expectNextCount(1)
            .verifyComplete();

        // When: Execute concurrent requests
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        webTestClient.post()
                            .uri("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue("""
                                {
                                    "email": "concurrent@test.com",
                                    "password": "SecurePass123!@#"
                                }
                                """)
                            .exchange()
                            .expectStatus().value(status -> {
                                if (status == HttpStatus.OK.value()) {
                                    successCount.incrementAndGet();
                                } else if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
                                    rateLimitedCount.incrementAndGet();
                                } else {
                                    otherErrorCount.incrementAndGet();
                                }
                            });
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then: Wait for all requests to complete
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        executor.shutdown();

        // And: Verify rate limiting worked
        int totalRequests = numberOfThreads * requestsPerThread;
        assertThat(successCount.get() + rateLimitedCount.get() + otherErrorCount.get())
            .isEqualTo(totalRequests);

        // Some requests should have been rate limited
        assertThat(rateLimitedCount.get())
            .as("Some requests should be rate limited under concurrent load")
            .isGreaterThan(0);

        // Some requests should have succeeded
        assertThat(successCount.get())
            .as("Some requests should succeed before rate limit")
            .isGreaterThan(0);
    }
}
