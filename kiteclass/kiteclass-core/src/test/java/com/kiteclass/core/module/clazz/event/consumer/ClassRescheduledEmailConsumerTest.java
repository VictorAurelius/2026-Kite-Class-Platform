package com.kiteclass.core.module.clazz.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kiteclass.core.module.clazz.event.ClassRescheduledEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit test for {@link ClassRescheduledEmailConsumer} (Wave beta-readiness-4 Bucket D — GAP-291).
 *
 * <p>Verifies feature-flag-enabled path: consumer forwards payload to
 * {@code class.rescheduled.email.queue} for kitehub-email to render + dispatch.
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

    @Test
    void handle_shouldForwardToEmailQueue_whenFeatureFlagEnabled() throws Exception {
        // Given
        ClassRescheduledEmailConsumer consumer = new ClassRescheduledEmailConsumer(objectMapper, rabbitTemplate);

        ClassRescheduledEvent event = new ClassRescheduledEvent(
                12345L,
                "tenant-uuid",
                "Trung tâm Anh ngữ Sky Education",
                "Lớp Anh ngữ 5A1",
                LocalDate.of(2026, 5, 14),
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 7, 7),
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000999"),
                Instant.now(),
                "PHONG_HOC_KHONG_KHA_DUNG",
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );
        String payloadJson = objectMapper.writeValueAsString(event);

        // When
        consumer.handle(payloadJson);

        // Then — forwarded to email dispatch queue verbatim
        ArgumentCaptor<String> forwardCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(
                eq(ClassRescheduledEmailConsumer.EMAIL_DISPATCH_QUEUE),
                forwardCaptor.capture()
        );

        // Forwarded payload is the original JSON (kitehub-email rehydrates + renders)
        org.assertj.core.api.Assertions.assertThat(forwardCaptor.getValue()).isEqualTo(payloadJson);
    }

    @Test
    void handle_shouldNotForward_whenPayloadMalformed() {
        // Given
        ClassRescheduledEmailConsumer consumer = new ClassRescheduledEmailConsumer(objectMapper, rabbitTemplate);
        String malformed = "{ broken json }";

        // When
        consumer.handle(malformed);

        // Then — no forward attempt for malformed payload (avoids clogging DLQ)
        verifyNoInteractions(rabbitTemplate);
    }
}
