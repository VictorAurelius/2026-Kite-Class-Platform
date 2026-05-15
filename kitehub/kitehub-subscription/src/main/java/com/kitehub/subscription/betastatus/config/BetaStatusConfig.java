package com.kitehub.subscription.betastatus.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Wires {@code kitehub.beta-status.*} config keys documented in
 * {@code documents/01-business/kitehub/beta-status/rules.md} §Config.
 *
 * <p>Closes GAP-555 Wave 79 Bucket A — 3 unwired beta-status config keys.
 * Defaults match the rules.md column verbatim; production overrides land via
 * {@code application*.yml} per {@code production-env-config-registry.md} v1.1.1.</p>
 *
 * @since Wave 79 Bucket A — GAP-555
 */
@Component
@Getter
public class BetaStatusConfig {

    private final String contentSource;
    private final int cacheTtlSeconds;
    private final int rateLimitPerMinPerIp;

    public BetaStatusConfig(
            @Value("${kitehub.beta-status.content-source:classpath:beta-status/beta-status.md}") String contentSource,
            @Value("${kitehub.beta-status.cache-ttl-seconds:300}") int cacheTtlSeconds,
            @Value("${kitehub.beta-status.rate-limit-per-min-per-ip:60}") int rateLimitPerMinPerIp
    ) {
        this.contentSource = contentSource;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.rateLimitPerMinPerIp = rateLimitPerMinPerIp;
    }
}
