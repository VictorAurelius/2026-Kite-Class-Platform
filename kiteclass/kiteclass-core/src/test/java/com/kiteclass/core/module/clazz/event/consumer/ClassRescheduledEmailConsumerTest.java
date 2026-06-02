package com.kiteclass.core.module.clazz.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kiteclass.core.common.idempotency.EmailIdempotencyGuard;
import com.kiteclass.core.module.clazz.event.ClassRescheduledEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit test for {@link ClassRescheduledEmailConsumer} (Wave beta-readiness-4 Bucket D — GAP-291;
 * GAP-840 dedup Wave local-doable-6 Bucket H).
 *
 * <p>Verifies feature-flag-enabled path: consumer forwards payload to
 * {@code class.rescheduled.email.queue} for kitehub-email to render + dispatch, AND
 * suppresses duplicate forwards on at-least-once redelivery via
 * {@link EmailIdempotencyGuard}.
 *
 * <p>Bean activation (`@ConditionalOnProperty` with `havingValue="true"`) verified by
 * Spring Boot AutoConfiguration — not in scope of this unit test.
 *
 * @author KiteClass Team
 * @since Wave beta-readiness-4 Bucket D (GAP-291)
 */
@ExtendWith(MockitoExtension.class)
class ClassRescheduledEmailConsumerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    /**
     * Real Caffeine-only guard (no Redis dep in unit test) — verifies the dedup
     * behavior end-to-end through {@code markIfFirstSeen} without mocking it,
     * because the dedup semantics IS the AC.
     */
    private EmailIdempotencyGuard guard() {
        return new EmailIdempotencyGuard(60, 50_000);
    }

    private ClassRescheduledEvent newEvent(long classId, Instant rescheduledAt) {
        return new ClassRescheduledEvent(
                classId,
                "tenant-uuid",
                "Trung tâm Anh ngữ Sky Education",
                "Lớp Anh ngữ 5A1",
                LocalDate.of(2026, 5, 14),
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 7, 7),
                UUID.fromString("00000000-0000-0000-0000-000000000999"),
                rescheduledAt,
                "PHONG_HOC_KHONG_KHA_DUNG",
                null,
                List.of(10L, 11L, 12L),
                List.of(20L, 21L)
        );
    }

    @Test
    void handle_shouldForwardToEmailQueue_whenFeatureFlagEnabled() throws Exception {
        // Given
        ClassRescheduledEmailConsumer consumer = new ClassRescheduledEmailConsumer(
                objectMapper, rabbitTemplate, guard());

        ClassRescheduledEvent event = newEvent(12345L, Instant.parse("2026-05-14T10:30:00Z"));
        String payloadJson = objectMapper.writeValueAsString(event);

        // When
        consumer.handle(payloadJson);

        // Then — forwarded to email dispatch queue verbatim
        ArgumentCaptor<String> forwardCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(
                eq(ClassRescheduledEmailConsumer.EMAIL_DISPATCH_QUEUE),
                forwardCaptor.capture()
        );

        assertThat(forwardCaptor.getValue()).isEqualTo(payloadJson);
    }

    @Test
    void handle_shouldNotForward_whenPayloadMalformed() {
        // Given
        ClassRescheduledEmailConsumer consumer = new ClassRescheduledEmailConsumer(
                objectMapper, rabbitTemplate, guard());
        String malformed = "{ broken json }";

        // When
        consumer.handle(malformed);

        // Then — no forward attempt for malformed payload (avoids clogging DLQ)
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void handle_shouldSuppressDuplicateForward_onRedelivery() throws Exception {
        // GAP-840 — at-least-once inbound redelivery must NOT produce duplicate
        // outbound forward. Same logical ClassRescheduledEvent delivered twice →
        // exactly one convertAndSend call.
        EmailIdempotencyGuard sharedGuard = guard();
        ClassRescheduledEmailConsumer consumer = new ClassRescheduledEmailConsumer(
                objectMapper, rabbitTemplate, sharedGuard);

        ClassRescheduledEvent event = newEvent(99999L, Instant.parse("2026-05-14T11:00:00Z"));
        String payloadJson = objectMapper.writeValueAsString(event);

        // Simulate RabbitMQ at-least-once: same payload delivered twice.
        consumer.handle(payloadJson);
        consumer.handle(payloadJson);

        // Then — exactly ONE forward to the downstream email queue
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(ClassRescheduledEmailConsumer.EMAIL_DISPATCH_QUEUE),
                eq(payloadJson)
        );
    }

    @Test
    void handle_shouldNotSuppress_distinctEvents() throws Exception {
        // Distinct classId → distinct dedup key → both forwards proceed.
        EmailIdempotencyGuard sharedGuard = guard();
        ClassRescheduledEmailConsumer consumer = new ClassRescheduledEmailConsumer(
                objectMapper, rabbitTemplate, sharedGuard);

        String a = objectMapper.writeValueAsString(newEvent(1L, Instant.parse("2026-05-14T11:00:00Z")));
        String b = objectMapper.writeValueAsString(newEvent(2L, Instant.parse("2026-05-14T11:00:00Z")));

        consumer.handle(a);
        consumer.handle(b);

        // Then — both forwards proceeded
        verify(rabbitTemplate).convertAndSend(eq(ClassRescheduledEmailConsumer.EMAIL_DISPATCH_QUEUE), eq(a));
        verify(rabbitTemplate).convertAndSend(eq(ClassRescheduledEmailConsumer.EMAIL_DISPATCH_QUEUE), eq(b));
    }

    @Test
    void recipientListKey_shouldBeOrderInsensitive() {
        // Same set, different order → same key (dedup must catch shuffled-list redelivery)
        String k1 = ClassRescheduledEmailConsumer.recipientListKey(Arrays.asList(3L, 1L, 2L), Arrays.asList(20L, 10L));
        String k2 = ClassRescheduledEmailConsumer.recipientListKey(Arrays.asList(1L, 2L, 3L), Arrays.asList(10L, 20L));
        assertThat(k1).isEqualTo(k2);
    }

    @Test
    void recipientListKey_shouldHandleNullsAndEmpty() {
        assertThat(ClassRescheduledEmailConsumer.recipientListKey(null, null))
                .isEqualTo("parents=;students=");
        assertThat(ClassRescheduledEmailConsumer.recipientListKey(Collections.emptyList(), Collections.emptyList()))
                .isEqualTo("parents=;students=");
    }
}
