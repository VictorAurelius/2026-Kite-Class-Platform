package com.kiteclass.core.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Security filter for internal API endpoints using HMAC-SHA256 signatures.
 *
 * <p>This filter protects {@code /internal/**} endpoints by verifying HMAC-SHA256
 * signatures on incoming requests. This prevents unauthorized access and replay attacks.
 *
 * <p><strong>Security Features:</strong>
 * <ul>
 *   <li>HMAC-SHA256 signature verification (prevents header spoofing)</li>
 *   <li>Timestamp-based replay attack prevention (5-minute window)</li>
 *   <li>Constant-time signature comparison (prevents timing attacks)</li>
 *   <li>Detailed audit logging of access attempts</li>
 * </ul>
 *
 * <p><strong>Request Format:</strong>
 * <pre>
 * X-Internal-Signature: {HMAC-SHA256(secret, timestamp)}
 * X-Internal-Timestamp: {unix_timestamp_seconds}
 * </pre>
 *
 * <p>The Gateway service generates signatures using the shared secret configured
 * in {@code internal.api.secret}. Requests older than 5 minutes are rejected.
 *
 * @author KiteClass Team
 * @since 2.11.0
 */
@Slf4j
@Component
@Order(1)
public class InternalRequestFilter extends OncePerRequestFilter {

    /**
     * Header name for HMAC signature.
     */
    public static final String SIGNATURE_HEADER = "X-Internal-Signature";

    /**
     * Header name for request timestamp.
     */
    public static final String TIMESTAMP_HEADER = "X-Internal-Timestamp";

    /**
     * Path prefix for internal API endpoints.
     */
    public static final String INTERNAL_PATH_PREFIX = "/internal/";

    /**
     * Time window for accepting requests (5 minutes in seconds).
     */
    private static final int TIME_WINDOW_SECONDS = 300;

    /**
     * Old insecure default value that must be rejected.
     */
    private static final String INSECURE_DEFAULT = "changeme-in-production";

    /**
     * Shared secret for HMAC signature generation.
     * Must match the secret configured in Gateway service.
     */
    @Value("${internal.api.secret:}")
    private String internalApiSecret;

    /**
     * Validates that the internal API secret is properly configured.
     * Fails fast at startup if the secret is missing or uses the old insecure default.
     */
    @PostConstruct
    public void validateSecret() {
        if (internalApiSecret == null || internalApiSecret.isBlank() || INSECURE_DEFAULT.equals(internalApiSecret)) {
            throw new IllegalStateException(
                "INTERNAL_API_SECRET must be configured. " +
                "Set environment variable INTERNAL_API_SECRET or property internal.api.secret"
            );
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Only apply filter to /internal/** paths
        if (requestURI.startsWith(INTERNAL_PATH_PREFIX)) {
            String signature = request.getHeader(SIGNATURE_HEADER);
            String timestamp = request.getHeader(TIMESTAMP_HEADER);

            // Verify signature is present
            if (!isValidSignature(signature, timestamp)) {
                log.warn("Invalid signature for internal API: {} from IP: {}",
                        requestURI, request.getRemoteAddr());
                sendErrorResponse(response, "INVALID_INTERNAL_SIGNATURE",
                        "Invalid or missing HMAC signature");
                return;
            }

            // Verify timestamp is within acceptable window (prevent replay attacks)
            if (!isWithinTimeWindow(timestamp)) {
                log.warn("Expired request for internal API: {} (timestamp: {}) from IP: {}",
                        requestURI, timestamp, request.getRemoteAddr());
                sendErrorResponse(response, "REQUEST_EXPIRED",
                        "Request timestamp outside acceptable window");
                return;
            }

            log.debug("Internal API call authenticated: {} from IP: {}",
                    requestURI, request.getRemoteAddr());
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Validates HMAC signature using constant-time comparison.
     *
     * @param signature the signature from request header
     * @param timestamp the timestamp from request header
     * @return true if signature is valid, false otherwise
     */
    private boolean isValidSignature(String signature, String timestamp) {
        if (signature == null || timestamp == null) {
            return false;
        }

        // Calculate expected signature: HMAC-SHA256(secret, timestamp)
        String expectedSignature = new HmacUtils("HmacSHA256", internalApiSecret)
                .hmacHex(timestamp);

        // Use constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Checks if timestamp is within acceptable time window.
     *
     * @param timestamp the timestamp string (unix seconds)
     * @return true if within window, false otherwise
     */
    private boolean isWithinTimeWindow(String timestamp) {
        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis() / 1000;
            long timeDiff = Math.abs(currentTime - requestTime);

            return timeDiff <= TIME_WINDOW_SECONDS;
        } catch (NumberFormatException e) {
            log.warn("Invalid timestamp format: {}", timestamp);
            return false;
        }
    }

    /**
     * Sends error response in JSON format.
     *
     * @param response the HTTP response
     * @param errorCode the error code
     * @param message the error message
     * @throws IOException if writing response fails
     */
    private void sendErrorResponse(HttpServletResponse response,
                                    String errorCode,
                                    String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"success\":false,\"message\":\"%s\",\"errorCode\":\"%s\"}",
                message, errorCode
        ));
    }
}
