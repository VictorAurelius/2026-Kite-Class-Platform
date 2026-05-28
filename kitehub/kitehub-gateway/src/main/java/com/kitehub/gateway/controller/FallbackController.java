package com.kitehub.gateway.controller;

import com.kitehub.gateway.client.BrandingClient;
import com.kitehub.gateway.client.GatewayBranding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Fallback controller for circuit breaker + route-miss errors.
 *
 * <p>Wave 4 (GAP-032): returns branded HTML pages instead of raw JSON so end
 * users see the tenant's identity during incidents. Requests arrive via
 * internal redirects configured in {@code application.yml}; the gateway's
 * {@code TenantResolverGatewayFilterFactory} will usually have attached the
 * {@code X-Tenant-Id} header — when it hasn't (very early failure modes) we
 * fall back to default styling.
 *
 * <p>GAP-786 (Wave meta-6 walk fix 2026-05-28): methods return {@code Mono} +
 * subscribe on {@code boundedElastic} so the synchronous {@code BrandingClient
 * .fetch()} (which itself contains {@code .block()} for {@code @Cacheable}
 * compatibility) runs off the reactor event loop. Direct sync return triggered
 * {@code IllegalStateException: block()/blockFirst()/blockLast() are blocking,
 * which is not supported in thread parallel-N}.
 *
 * @since 1.0 (JSON), refactored Wave 4 + Wave meta-6 (GAP-786)
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
@RequiredArgsConstructor
public class FallbackController {

    private static final String X_TENANT_ID = "X-Tenant-Id";

    private final BrandingClient brandingClient;
    private final ErrorPageRenderer renderer;

    @RequestMapping("/subscription")
    public Mono<ResponseEntity<String>> subscriptionFallback(
            @RequestHeader(value = X_TENANT_ID, required = false) String tenantId) {
        log.warn("Circuit breaker triggered for subscription service");
        return html503(tenantId, "Subscription service");
    }

    @RequestMapping("/payment")
    public Mono<ResponseEntity<String>> paymentFallback(
            @RequestHeader(value = X_TENANT_ID, required = false) String tenantId) {
        log.warn("Circuit breaker triggered for payment service");
        return html503(tenantId, "Payment service");
    }

    @RequestMapping("/branding")
    public Mono<ResponseEntity<String>> brandingFallback(
            @RequestHeader(value = X_TENANT_ID, required = false) String tenantId) {
        log.warn("Circuit breaker triggered for branding service");
        return html503(tenantId, "Branding service");
    }

    @RequestMapping("/admin")
    public Mono<ResponseEntity<String>> adminFallback(
            @RequestHeader(value = X_TENANT_ID, required = false) String tenantId) {
        log.warn("Circuit breaker triggered for admin service");
        return html503(tenantId, "Admin service");
    }

    @RequestMapping("/email")
    public Mono<ResponseEntity<String>> emailFallback(
            @RequestHeader(value = X_TENANT_ID, required = false) String tenantId) {
        log.warn("Circuit breaker triggered for email service");
        return html503(tenantId, "Email service");
    }

    @RequestMapping("/auth")
    public Mono<ResponseEntity<String>> authFallback(
            @RequestHeader(value = X_TENANT_ID, required = false) String tenantId) {
        log.warn("Circuit breaker triggered for auth service");
        return html503(tenantId, "Auth service");
    }

    @RequestMapping("/instance")
    public Mono<ResponseEntity<String>> instanceFallback(
            @RequestHeader(value = X_TENANT_ID, required = false) String tenantId) {
        log.warn("Circuit breaker triggered for instance service");
        return html503(tenantId, "Instance service");
    }

    @RequestMapping("/not-found")
    public Mono<ResponseEntity<String>> notFound(
            @RequestHeader(value = X_TENANT_ID, required = false) String tenantId) {
        return Mono.fromCallable(() -> {
            GatewayBranding branding = safeFetch(tenantId);
            String body = renderer.render("404-not-found.html", branding,
                    "Không tìm thấy trang",
                    "Trang bạn đang tìm không tồn tại hoặc đã bị di chuyển.",
                    "");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_HTML)
                    .body(body);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @RequestMapping("/server-error")
    public Mono<ResponseEntity<String>> serverError(
            @RequestHeader(value = X_TENANT_ID, required = false) String tenantId) {
        return Mono.fromCallable(() -> {
            GatewayBranding branding = safeFetch(tenantId);
            String body = renderer.render("500-server-error.html", branding,
                    "Lỗi máy chủ",
                    "Hệ thống đang gặp sự cố.",
                    "");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_HTML)
                    .body(body);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<ResponseEntity<String>> html503(String tenantId, String serviceLabel) {
        return Mono.fromCallable(() -> {
            GatewayBranding branding = safeFetch(tenantId);
            String body = renderer.render("503-service-unavailable.html", branding,
                    "Dịch vụ tạm ngưng",
                    "Vui lòng thử lại sau vài phút.",
                    serviceLabel);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.TEXT_HTML)
                    .body(body);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private GatewayBranding safeFetch(String tenantId) {
        try {
            return brandingClient.fetch(tenantId);
        } catch (Exception ex) {
            log.debug("BrandingClient.fetch failed — using defaults: {}", ex.getMessage());
            return GatewayBranding.defaults();
        }
    }
}
