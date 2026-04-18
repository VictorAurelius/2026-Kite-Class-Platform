package com.kitehub.gateway.client;

import com.kitehub.gateway.config.GatewayBrandingCacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Fetches branding from {@code kiteclass-core} for the gateway's branded error
 * pages. Keeps the fetch fully reactive so we never block on the event loop.
 *
 * <p>Caches per-tenant with a 5-minute TTL. On any failure (timeout, network
 * error, 4xx/5xx) returns {@link GatewayBranding#defaults()} so errors still
 * render with a usable look.
 *
 * @since Wave 4 (GAP-032)
 */
@Slf4j
@Service
public class BrandingClient {

    private final WebClient webClient;
    private final int timeoutSeconds;

    public BrandingClient(
            @Value("${kitehub.gateway.branding.core-base-url:http://kiteclass-core:8080}") String coreBaseUrl,
            @Value("${kitehub.gateway.branding.timeout-seconds:2}") int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        this.webClient = WebClient.builder()
                .baseUrl(coreBaseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Cacheable(value = GatewayBrandingCacheConfig.GATEWAY_BRANDING_CACHE,
            key = "#tenantId != null ? #tenantId : 'anon'",
            sync = true)
    public GatewayBranding fetch(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return GatewayBranding.defaults();
        }
        return fetchReactive(tenantId)
                .onErrorResume(ex -> {
                    log.debug("Branding fetch failed for tenant={} — using defaults: {}",
                            tenantId, ex.getMessage());
                    return Mono.just(GatewayBranding.defaults());
                })
                .block(Duration.ofSeconds(timeoutSeconds + 1L));
    }

    @SuppressWarnings("unchecked")
    private Mono<GatewayBranding> fetchReactive(String tenantId) {
        return webClient.get()
                .uri("/api/v1/branding/public?tenantId={tenantId}", tenantId)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .map(body -> mapToBranding((Map<String, Object>) body));
    }

    private GatewayBranding mapToBranding(Map<String, Object> pkg) {
        if (pkg == null) {
            return GatewayBranding.defaults();
        }
        GatewayBranding defaults = GatewayBranding.defaults();
        GatewayBranding.GatewayBrandingBuilder builder = GatewayBranding.builder()
                .displayName(defaults.getDisplayName())
                .logoUrl(defaults.getLogoUrl())
                .primaryColor(defaults.getPrimaryColor())
                .secondaryColor(defaults.getSecondaryColor());

        applyString(pkg, "displayName", builder::displayName);
        applyString(pkg, "logoUrl", builder::logoUrl);
        applyString(pkg, "primaryColor", builder::primaryColor);
        applyString(pkg, "secondaryColor", builder::secondaryColor);

        // Also tolerate the composite package shape (theme + assets) if a caller passes it.
        Object theme = pkg.get("theme");
        if (theme instanceof Map<?, ?> themeMap) {
            applyString(themeMap, "primaryColor", builder::primaryColor);
            applyString(themeMap, "secondaryColor", builder::secondaryColor);
        }
        Object slug = pkg.get("slug");
        if (slug instanceof String s && !s.isBlank()) {
            builder.displayName(s);
        }
        Object assets = pkg.get("assets");
        if (assets instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> m
                        && "LOGO".equalsIgnoreCase(String.valueOf(m.get("type")))) {
                    Object url = m.get("url");
                    if (url instanceof String logo && !logo.isBlank()) {
                        builder.logoUrl(logo);
                        break;
                    }
                }
            }
        }

        return builder.build();
    }

    private static void applyString(Map<?, ?> src, String key, java.util.function.Consumer<String> sink) {
        Object v = src.get(key);
        if (v instanceof String s && !s.isBlank()) {
            sink.accept(s);
        }
    }
}
