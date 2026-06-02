package com.kiteclass.core.common.config;

import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Application-context integration test verifying RabbitMQ wiring boots clean.
 *
 * <p>Closes GAP-866 — kc-core crashloop caused by missing {@link RabbitAdmin}
 * bean when Spring tried to satisfy the implicit eager queue declarer's autowire
 * for {@link org.springframework.amqp.core.AmqpAdmin}. Without an explicit
 * {@code @Bean RabbitAdmin}, certain autoconfig orderings (or downstream
 * consumers expecting the bean) trigger {@code UnsatisfiedDependencyException}
 * at startup → restart loop → gateway 503 fallback for every {@code /api/v1/*}.
 *
 * <p><strong>What this test catches:</strong>
 * <ul>
 *   <li>{@code RabbitAdmin} bean missing from the application context</li>
 *   <li>{@code RabbitTemplate} bean missing (sister wiring)</li>
 *   <li>{@code @Bean Queue} declarations failing to register</li>
 *   <li>{@code UnsatisfiedDependencyException} for {@code declareRabbitQueuesEagerly}-style
 *       runners that Spring Boot's {@code RabbitAutoConfiguration} may create when
 *       explicit {@link Queue} beans are present</li>
 * </ul>
 *
 * <p>Per {@code pre-handoff-self-test-completeness.md} §2.9 (background-job class) +
 * {@code design-patterns.md} §3.5 (Outbox pattern) — this test verifies the broker
 * wiring layer that ClassRescheduledEvent + ClassRescheduledEmailQueue depend on.
 *
 * <p><strong>Why @SpringBootTest, not @DataJpaTest:</strong> The crashloop fired at
 * full application context refresh time — only a full SpringBootTest reproduces the
 * autoconfig graph that surfaced the missing bean.
 *
 * @see RabbitConfig
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@DisplayName("RabbitConfig — application context boots clean (GAP-866)")
class RabbitConfigContextIT {

    @Autowired(required = false)
    private RabbitAdmin rabbitAdmin;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Autowired(required = false)
    private RabbitConfig rabbitConfig;

    @Test
    @DisplayName("RabbitAdmin bean exists in application context")
    void rabbitAdminBeanExists() {
        assertThat(rabbitAdmin)
            .as("RabbitAdmin must be autowire-available to satisfy Spring AMQP eager queue declarer; "
                + "missing bean caused GAP-866 crashloop")
            .isNotNull();
    }

    @Test
    @DisplayName("RabbitTemplate bean exists (sister wiring)")
    void rabbitTemplateBeanExists() {
        assertThat(rabbitTemplate)
            .as("RabbitTemplate must wire alongside RabbitAdmin for producer-side publishing")
            .isNotNull();
    }

    @Test
    @DisplayName("Class-rescheduled queue beans register (Outbox consumer side)")
    void queueBeansRegister() {
        assertThat(rabbitConfig).isNotNull();
        Queue rescheduled = rabbitConfig.classRescheduledQueue();
        Queue email = rabbitConfig.classRescheduledEmailQueue();

        assertThat(rescheduled.getName())
            .as("class.rescheduled.queue must be declared per ADR-021 outbox flow")
            .isEqualTo("class.rescheduled.queue");
        assertThat(email.getName())
            .as("class.rescheduled.email.queue must be declared for kitehub-email forwarding")
            .isEqualTo("class.rescheduled.email.queue");
        assertThat(rescheduled.isDurable())
            .as("Durable queue required for delivery guarantees")
            .isTrue();
        assertThat(email.isDurable()).isTrue();
    }
}
