package com.kiteclass.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for Core Service internal APIs.
 *
 * <p>This client communicates with Core Service's {@code /internal/**} endpoints
 * using HMAC-SHA256 signature authentication for security.
 *
 * <p>The {@link com.kiteclass.gateway.config.InternalApiConfig} automatically adds
 * the required security headers ({@code X-Internal-Signature}, {@code X-Internal-Timestamp})
 * to all requests made through this client.
 *
 * <p><strong>Security:</strong>
 * <ul>
 *   <li>HMAC-SHA256 signature prevents unauthorized access</li>
 *   <li>Timestamp-based replay attack prevention</li>
 *   <li>Shared secret configured via {@code internal.api.secret}</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.4.0
 * @see com.kiteclass.gateway.config.InternalApiConfig
 */
@FeignClient(
    name = "kiteclass-core",
    url = "${core.service.url}",
    configuration = com.kiteclass.gateway.config.InternalApiConfig.class
)
public interface CoreServiceClient {

    /**
     * Get student by ID from Core Service internal API.
     *
     * <p>This is an example method demonstrating how to call Core Service
     * internal endpoints. Security headers are automatically added by
     * {@link com.kiteclass.gateway.config.InternalApiConfig}.
     *
     * @param id the student ID
     * @return student data as JSON string
     */
    @GetMapping("/internal/students/{id}")
    String getStudentById(@PathVariable("id") Long id);

    // Add more internal API methods as needed
}
