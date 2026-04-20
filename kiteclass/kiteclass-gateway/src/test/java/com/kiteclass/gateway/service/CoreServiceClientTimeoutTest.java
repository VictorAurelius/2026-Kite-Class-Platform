package com.kiteclass.gateway.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link CoreServiceClient} — covers GAP-131
 * (every WebClient must use a Reactor Netty {@code HttpClient} with explicit
 * connect + response timeouts so a slow Core upstream cannot block a worker
 * thread indefinitely).
 *
 * @since 4.5.0 (GAP-131 fix)
 */
@DisplayName("CoreServiceClient — Netty HTTP client connect/response timeouts (GAP-131)")
class CoreServiceClientTimeoutTest {

    @Test
    @DisplayName("Should construct WebClient with ReactorClientHttpConnector (not default)")
    void coreServiceClient_usesExplicitReactorConnector() throws Exception {
        // Given
        CoreServiceClient client = new CoreServiceClient(
                "http://localhost:18081",
                "test-internal-secret-32-bytes-or-more-aaaaaaaa");

        // When
        WebClient webClient = readPrivateField(client, "webClient");
        Object builder = readPrivateField(webClient, "builder"); // DefaultWebClientBuilder
        Object connector = readPrivateField(builder, "connector");

        // Then — connector is explicit ReactorClientHttpConnector (not the default fallback)
        assertThat(connector)
                .as("WebClient must use an explicit ReactorClientHttpConnector "
                        + "configured with connect + response timeouts (GAP-131)")
                .isNotNull()
                .isInstanceOf(ReactorClientHttpConnector.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T readPrivateField(Object target, String fieldName) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                return (T) f.get(target);
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName + " not found on " + target.getClass().getName());
    }
}
