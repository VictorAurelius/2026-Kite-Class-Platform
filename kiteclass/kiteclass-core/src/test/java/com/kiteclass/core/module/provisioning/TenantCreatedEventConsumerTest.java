package com.kiteclass.core.module.provisioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link TenantCreatedEventConsumer} (Wave provisioning-1 Bucket A — GAP-945).
 *
 * <p>Verifies the keystone wiring contract:
 * <ul>
 *   <li>valid JSON payload (raw-UTF8 producer shape, GAP-925) → deserialized + saga.provision invoked</li>
 *   <li>malformed JSON → swallowed (saga untouched, no throw — broken payload must not clog queue)</li>
 *   <li>saga failure → ACK (no propagation) so the broker does not poison-loop a FAILED tenant
 *       (retry is admin-driven per GAP-953)</li>
 * </ul>
 *
 * <p>Binding (queue → email.exchange routing key {@code tenant.created}) is verified by the
 * Testcontainers round-trip IT, not this unit test.
 */
@ExtendWith(MockitoExtension.class)
class TenantCreatedEventConsumerTest {

    @Mock
    private TenantProvisioningSaga saga;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    private TenantCreatedEventConsumer consumer() {
        return new TenantCreatedEventConsumer(objectMapper, saga);
    }

    @Test
    void handle_validPayload_deserializesAndInvokesSaga() {
        when(saga.provision(org.mockito.ArgumentMatchers.any())).thenReturn(42L);
        String json = "{\"tenantId\":\"7\",\"slug\":\"acme-school\","
                + "\"audience\":\"education\",\"tone\":\"professional\"}";

        consumer().handle(json);

        ArgumentCaptor<TenantCreatedEvent> captor = ArgumentCaptor.forClass(TenantCreatedEvent.class);
        verify(saga).provision(captor.capture());
        TenantCreatedEvent event = captor.getValue();
        assertThat(event.getTenantId()).isEqualTo("7");
        assertThat(event.getSlug()).isEqualTo("acme-school");
        assertThat(event.getAudience()).isEqualTo("education");
        assertThat(event.getTone()).isEqualTo("professional");
    }

    @Test
    void handle_malformedPayload_swallowsWithoutInvokingSaga() {
        String broken = "{not-json";

        assertThatCode(() -> consumer().handle(broken)).doesNotThrowAnyException();

        verifyNoInteractions(saga);
    }

    @Test
    void handle_sagaThrows_isAckedNotPropagated() {
        when(saga.provision(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("provision blew up"));
        String json = "{\"tenantId\":\"9\",\"slug\":\"beta-school\","
                + "\"audience\":\"education\",\"tone\":\"professional\"}";

        // ACK-on-failure: consumer must NOT rethrow (saga already compensated/markFailed;
        // broker requeue would poison-loop the FAILED tenant — recovery is admin retry GAP-953).
        assertThatCode(() -> consumer().handle(json)).doesNotThrowAnyException();

        verify(saga).provision(org.mockito.ArgumentMatchers.any());
    }
}
