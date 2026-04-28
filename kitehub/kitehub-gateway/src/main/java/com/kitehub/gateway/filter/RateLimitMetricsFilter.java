package com.kitehub.gateway.filter;

import com.kitehub.gateway.config.KeyResolverConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Emits a Micrometer counter when a downstream rate-limit filter rejects a request with
 * HTTP 429 (GAP-259).
 *
 * <p>Counter: {@code gateway.rate.limit.rejected} tagged with
 * {@code key_type} (ip / tenant / apikey / unknown) and {@code tenant} (the resolved
 * subdomain when keying by tenant; {@code unknown} otherwise).</p>
 *
 * <p>Runs after the response status is set so the counter reflects actual 429s rather than
 * upstream service rate-limits — i.e. only gateway-level enforcement is captured here.</p>
 *
 * @since 1.1.0 (GAP-259)
 */
@Slf4j
@Component
public class RateLimitMetricsFilter implements GlobalFilter, Ordered {

    static final String COUNTER_NAME = "gateway.rate.limit.rejected";

    private final MeterRegistry meterRegistry;

    public RateLimitMetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).doFinally(signal -> {
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            if (status == null || status.value() != 429) {
                return;
            }
            String keyType = resolveKeyType(exchange);
            String tenant = resolveTenant(exchange);
            Counter.builder(COUNTER_NAME)
                    .description("Gateway rate-limit rejections (HTTP 429)")
                    .tag("key_type", keyType)
                    .tag("tenant", tenant)
                    .register(meterRegistry)
                    .increment();
            log.debug("Gateway 429 — key_type={} tenant={}", keyType, tenant);
        });
    }

    /**
     * Best-effort key-type resolution. Looks at the request shape to attribute the
     * rejection to a category.
     */
    private String resolveKeyType(ServerWebExchange exchange) {
        if (exchange.getRequest().getHeaders().containsKey(KeyResolverConfig.X_API_KEY_HEADER)) {
            return "apikey";
        }
        // X-Tenant-Id is set by TenantResolverGatewayFilterFactory; X-Instance-Subdomain
        // is set by FE / dev tooling. Either signals tenant-keyed traffic.
        if (exchange.getRequest().getHeaders().containsKey("X-Tenant-Id")
                || exchange.getRequest().getHeaders().containsKey(KeyResolverConfig.X_INSTANCE_SUBDOMAIN_HEADER)) {
            return "tenant";
        }
        return "ip";
    }

    private String resolveTenant(ServerWebExchange exchange) {
        String subdomain = exchange.getRequest().getHeaders()
                .getFirst(KeyResolverConfig.X_INSTANCE_SUBDOMAIN_HEADER);
        if (subdomain != null && !subdomain.isBlank()) {
            return subdomain;
        }
        String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        // Run AFTER all upstream filters so the response status reflects the final
        // verdict (rate-limit filter writes 429 then returns; doFinally still fires).
        return Ordered.LOWEST_PRECEDENCE;
    }
}
