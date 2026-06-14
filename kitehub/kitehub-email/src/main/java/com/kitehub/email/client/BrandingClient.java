package com.kitehub.email.client;

import com.kitehub.email.config.BrandingCacheConfig;
import com.kitehub.email.dto.TenantBranding;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Fetches tenant branding from {@code kiteclass-core} for email template rendering.
 *
 * <p>Graceful degradation: any failure (timeout, 4xx/5xx, network error) logs a
 * warning and returns {@link TenantBranding#defaultBranding()} so emails continue
 * to send with the legacy palette. This is intentional — failing to send an
 * email because branding lookup failed would be a worse user experience.
 *
 * <p>Caches per {@code (instanceId, tenantId)} pair for 5 minutes via Caffeine.
 * Cache is evicted eagerly when {@code branding.updated} events arrive.
 *
 * <p><b>GAP-1361 — external-call resilience.</b> No {@code @CircuitBreaker} is added: this
 * client is already fail-safe by construction — an explicit Netty connect timeout (5s) +
 * response timeout (GAP-131) bound the call, and any failure degrades to
 * {@link TenantBranding#defaultBranding()} inside the {@code try/catch} below rather than
 * propagating. Email itself flows through the RabbitMQ broker (async isolation), so this
 * lookup never sits on a user-facing request thread.
 *
 * @since Wave 4 (GAP-021 — email branding propagation)
 */
@Slf4j
@Service
public class BrandingClient {

    private final WebClient webClient;
    private final int timeoutSeconds;
    private final boolean brandingEnabled;

    public BrandingClient(
            @Value("${kitehub.email.branding.core-base-url:http://kiteclass-core:8080}") String coreBaseUrl,
            @Value("${kitehub.email.branding.timeout-seconds:3}") int timeoutSeconds,
            @Value("${kitehub.email.branding-enabled:true}") boolean brandingEnabled) {
        this.timeoutSeconds = timeoutSeconds;
        this.brandingEnabled = brandingEnabled;
        // GAP-131: explicit Netty connect timeout — without this, the JVM default
        // is infinite, allowing a hung core upstream to leak Tomcat workers.
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds + 1L));
        this.webClient = WebClient.builder()
                .baseUrl(coreBaseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        log.info("BrandingClient initialized: coreBaseUrl={}, timeout={}s, enabled={}",
                coreBaseUrl, timeoutSeconds, brandingEnabled);
    }

    /**
     * Fetch branding for the given tenant. Blocks briefly (≤ timeoutSeconds)
     * because email rendering runs in a synchronous request thread.
     *
     * @param instanceId tenant instance ID (may be null)
     * @param tenantId tenant header value for multi-tenant isolation (may be null)
     * @return branding payload, never null — defaults on any failure
     */
    // GAP-043 — sync=true coalesces concurrent misses for the same key onto a single
    // loader call. Without this, a cache-expiry storm on a popular tenant can fan out
    // N concurrent requests into N upstream calls (stampede). Spring's cache abstraction
    // delegates this to the Caffeine CacheLoader under the hood when sync is enabled.
    @Cacheable(value = BrandingCacheConfig.TENANT_BRANDING_CACHE,
            key = "#instanceId != null ? #instanceId : 'anon'",
            sync = true,
            unless = "#result == null")
    @SuppressWarnings("unchecked")
    public TenantBranding fetchBranding(Long instanceId, String tenantId) {
        if (!brandingEnabled || instanceId == null) {
            return TenantBranding.defaultBranding();
        }

        try {
            Map<String, Object> response = (Map<String, Object>) webClient.get()
                    .uri("/api/v1/branding/{instanceId}/package", instanceId)
                    .headers(headers -> {
                        if (tenantId != null) {
                            headers.set("X-Tenant-Id", tenantId);
                        }
                    })
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            return mapToBranding(response);
        } catch (Exception ex) {
            log.warn("Branding lookup failed for instance={} — falling back to defaults: {}",
                    instanceId, ex.getMessage());
            return TenantBranding.defaultBranding();
        }
    }

    /**
     * Evict cached branding when a {@code branding.updated} event arrives so the
     * next email send re-fetches fresh values.
     */
    @CacheEvict(value = BrandingCacheConfig.TENANT_BRANDING_CACHE, key = "#instanceId")
    public void evict(Long instanceId) {
        log.info("Evicted cached branding for instance={}", instanceId);
    }

    private TenantBranding mapToBranding(Map<String, Object> pkg) {
        if (pkg == null) {
            return TenantBranding.defaultBranding();
        }

        TenantBranding defaults = TenantBranding.defaultBranding();
        TenantBranding.TenantBrandingBuilder builder = TenantBranding.builder()
                .displayName(defaults.getDisplayName())
                .organizationName(defaults.getOrganizationName())
                .primaryColor(defaults.getPrimaryColor())
                .secondaryColor(defaults.getSecondaryColor())
                .accentColor(defaults.getAccentColor())
                .contactEmail(defaults.getContactEmail());

        // The composite package contains theme + assets; extract the pieces emails need.
        Object theme = pkg.get("theme");
        if (theme instanceof Map<?, ?> themeMap) {
            applyIfString(themeMap, "primaryColor", builder::primaryColor);
            applyIfString(themeMap, "secondaryColor", builder::secondaryColor);
            applyIfString(themeMap, "accentColor", builder::accentColor);
        }

        Object slug = pkg.get("slug");
        if (slug instanceof String s && !s.isBlank()) {
            builder.displayName(s);
            builder.organizationName(s);
        }

        // Pull first logo asset if present.
        Object assets = pkg.get("assets");
        if (assets instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> assetMap
                        && "LOGO".equalsIgnoreCase(String.valueOf(assetMap.get("type")))) {
                    Object url = assetMap.get("url");
                    if (url instanceof String logoUrl && !logoUrl.isBlank()) {
                        builder.logoUrl(logoUrl);
                        break;
                    }
                }
            }
        }

        return builder.build();
    }

    private static void applyIfString(Map<?, ?> src, String key, java.util.function.Consumer<String> sink) {
        Object v = src.get(key);
        if (v instanceof String s && !s.isBlank()) {
            sink.accept(s);
        }
    }
}
