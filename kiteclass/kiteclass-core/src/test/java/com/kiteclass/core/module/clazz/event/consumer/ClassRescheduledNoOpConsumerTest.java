package com.kiteclass.core.module.clazz.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kiteclass.core.module.clazz.event.ClassRescheduledEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit test for {@link ClassRescheduledNoOpConsumer} (Wave beta-readiness-4 Bucket D — GAP-291).
 *
 * <p>Verifies feature-flag-disabled path: consumer accepts payload, deserializes, logs only,
 * triggers no user-visible side effect.
 *
 * <p>Bean activation (`@ConditionalOnProperty` with `havingValue="false"` + `matchIfMissing=true`)
 * verified by Spring Boot AutoConfiguration — not in scope of this unit test.
 *
 * @author KiteClass Team
 * @since Wave beta-readiness-4 Bucket D (GAP-291)
 */
class ClassRescheduledNoOpConsumerTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();
    private final ClassRescheduledNoOpConsumer consumer = new ClassRescheduledNoOpConsumer(objectMapper);

    @Test
    void handle_shouldLogOnly_whenFeatureFlagDefaultsToDisabled() throws Exception {
        // Given — typical Outbox payload from kiteclass-core
        ClassRescheduledEvent event = new ClassRescheduledEvent(
                12345L,
                "tenant-uuid",
                "Trung tâm Anh ngữ Sky Education",
                "Lớp Anh ngữ 5A1",
                LocalDate.of(2026, 5, 14),
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 7, 7),
                999L,
                Instant.now(),
                "GV_OM_BAN_DOT_XUAT",
                "Cô giáo Trần Thị Hồng xin nghỉ ốm 1 tuần.",
                Collections.emptyList(),
                Collections.emptyList()
        );
        String payloadJson = objectMapper.writeValueAsString(event);

        // When + Then — no exception, no side effect (logs only)
        assertThatCode(() -> consumer.handle(payloadJson)).doesNotThrowAnyException();
    }

    @Test
    void handle_shouldSwallowMalformedPayload_withoutThrowing() {
        // Given — broken JSON
        String malformed = "{ this is not json }";

        // When + Then — consumer logs warn and returns; never throws
        assertThatCode(() -> consumer.handle(malformed)).doesNotThrowAnyException();
    }
}
