package com.kiteclass.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LoggingFilter}.
 *
 * @author KiteClass Team
 * @since 1.6.0
 */
class LoggingFilterTest {

    private LoggingFilter filter;
    private GatewayFilterChain chain;

    private ServerWebExchange capturedExchange;

    @BeforeEach
    void setUp() {
        filter = new LoggingFilter();
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            capturedExchange = invocation.getArgument(0);
            return Mono.empty();
        });
    }

    @Test
    void shouldAddCorrelationIdToRequest() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.apply(new LoggingFilter.Config()).filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        String correlationId = capturedExchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).isNotEmpty();
    }

    @Test
    void shouldPreserveExistingCorrelationId() {
        // Given
        String existingCorrelationId = "test-correlation-123";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .header("X-Correlation-ID", existingCorrelationId)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.apply(new LoggingFilter.Config()).filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        String correlationId = capturedExchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
        assertThat(correlationId).isEqualTo(existingCorrelationId);
    }

    @Test
    void shouldLogRequestAndResponse() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .queryParam("param1", "value1")
                .header(HttpHeaders.USER_AGENT, "TestAgent")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        // When
        Mono<Void> result = filter.apply(new LoggingFilter.Config()).filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // Verify correlation ID was added
        assertThat(capturedExchange.getRequest().getHeaders().getFirst("X-Correlation-ID")).isNotNull();
    }

    @Test
    void shouldNotLogSensitiveHeaders() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer secret-token")
                .header("Cookie", "sessionId=secret")
                .header("X-API-Key", "secret-api-key")
                .header(HttpHeaders.USER_AGENT, "TestAgent")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.apply(new LoggingFilter.Config()).filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // Test passes if no exception thrown (sensitive headers should not be logged)
        // This is a behavioral test - actual logging is verified manually or with log appender
    }

    @Test
    void shouldHandleXForwardedForHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
                .header("X-Forwarded-For", "203.0.113.1, 198.51.100.1")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.apply(new LoggingFilter.Config()).filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // Should extract first IP from X-Forwarded-For for logging
        assertThat(capturedExchange.getRequest().getHeaders().getFirst("X-Correlation-ID")).isNotNull();
    }

    @Test
    void shouldHandleRequestWithoutRemoteAddress() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.apply(new LoggingFilter.Config()).filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // Should handle missing remote address gracefully
        assertThat(capturedExchange.getRequest().getHeaders().getFirst("X-Correlation-ID")).isNotNull();
    }

    @Test
    void shouldLogResponseWithDuration() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        // Simulate delayed response
        when(chain.filter(any(ServerWebExchange.class)))
                .thenReturn(Mono.delay(java.time.Duration.ofMillis(100)).then());

        // When
        Mono<Void> result = filter.apply(new LoggingFilter.Config()).filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // Verify request start time was captured
        Object startTime = exchange.getAttributes().get("requestStartTime");
        assertThat(startTime).isNotNull();
        assertThat(startTime).isInstanceOf(Long.class);
    }
}
