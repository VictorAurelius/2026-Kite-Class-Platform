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
import java.util.Set;

/**
 * Gateway filter to resolve tenant from subdomain or header.
 * <p>
 * Resolution order:
 * 1. X-Instance-Subdomain header (for local development)
 * 2. Subdomain from Host header (e.g., customer1.kiteclass.com)
 * 3. Custom domain lookup
 * <p>
 * Verifies instance is ACTIVE or TRIAL, then adds X-Tenant-Id header.
 *
 * @since 1.0
 */
@Slf4j
@Component
public class TenantResolverFilter extends AbstractGatewayFilterFactory<TenantResolverFilter.Config> {

    private static final String X_TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String X_INSTANCE_SUBDOMAIN_HEADER = "X-Instance-Subdomain";
    private static final String BASE_DOMAIN = ".kiteclass.com";
    private static final Set<InstanceStatus> ALLOWED_STATUSES = Set.of(
        InstanceStatus.ACTIVE, InstanceStatus.TRIAL
    );

    private final InstanceRepository instanceRepository;

    public TenantResolverFilter(InstanceRepository instanceRepository) {
        super(Config.class);
        this.instanceRepository = instanceRepository;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. Try header-based resolution (local dev)
            String subdomain = request.getHeaders().getFirst(X_INSTANCE_SUBDOMAIN_HEADER);

            // 2. Try subdomain from Host header
            if (subdomain == null || subdomain.isBlank()) {
                String host = request.getURI().getHost();
                log.debug("TenantResolverFilter: Host = {}", host);
                subdomain = extractSubdomain(host);

                // 3. Try custom domain
                if (subdomain == null && host != null) {
                    Optional<Instance> customDomainInstance = instanceRepository.findByCustomDomain(host);
                    if (customDomainInstance.isPresent()) {
                        return routeToInstance(exchange, chain, customDomainInstance.get());
                    }
                }
            }

            if (subdomain == null || subdomain.isBlank()) {
                log.warn("Could not resolve tenant from request");
                return respondWithError(exchange, HttpStatus.BAD_REQUEST, "Cannot resolve tenant");
            }

            log.debug("Resolved subdomain: {}", subdomain);

            // Lookup instance by subdomain
            Optional<Instance> instanceOpt = instanceRepository.findBySubdomain(subdomain);
            if (instanceOpt.isEmpty()) {
                log.warn("Instance not found for subdomain: {}", subdomain);
                return respondWithError(exchange, HttpStatus.NOT_FOUND, "Instance not found");
            }

            return routeToInstance(exchange, chain, instanceOpt.get());
        };
    }

    private Mono<Void> routeToInstance(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain, Instance instance) {
        // Verify instance status
        if (!ALLOWED_STATUSES.contains(instance.getStatus())) {
            log.warn("Instance {} is not accessible: {}", instance.getSubdomain(), instance.getStatus());
            return respondWithError(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                "Instance is " + instance.getStatus().name().toLowerCase());
        }

        // Add X-Tenant-Id header
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header(X_TENANT_ID_HEADER, instance.getId().toString())
                .build();

        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(modifiedRequest)
                .build();

        log.debug("Routing to instance: {} (tenant ID: {})", instance.getSubdomain(), instance.getId());

        return chain.filter(modifiedExchange);
    }

    private String extractSubdomain(String host) {
        if (host == null) {
            return null;
        }

        // Strip port if present
        if (host.contains(":")) {
            host = host.substring(0, host.indexOf(":"));
        }

        // Check for kiteclass.com domain
        if (host.endsWith(BASE_DOMAIN)) {
            int endIndex = host.indexOf(BASE_DOMAIN);
            return endIndex > 0 ? host.substring(0, endIndex) : null;
        }

        // For localhost or other domains, return null (use header fallback)
        return null;
    }

    private Mono<Void> respondWithError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
    }
}
