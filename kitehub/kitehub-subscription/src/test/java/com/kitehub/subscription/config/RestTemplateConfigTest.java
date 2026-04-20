package com.kitehub.subscription.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RestTemplateConfig} — covers GAP-131
 * (every {@code RestTemplate} must declare explicit connect + read timeouts so
 * a slow upstream cannot block a Tomcat worker thread indefinitely).
 *
 * <p>The bean is built via {@link RestTemplateBuilder}, which (on Spring 6.2+)
 * produces a {@code JdkClientHttpRequestFactory}. Spring stores the configured
 * read timeout on the factory as a {@link Duration}, and the connect timeout
 * on the underlying {@code java.net.http.HttpClient}. This test navigates both
 * via reflection — brittle vs. a full-stack integration test, but sufficient
 * to guard against the timeout regression that audit performance-2026-04-19
 * flagged.
 *
 * @since 4.5.0 (GAP-131 fix)
 */
@DisplayName("RestTemplateConfig — connect/read timeouts (GAP-131)")
class RestTemplateConfigTest {

    @Test
    @DisplayName("Should produce a RestTemplate with an explicit ClientHttpRequestFactory")
    void restTemplate_hasExplicitRequestFactory() {
        RestTemplate restTemplate = new RestTemplateConfig().restTemplate(new RestTemplateBuilder());
        assertThat(restTemplate.getRequestFactory()).isNotNull();
    }

    @Test
    @DisplayName("Connect timeout MUST be ≤ 5 s (GAP-131 — RestTemplateConfig.CONNECT_TIMEOUT)")
    void restTemplate_connectTimeoutBounded() throws Exception {
        RestTemplate restTemplate = new RestTemplateConfig().restTemplate(new RestTemplateBuilder());
        Duration connect = extractConnectTimeout(restTemplate.getRequestFactory());

        assertThat(connect)
                .as("connect timeout must be set (not infinite)")
                .isNotNull()
                .isLessThanOrEqualTo(Duration.ofSeconds(5))
                .isGreaterThan(Duration.ZERO);
        // Cross-check against the declared constant so changing one without the other fails.
        assertThat(connect).isEqualTo(RestTemplateConfig.CONNECT_TIMEOUT);
    }

    @Test
    @DisplayName("Read timeout MUST be ≤ 30 s (GAP-131 — RestTemplateConfig.READ_TIMEOUT)")
    void restTemplate_readTimeoutBounded() throws Exception {
        RestTemplate restTemplate = new RestTemplateConfig().restTemplate(new RestTemplateBuilder());
        Duration read = extractReadTimeout(restTemplate.getRequestFactory());

        assertThat(read)
                .as("read timeout must be set (not infinite)")
                .isNotNull()
                .isLessThanOrEqualTo(Duration.ofSeconds(30))
                .isGreaterThan(Duration.ZERO);
        assertThat(read).isEqualTo(RestTemplateConfig.READ_TIMEOUT);
    }

    /**
     * Extract the read timeout from Spring's request factory. Handles both
     * {@code SimpleClientHttpRequestFactory} (int millis) and
     * {@code JdkClientHttpRequestFactory} (Duration).
     */
    private Duration extractReadTimeout(ClientHttpRequestFactory factory) throws Exception {
        Field f = findField(factory.getClass(), "readTimeout");
        f.setAccessible(true);
        Object value = f.get(factory);
        if (value instanceof Duration d) {
            return d;
        }
        if (value instanceof Integer millis) {
            return Duration.ofMillis(millis);
        }
        if (value instanceof Long millisL) {
            return Duration.ofMillis(millisL);
        }
        throw new IllegalStateException(
                "Unsupported readTimeout type on " + factory.getClass().getName()
                        + ": " + (value == null ? "null" : value.getClass().getName()));
    }

    /**
     * Extract the connect timeout. For {@code JdkClientHttpRequestFactory} this
     * is stored on the underlying {@code java.net.http.HttpClient}. For
     * {@code SimpleClientHttpRequestFactory} it is an int millis field on the
     * factory itself.
     */
    private Duration extractConnectTimeout(ClientHttpRequestFactory factory) throws Exception {
        // Try direct field first (SimpleClientHttpRequestFactory).
        try {
            Field f = findField(factory.getClass(), "connectTimeout");
            f.setAccessible(true);
            Object value = f.get(factory);
            if (value instanceof Integer millis) {
                return Duration.ofMillis(millis);
            }
            if (value instanceof Duration d) {
                return d;
            }
        } catch (NoSuchFieldException ignored) {
            // Fall through — JdkClientHttpRequestFactory stores timeout on httpClient
        }

        // JdkClientHttpRequestFactory: delegate to the wrapped HttpClient.
        Field httpClientField = findField(factory.getClass(), "httpClient");
        httpClientField.setAccessible(true);
        Object httpClient = httpClientField.get(factory);
        if (httpClient instanceof java.net.http.HttpClient jdkClient) {
            return jdkClient.connectTimeout().orElseThrow(() -> new IllegalStateException(
                    "JDK HttpClient has no connectTimeout — GAP-131 regression"));
        }
        throw new IllegalStateException(
                "Cannot extract connect timeout from " + factory.getClass().getName());
    }

    private Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> c = clazz;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name + " not found on " + clazz.getName());
    }
}
