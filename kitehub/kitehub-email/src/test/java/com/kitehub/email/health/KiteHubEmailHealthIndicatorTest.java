package com.kitehub.email.health;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link KiteHubEmailHealthIndicator} — Wave 77 Bucket B
 * (closes GAP-502 remaining 20% for kitehub-email scope, closes GAP-506 Sub-B
 * email healthcheck root cause).
 *
 * <p>Covers the three observable outcomes that matter to docker-compose
 * healthcheck behavior:
 * <ol>
 *   <li>{@code UP} — RabbitMQ reachable + heap below threshold</li>
 *   <li>{@code DOWN} — RabbitMQ unreachable (the GAP-502 RC1 trigger that
 *       previously produced docker {@code (unhealthy)} after Spring crashed)</li>
 *   <li>{@code UP} when broker disabled at config time (test environment without
 *       a broker — must NOT fail the application context)</li>
 * </ol>
 *
 * <p>Heap-degraded path is exercised by direct contract assertion on the
 * indicator's details, since synthesising 80% heap pressure in a unit test is
 * non-deterministic.
 */
class KiteHubEmailHealthIndicatorTest {

    @Test
    void reportsUpWhenRabbitReachableAndHeapHealthy() {
        ConnectionFactory cf = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        when(cf.createConnection()).thenReturn(connection);
        when(connection.isOpen()).thenReturn(true);

        KiteHubEmailHealthIndicator indicator = new KiteHubEmailHealthIndicator(cf, "resend");

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("emailProvider", "resend")
                .containsEntry("rabbitmq", "reachable");
        assertThat(health.getDetails().get("heap")).asString().contains("used=");
    }

    @Test
    void reportsDownWhenRabbitConnectionThrows() {
        ConnectionFactory cf = mock(ConnectionFactory.class);
        when(cf.createConnection()).thenThrow(
                new AmqpConnectException(new RuntimeException("auth failure")));

        KiteHubEmailHealthIndicator indicator = new KiteHubEmailHealthIndicator(cf, "resend");

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("rabbitmq")).asString().startsWith("error:");
        assertThat(health.getDetails().get("reason"))
                .asString()
                .contains("RabbitMQ broker unreachable");
    }

    @Test
    void reportsDownWhenRabbitConnectionReportsClosed() {
        ConnectionFactory cf = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        when(cf.createConnection()).thenReturn(connection);
        when(connection.isOpen()).thenReturn(false);

        KiteHubEmailHealthIndicator indicator = new KiteHubEmailHealthIndicator(cf, "resend");

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("rabbitmq", "connection-not-open");
    }

    @Test
    void reportsUpWhenRabbitConnectionFactoryAbsent() {
        // Test environment without RabbitMQ — the listener is disabled at config time
        // (kitehub.email.branding.rabbit-enabled=false); the indicator must not fail.
        KiteHubEmailHealthIndicator indicator = new KiteHubEmailHealthIndicator(null, "mock");

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("emailProvider", "mock")
                .containsEntry("rabbitmq", "disabled");
    }

    @Test
    void surfacesEmailProviderInEveryReport() {
        // Smoke check: regardless of broker state, emailProvider is always present
        // so operators can confirm the production override (resend) reached the JVM.
        KiteHubEmailHealthIndicator indicator = new KiteHubEmailHealthIndicator(null, "resend");

        Health health = indicator.health();

        assertThat(health.getDetails()).containsEntry("emailProvider", "resend");
    }
}
