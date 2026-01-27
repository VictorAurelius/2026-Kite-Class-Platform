package com.kiteclass.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Gateway filter for logging requests and responses.
 *
 * <p>Logs:
 * <ul>
 *   <li>Request: method, path, query params, headers (filtered)</li>
 *   <li>Response: status code, duration</li>
 *   <li>Assigns correlation ID for request tracking</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.6.0
 */
@Slf4j
@Component
public class LoggingFilter extends AbstractGatewayFilterFactory<LoggingFilter.Config> {

    private static final String X_CORRELATION_ID = "X-Correlation-ID";
    private static final String REQUEST_START_TIME = "requestStartTime";

    // Headers to exclude from logging (security)
    private static final List<String> SENSITIVE_HEADERS = List.of(
            HttpHeaders.AUTHORIZATION.toLowerCase(),
            "x-api-key",
            "cookie",
            "set-cookie"
    );

    public LoggingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            long startTime = Instant.now().toEpochMilli();

            // Generate or retrieve correlation ID
            String correlationId = getOrGenerateCorrelationId(request);

            // Add correlation ID to request headers
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header(X_CORRELATION_ID, correlationId)
                    .build();

            // Log request
            logRequest(modifiedRequest, correlationId);

            // Store start time in exchange attributes
            exchange.getAttributes().put(REQUEST_START_TIME, startTime);

            return chain.filter(exchange.mutate().request(modifiedRequest).build())
                    .then(Mono.fromRunnable(() -> {
                        // Log response
                        long duration = Instant.now().toEpochMilli() - startTime;
                        logResponse(exchange.getResponse(), correlationId, duration);
                    }));
        };
    }

    /**
     * Get correlation ID from request or generate new one.
     *
     * @param request HTTP request
     * @return correlation ID
     */
    private String getOrGenerateCorrelationId(ServerHttpRequest request) {
        String correlationId = request.getHeaders().getFirst(X_CORRELATION_ID);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    /**
     * Log request details.
     *
     * @param request HTTP request
     * @param correlationId correlation ID
     */
    private void logRequest(ServerHttpRequest request, String correlationId) {
        HttpMethod method = request.getMethod();
        String path = request.getPath().toString();
        String query = request.getURI().getQuery();
        String remoteAddress = getRemoteAddress(request);

        StringBuilder logMessage = new StringBuilder()
                .append("Incoming request [").append(correlationId).append("] ")
                .append(method).append(" ").append(path);

        if (query != null && !query.isEmpty()) {
            logMessage.append("?").append(query);
        }

        logMessage.append(" from ").append(remoteAddress);

        // Log headers (excluding sensitive ones)
        if (log.isDebugEnabled()) {
            logMessage.append("\nHeaders: ");
            request.getHeaders().forEach((name, values) -> {
                if (!isSensitiveHeader(name)) {
                    logMessage.append("\n  ").append(name).append(": ").append(values);
                }
            });
        }

        log.info(logMessage.toString());
    }

    /**
     * Log response details.
     *
     * @param response HTTP response
     * @param correlationId correlation ID
     * @param durationMs request duration in milliseconds
     */
    private void logResponse(ServerHttpResponse response, String correlationId, long durationMs) {
        String statusCode = response.getStatusCode() != null
                ? response.getStatusCode().toString()
                : "UNKNOWN";

        StringBuilder logMessage = new StringBuilder()
                .append("Response [").append(correlationId).append("] ")
                .append(statusCode)
                .append(" (").append(durationMs).append("ms)");

        // Log slow requests as warning
        if (durationMs > 5000) {
            log.warn("{} - SLOW REQUEST", logMessage);
        } else {
            log.info(logMessage.toString());
        }
    }

    /**
     * Get remote address from request.
     *
     * @param request HTTP request
     * @return remote address
     */
    private String getRemoteAddress(ServerHttpRequest request) {
        // Check X-Forwarded-For header (for proxies)
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }

        // Fall back to remote address
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    /**
     * Check if header is sensitive and should not be logged.
     *
     * @param headerName header name
     * @return true if header is sensitive
     */
    private boolean isSensitiveHeader(String headerName) {
        return SENSITIVE_HEADERS.contains(headerName.toLowerCase());
    }

    /**
     * Configuration properties for the filter.
     */
    public static class Config {
        // Configuration properties can be added here if needed
    }
}
