package com.kitehub.email.config;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryTracingAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-112 — Distributed tracing wiring smoke test for kitehub-email.
 *
 * <p>Verifies Spring Boot auto-configures the Micrometer {@link Tracer} bean when
 * {@code micrometer-tracing-bridge-otel} + {@code opentelemetry-exporter-otlp}
 * are on the classpath and {@code management.otlp.tracing.endpoint} is configured.
 */
class TracingConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    OpenTelemetryAutoConfiguration.class,
                    OpenTelemetryTracingAutoConfiguration.class,
                    MicrometerTracingAutoConfiguration.class,
                    OtlpTracingAutoConfiguration.class));

    @Test
    void tracerBean_isWired_whenOtlpEndpointConfigured() {
        contextRunner
                .withPropertyValues(
                        "management.otlp.tracing.endpoint=http://test-tempo:4318",
                        "management.tracing.sampling.probability=1.0")
                .run(context -> assertThat(context).hasSingleBean(Tracer.class));
    }
}
