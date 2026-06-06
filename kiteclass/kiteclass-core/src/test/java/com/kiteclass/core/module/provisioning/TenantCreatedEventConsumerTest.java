package com.kiteclass.core.module.provisioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

    @Mock
    private TenantReadyNotifier tenantReadyNotifier;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    private TenantCreatedEventConsumer consumer() {
        return new TenantCreatedEventConsumer(objectMapper, saga, tenantReadyNotifier);
    }

    @Test
    void handle_validPayload_deserializesAndInvokesSaga() {
        when(saga.provision(org.mockito.ArgumentMatchers.any())).thenReturn(42L);
        String json = "{\"tenantId\":\"00000000-0000-0000-0000-000000000007\",\"slug\":\"acme-school\","
                + "\"audience\":\"education\",\"tone\":\"professional\"}";

        consumer().handlePayload(json);

        ArgumentCaptor<TenantCreatedEvent> captor = ArgumentCaptor.forClass(TenantCreatedEvent.class);
        verify(saga).provision(captor.capture());
        TenantCreatedEvent event = captor.getValue();
        assertThat(event.getTenantId()).isEqualTo("00000000-0000-0000-0000-000000000007");
        assertThat(event.getSlug()).isEqualTo("acme-school");
        assertThat(event.getAudience()).isEqualTo("education");
        assertThat(event.getTone()).isEqualTo("professional");

        // GAP-948: provision success → publish tenant.deployed (carries saga-returned
        // frontendInstanceId) so kitehub-subscription sends the tenant-ready email.
        verify(tenantReadyNotifier).notifyDeployed("00000000-0000-0000-0000-000000000007", "acme-school", 42L);
    }

    @Test
    void handle_malformedPayload_swallowsWithoutInvokingSaga() {
        String broken = "{not-json";

        assertThatCode(() -> consumer().handlePayload(broken)).doesNotThrowAnyException();

        verifyNoInteractions(saga);
        verifyNoInteractions(tenantReadyNotifier);
    }

    @Test
    void handle_sagaThrows_isAckedNotPropagated() {
        when(saga.provision(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("provision blew up"));
        String json = "{\"tenantId\":\"00000000-0000-0000-0000-000000000009\",\"slug\":\"beta-school\","
                + "\"audience\":\"education\",\"tone\":\"professional\"}";

        // ACK-on-failure: consumer must NOT rethrow (saga already compensated/markFailed;
        // broker requeue would poison-loop the FAILED tenant — recovery is admin retry GAP-953).
        assertThatCode(() -> consumer().handlePayload(json)).doesNotThrowAnyException();

        verify(saga).provision(org.mockito.ArgumentMatchers.any());
        // GAP-948: saga failed (compensated/markFailed) → NO tenant-ready email; the tenant
        // is FAILED, not DEPLOYED.
        verify(tenantReadyNotifier, never()).notifyDeployed(any(), any(), any());
    }

    @Test
    void handle_notifierThrows_isStillAckedNotPropagated() {
        // GAP-948 defensive: even if the notifier somehow throws (it shouldn't — designed
        // best-effort), the consumer must NOT propagate (broker requeue would poison-loop a
        // tenant the saga already DEPLOYED).
        when(saga.provision(org.mockito.ArgumentMatchers.any())).thenReturn(99L);
        doThrow(new IllegalStateException("broker down"))
                .when(tenantReadyNotifier).notifyDeployed(eq("00000000-0000-0000-0000-000000000011"), eq("gamma-school"), eq(99L));
        String json = "{\"tenantId\":\"00000000-0000-0000-0000-000000000011\",\"slug\":\"gamma-school\","
                + "\"audience\":\"education\",\"tone\":\"professional\"}";

        assertThatCode(() -> consumer().handlePayload(json)).doesNotThrowAnyException();

        verify(saga).provision(org.mockito.ArgumentMatchers.any());
        verify(tenantReadyNotifier).notifyDeployed("00000000-0000-0000-0000-000000000011", "gamma-school", 99L);
    }

    @Test
    void handle_rawMessage_decodesUtf8BodyAndDelegates() {
        // GAP-1045 regression: the @RabbitListener entry takes a raw Message (NOT String) because
        // the shared Jackson2JsonMessageConverter would reject an application/json body bound to a
        // String param ("Fatal message conversion error" → message dropped → saga never runs).
        when(saga.provision(org.mockito.ArgumentMatchers.any())).thenReturn(7L);
        String json = "{\"tenantId\":\"00000000-0000-0000-0000-000000000007\",\"slug\":\"acme-school\","
                + "\"audience\":\"education\",\"tone\":\"professional\"}";
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        Message message = new Message(json.getBytes(StandardCharsets.UTF_8), props);

        consumer().handle(message);

        verify(saga).provision(org.mockito.ArgumentMatchers.any());
        verify(tenantReadyNotifier).notifyDeployed("00000000-0000-0000-0000-000000000007", "acme-school", 7L);
    }

    @Test
    void handle_nonUuidTenantId_droppedWithoutInvokingSaga() {
        // GAP-1047: the saga must establish TenantContext from the event's tenantId (the subscription
        // Instance UUID, used as the RLS tenant). A non-UUID tenantId cannot scope RLS → drop + ACK.
        String json = "{\"tenantId\":\"not-a-uuid\",\"slug\":\"acme-school\","
                + "\"audience\":\"education\",\"tone\":\"professional\"}";

        assertThatCode(() -> consumer().handlePayload(json)).doesNotThrowAnyException();

        verifyNoInteractions(saga);
        verifyNoInteractions(tenantReadyNotifier);
    }
}
