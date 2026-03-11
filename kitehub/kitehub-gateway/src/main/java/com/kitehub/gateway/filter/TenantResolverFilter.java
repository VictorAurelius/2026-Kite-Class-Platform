package com.kitehub.gateway.filter;

import com.kitehub.gateway.repository.InstanceRepository;
import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Gateway filter to resolve tenant from subdomain.
 * <p>
 * Extracts subdomain from Host header (e.g., customer1.kiteclass.com → customer1)
 * Looks up instance in database
 * Verifies instance is ACTIVE
 * Adds X-Tenant-Id header for downstream services
 *
 * @since 1.0
 */
@Slf4j
@Component
public class TenantResolverFilter extends AbstractGatewayFilterFactory<TenantResolverFilter.Config> {

    private static final String X_TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String BASE_DOMAIN = ".kiteclass.com";

    private final InstanceRepository instanceRepository;

    /**
     * Constructor.
     *
     * @param instanceRepository instance repository for tenant lookup
     */
    public TenantResolverFilter(InstanceRepository instanceRepository) {
        super(Config.class);
        this.instanceRepository = instanceRepository;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String host = request.getURI().getHost();

            log.debug("TenantResolverFilter: Host = {}", host);

            // Extract subdomain
            String subdomain = extractSubdomain(host);
            if (subdomain == null) {
                log.warn("Could not extract subdomain from host: {}", host);
                return respondWithError(exchange, HttpStatus.BAD_REQUEST, "Invalid host");
            }

            log.debug("Extracted subdomain: {}", subdomain);

            // Lookup instance
            Optional<Instance> instanceOpt = instanceRepository.findBySubdomain(subdomain);
            if (instanceOpt.isEmpty()) {
                // Try custom domain
                instanceOpt = instanceRepository.findByCustomDomain(host);
            }

            if (instanceOpt.isEmpty()) {
                log.warn("Instance not found for subdomain: {}", subdomain);
                return respondWithError(exchange, HttpStatus.NOT_FOUND, "Instance not found");
            }

            Instance instance = instanceOpt.get();

            // Verify instance is ACTIVE
            if (!InstanceStatus.ACTIVE.equals(instance.getStatus())) {
                log.warn("Instance {} is not active: {}", subdomain, instance.getStatus());
                return respondWithError(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                    "Instance is " + instance.getStatus().name().toLowerCase());
            }

            // Add X-Tenant-Id header
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header(X_TENANT_ID_HEADER, instance.getId().toString())
                    .build();

            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build();

            log.debug("Routing to instance: {} (tenant ID: {})", subdomain, instance.getId());

            return chain.filter(modifiedExchange);
        };
    }

    /**
     * Extract subdomain from host.
     *
     * @param host the host header value
     * @return subdomain or null if invalid
     */
    private String extractSubdomain(String host) {
        if (host == null || !host.endsWith(BASE_DOMAIN)) {
            return host; // Custom domain or localhost
        }

        // Extract subdomain: customer1.kiteclass.com → customer1
        int endIndex = host.indexOf(BASE_DOMAIN);
        if (endIndex <= 0) {
            return null;
        }

        return host.substring(0, endIndex);
    }

    /**
     * Respond with error.
     *
     * @param exchange the exchange
     * @param status HTTP status
     * @param message error message
     * @return Mono signaling completion
     */
    private Mono<Void> respondWithError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    /**
     * Configuration class for filter.
     */
    public static class Config {
        // Configuration properties if needed
    }
}
