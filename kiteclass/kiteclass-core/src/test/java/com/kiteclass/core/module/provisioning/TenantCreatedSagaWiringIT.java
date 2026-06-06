package com.kiteclass.core.module.provisioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kiteclass.core.common.config.RabbitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcontainers round-trip IT for the tenant-provisioning saga wiring
 * (Wave provisioning-1 Bucket A — GAP-945, plan §5 verification gate).
 *
 * <p>Proves the cross-service contract end-to-end through a real broker:
 * <ol>
 *   <li>producer publishes raw-UTF8 JSON (GAP-925 shape) to the {@code email.exchange}
 *       DirectExchange with routing key {@code tenant.created}</li>
 *   <li>the {@code tenant.created.queue} binding (declared by {@link RabbitConfig}) routes it</li>
 *   <li>{@link TenantCreatedEventConsumer} receives the JSON object (NOT a double-encoded string),
 *       deserializes, and invokes {@code TenantProvisioningSaga.provision(...)}</li>
 * </ol>
 *
 * <p>Named {@code *IT}: excluded from the surefire CI run (no maven-failsafe plugin per GAP-1044).
 * The CI regression guards are the unit tests {@code TenantCreatedEventConsumerTest} +
 * {@code AuthServiceTenantCreatedPublishTest}; this IT is the local broker-backed proof of the
 * binding + raw-bytes round-trip and runs via {@code ./mvnw test -Dtest=TenantCreatedSagaWiringIT}.
 */
@Testcontainers
class TenantCreatedSagaWiringIT {

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-alpine");

    @Test
    void publishToEmailExchange_routesToQueue_invokesSaga() throws Exception {
        CachingConnectionFactory cf = new CachingConnectionFactory(RABBIT.getHost());
        cf.setPort(RABBIT.getAmqpPort());
        cf.setUsername(RABBIT.getAdminUsername());
        cf.setPassword(RABBIT.getAdminPassword());

        // Declare exactly the topology RabbitConfig declares (same constants).
        RabbitAdmin admin = new RabbitAdmin(cf);
        DirectExchange exchange = ExchangeBuilder.directExchange(RabbitConfig.EMAIL_EXCHANGE).durable(true).build();
        Queue queue = QueueBuilder.durable(RabbitConfig.TENANT_CREATED_QUEUE).build();
        admin.declareExchange(exchange);
        admin.declareQueue(queue);
        admin.declareBinding(
                BindingBuilder.bind(queue).to(exchange).with(RabbitConfig.TENANT_CREATED_ROUTING_KEY));

        TenantProvisioningSaga saga = mock(TenantProvisioningSaga.class);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TenantCreatedEvent> received = new AtomicReference<>();
        when(saga.provision(any())).thenAnswer(inv -> {
            received.set(inv.getArgument(0));
            latch.countDown();
            return 99L;
        });

        ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
        // GAP-948: notifier is mocked — this IT verifies tenant.created round-trip only;
        // the tenant.deployed publish path is covered by TenantReadyNotifierTest.
        TenantReadyNotifier notifier = mock(TenantReadyNotifier.class);
        TenantCreatedEventConsumer consumer = new TenantCreatedEventConsumer(mapper, saga, notifier);

        SimpleMessageListenerContainer listener = new SimpleMessageListenerContainer(cf);
        listener.setQueueNames(RabbitConfig.TENANT_CREATED_QUEUE);
        // GAP-1045: deliver the raw Message straight to handle(Message) — exactly the production
        // @RabbitListener entry. The consumer decodes UTF-8 itself, so this IT now exercises the real
        // converter-bypass path (previously it decoded to String + called handle(String), structurally
        // unable to catch the Jackson-vs-String conversion bug).
        listener.setMessageListener((MessageListener) consumer::handle);
        listener.start();

        try {
            // Publish EXACTLY as SubscriptionEventEmitter does: raw UTF-8 bytes + Content-Type JSON.
            RabbitTemplate template = new RabbitTemplate(cf);
            String payload = "{\"tenantId\":\"00000000-0000-0000-0000-000000000055\",\"slug\":\"round-trip-school\","
                    + "\"audience\":\"education\",\"tone\":\"professional\"}";
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setContentEncoding(StandardCharsets.UTF_8.name());
            template.send(RabbitConfig.EMAIL_EXCHANGE, RabbitConfig.TENANT_CREATED_ROUTING_KEY,
                    new Message(payload.getBytes(StandardCharsets.UTF_8), props));

            assertThat(latch.await(15, TimeUnit.SECONDS))
                    .as("saga.provision invoked via broker round-trip within 15s")
                    .isTrue();
            assertThat(received.get()).isNotNull();
            assertThat(received.get().getTenantId()).isEqualTo("00000000-0000-0000-0000-000000000055");
            assertThat(received.get().getSlug()).isEqualTo("round-trip-school");
            assertThat(received.get().getAudience()).isEqualTo("education");
            assertThat(received.get().getTone()).isEqualTo("professional");
        } finally {
            listener.stop();
            cf.destroy();
        }
    }
}
