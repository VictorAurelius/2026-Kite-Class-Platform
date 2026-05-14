package com.kitehub.email.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Composite health indicator for kitehub-email scope (Wave 77 Bucket B — closes GAP-502
 * remaining 20% for email scope; closes GAP-506 Sub-B email healthcheck root cause).
 *
 * <p>Defense-in-depth on top of Spring Actuator default {@code /actuator/health}: even
 * when default composite probes report UP, this indicator surfaces RabbitMQ broker
 * reachability + JVM memory headroom directly so the docker-compose
 * {@code curl localhost:8080/actuator/health} probe reflects true service capability.
 *
 * <p><b>Statuses reported:</b>
 * <ul>
 *   <li>{@code UP} — RabbitMQ reachable AND JVM heap headroom ≥20%</li>
 *   <li>{@code DOWN} — RabbitMQ unreachable (P0; breaks branding.updated listener
 *       triggering the GAP-502 RC1 thrash pattern)</li>
 *   <li>{@code OUT_OF_SERVICE} — JVM heap usage ≥80% (GAP-502 RC2 OOM precursor;
 *       gives docker-compose healthcheck a chance to restart before kernel OOM-kills)</li>
 * </ul>
 *
 * <p><b>Email vendor reachability is intentionally NOT a fail-fast signal here.</b>
 * Phase 1 BETA uses Resend HTTP API (ADR-025 Stream A); transient vendor flakiness
 * should not flap the container. Vendor failures surface via metrics + send-failure
 * counters (see {@code MetricsConfig}); not the healthcheck.
 *
 * <p>Registered as bean {@code kiteHubEmail} → endpoint key {@code kitehubEmail}
 * (Spring strips the {@code HealthIndicator} suffix).
 *
 * @since Wave 77 Bucket B
 * @see com.kitehub.email.config.BrandingEventsConfig — owns the RabbitMQ ConnectionFactory
 * @see <a href="../../../../../../documents/04-quality/gaps/GAP-502-rabbitmq-auth-fail-plus-oom-thrash-kh-backend.md">GAP-502</a>
 */
@Slf4j
@Component("kiteHubEmail")
public class KiteHubEmailHealthIndicator implements HealthIndicator {

    /** Heap usage threshold above which the indicator returns OUT_OF_SERVICE. */
    private static final double HEAP_DEGRADED_THRESHOLD = 0.80;

    @Nullable
    private final ConnectionFactory rabbitConnectionFactory;

    private final String emailProvider;

    public KiteHubEmailHealthIndicator(
            @Nullable ConnectionFactory rabbitConnectionFactory,
            @Value("${email.provider:mock}") String emailProvider) {
        this.rabbitConnectionFactory = rabbitConnectionFactory;
        this.emailProvider = emailProvider;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        builder.withDetail("emailProvider", emailProvider);

        RabbitProbe rabbit = probeRabbit();
        builder.withDetail("rabbitmq", rabbit.detail());
        if (!rabbit.ok()) {
            return Health.down()
                    .withDetail("emailProvider", emailProvider)
                    .withDetail("rabbitmq", rabbit.detail())
                    .withDetail("reason",
                            "RabbitMQ broker unreachable — branding.updated listener cannot bind; "
                                    + "see GAP-502 RC1")
                    .build();
        }

        HeapProbe heap = probeHeap();
        builder.withDetail("heap", heap.detail());
        if (heap.degraded()) {
            return Health.outOfService()
                    .withDetail("emailProvider", emailProvider)
                    .withDetail("rabbitmq", rabbit.detail())
                    .withDetail("heap", heap.detail())
                    .withDetail("reason",
                            "JVM heap usage ≥" + (int) (HEAP_DEGRADED_THRESHOLD * 100)
                                    + "% — restart container to avoid kernel OOM-kill; see GAP-502 RC2")
                    .build();
        }

        return builder.build();
    }

    private RabbitProbe probeRabbit() {
        if (rabbitConnectionFactory == null) {
            // Tests / environments với rabbit-enabled=false. Treat as OK — the listener is
            // disabled at config time, not a runtime failure.
            return new RabbitProbe(true, "disabled");
        }
        try (Connection connection = rabbitConnectionFactory.createConnection()) {
            boolean open = connection.isOpen();
            if (!open) {
                return new RabbitProbe(false, "connection-not-open");
            }
            return new RabbitProbe(true, "reachable");
        } catch (Exception ex) {
            log.warn("RabbitMQ probe failed: {}", ex.getMessage());
            return new RabbitProbe(false, "error: " + ex.getClass().getSimpleName());
        }
    }

    private HeapProbe probeHeap() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long used = runtime.totalMemory() - runtime.freeMemory();
        double usedRatio = max <= 0 ? 0.0 : (double) used / (double) max;
        boolean degraded = usedRatio >= HEAP_DEGRADED_THRESHOLD;
        String detail = String.format(
                "used=%dMB/%dMB (%.0f%%)",
                used / (1024 * 1024),
                max / (1024 * 1024),
                usedRatio * 100);
        return new HeapProbe(degraded, detail);
    }

    private record RabbitProbe(boolean ok, String detail) {}

    private record HeapProbe(boolean degraded, String detail) {}
}
