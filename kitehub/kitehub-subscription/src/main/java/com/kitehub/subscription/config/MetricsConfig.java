package com.kitehub.subscription.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires Micrometer instrumentation for SLO observability (GAP-135).
 *
 * <p>Two responsibilities:
 * <ul>
 *   <li>{@link TimedAspect} — enables {@code @Timed} annotation support so
 *       controllers tagged with {@code @Timed(value = "http.server.requests",
 *       extraTags = {"slo", "tier-c"})} export the {@code slo} tag.</li>
 *   <li>{@link MeterFilter} — forces percentile-histogram (p50/p95/p99) emission
 *       for the {@code http.server.requests} metric so Prometheus
 *       {@code histogram_quantile()} can compute SLO breaches without
 *       additional service config.</li>
 * </ul>
 *
 * <p>SLO tier rubric: see {@code documents/05-guides/api-performance-slo.md}.
 */
@Configuration
public class MetricsConfig {

    /** Enables {@code @Timed} annotation processing on Spring beans. */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /** Forces percentile-histograms on HTTP request metrics for SLO grading. */
    @Bean
    public MeterFilter httpServerRequestsHistogramFilter() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(
                    io.micrometer.core.instrument.Meter.Id id,
                    DistributionStatisticConfig config) {
                if ("http.server.requests".equals(id.getName())) {
                    return DistributionStatisticConfig.builder()
                            .percentilesHistogram(true)
                            .percentiles(0.5, 0.95, 0.99)
                            .build()
                            .merge(config);
                }
                return config;
            }
        };
    }
}
