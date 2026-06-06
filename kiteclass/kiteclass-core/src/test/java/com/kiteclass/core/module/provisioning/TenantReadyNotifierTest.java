package com.kiteclass.core.module.provisioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kiteclass.core.common.config.RabbitConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit test for {@link TenantReadyNotifier} (Wave provisioning-1 Bucket C — GAP-948).
 *
 * <p>Verifies the dedicated-dispatcher contract:
 * <ul>
 *   <li>publishes raw-UTF8 JSON to {@code email.exchange} routing key {@code tenant.deployed}
 *       with content-type {@code application/json} (GAP-925 wire-format)</li>
 *   <li>payload round-trips back to a {@link TenantDeployedEvent} with all 3 fields</li>
 *   <li>broker failure is swallowed (best-effort — never throws)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class TenantReadyNotifierTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    private TenantReadyNotifier notifier() {
        return new TenantReadyNotifier(rabbitTemplate, objectMapper);
    }

    @Test
    void notifyDeployed_publishesRawJsonToEmailExchange() throws Exception {
        notifier().notifyDeployed("7", "acme-school", 42L);

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(
                eq(RabbitConfig.EMAIL_EXCHANGE),
                eq(RabbitConfig.TENANT_DEPLOYED_ROUTING_KEY),
                msgCaptor.capture());

        Message msg = msgCaptor.getValue();
        assertThat(msg.getMessageProperties().getContentType())
                .isEqualTo(MessageProperties.CONTENT_TYPE_JSON);

        // Raw UTF-8 JSON body — NOT double-encoded — deserializes straight to the DTO.
        TenantDeployedEvent decoded = objectMapper.readValue(
                new String(msg.getBody(), StandardCharsets.UTF_8), TenantDeployedEvent.class);
        assertThat(decoded.tenantId()).isEqualTo("7");
        assertThat(decoded.slug()).isEqualTo("acme-school");
        assertThat(decoded.frontendInstanceId()).isEqualTo(42L);
    }

    @Test
    void notifyDeployed_brokerFailure_swallowed() {
        doThrow(new AmqpException("broker down"))
                .when(rabbitTemplate).send(any(), any(), any(Message.class));

        // Best-effort: provisioning already succeeded — a publish miss must NOT propagate.
        assertThatCode(() -> notifier().notifyDeployed("9", "beta-school", 1L))
                .doesNotThrowAnyException();
    }
}
