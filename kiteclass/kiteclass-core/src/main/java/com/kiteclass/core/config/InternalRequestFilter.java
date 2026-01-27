package com.kiteclass.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security filter for internal API endpoints.
 *
 * <p>This filter protects {@code /internal/**} endpoints by requiring
 * the {@code X-Internal-Request} header to be present with value "true".
 * This provides basic protection for service-to-service communication.
 *
 * <p><strong>Security Note:</strong> In production environments, additional
 * security measures should be implemented:
 * <ul>
 *   <li>IP whitelist (only accept from Gateway IP)</li>
 *   <li>Service-to-service JWT authentication</li>
 *   <li>mTLS (mutual TLS) for encrypted communication</li>
 * </ul>
 *
 * <p>The current implementation is suitable for internal network deployments
 * where services are deployed in a trusted environment (e.g., same VPC,
 * same Kubernetes cluster).
 *
 * @author KiteClass Team
 * @since 2.11.0
 */
@Slf4j
@Component
@Order(1)
public class InternalRequestFilter extends OncePerRequestFilter {

    /**
     * Header name for internal request verification.
     */
    public static final String INTERNAL_REQUEST_HEADER = "X-Internal-Request";

    /**
     * Expected header value for internal requests.
     */
    public static final String INTERNAL_REQUEST_VALUE = "true";

    /**
     * Path prefix for internal API endpoints.
     */
    public static final String INTERNAL_PATH_PREFIX = "/internal/";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Only apply filter to /internal/** paths
        if (requestURI.startsWith(INTERNAL_PATH_PREFIX)) {
            String headerValue = request.getHeader(INTERNAL_REQUEST_HEADER);

            if (!INTERNAL_REQUEST_VALUE.equals(headerValue)) {
                log.warn("Unauthorized internal API access attempt: {} from IP: {}",
                        requestURI, request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"success\":false," +
                        "\"message\":\"Access to internal APIs is forbidden\"," +
                        "\"errorCode\":\"INTERNAL_API_FORBIDDEN\"}"
                );
                return;
            }

            log.debug("Internal API call authenticated: {} from IP: {}",
                    requestURI, request.getRemoteAddr());
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}
