package com.kitehub.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Key resolvers for rate limiting.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Configuration
public class KeyResolverConfig {

    /**
     * Rate limit by client IP address.
     * Used for register endpoint to prevent spam.
     *
     * @return KeyResolver based on remote address
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }
}
