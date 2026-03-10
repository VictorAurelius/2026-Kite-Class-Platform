package com.kitehub.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback controller for circuit breaker.
 * <p>
 * Returns maintenance messages when downstream services are unavailable.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * Fallback for subscription service.
     *
     * @return error response
     */
    @GetMapping("/subscription")
    public ResponseEntity<Map<String, Object>> subscriptionFallback() {
        log.warn("Circuit breaker triggered for subscription service");
        return createFallbackResponse("Subscription service is temporarily unavailable");
    }

    /**
     * Fallback for payment service.
     *
     * @return error response
     */
    @GetMapping("/payment")
    public ResponseEntity<Map<String, Object>> paymentFallback() {
        log.warn("Circuit breaker triggered for payment service");
        return createFallbackResponse("Payment service is temporarily unavailable");
    }

    /**
     * Fallback for branding service.
     *
     * @return error response
     */
    @GetMapping("/branding")
    public ResponseEntity<Map<String, Object>> brandingFallback() {
        log.warn("Circuit breaker triggered for branding service");
        return createFallbackResponse("Branding service is temporarily unavailable");
    }

    /**
     * Fallback for admin service.
     *
     * @return error response
     */
    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> adminFallback() {
        log.warn("Circuit breaker triggered for admin service");
        return createFallbackResponse("Admin service is temporarily unavailable");
    }

    /**
     * Fallback for email service.
     *
     * @return error response
     */
    @GetMapping("/email")
    public ResponseEntity<Map<String, Object>> emailFallback() {
        log.warn("Circuit breaker triggered for email service");
        return createFallbackResponse("Email service is temporarily unavailable");
    }

    /**
     * Fallback for instance APIs.
     *
     * @return error response
     */
    @GetMapping("/instance")
    public ResponseEntity<Map<String, Object>> instanceFallback() {
        log.warn("Circuit breaker triggered for instance service");
        return createFallbackResponse("Instance is temporarily unavailable");
    }

    /**
     * Create fallback response.
     *
     * @param message error message
     * @return ResponseEntity with error details
     */
    private ResponseEntity<Map<String, Object>> createFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        response.put("retryAfter", "Please try again in a few moments");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
