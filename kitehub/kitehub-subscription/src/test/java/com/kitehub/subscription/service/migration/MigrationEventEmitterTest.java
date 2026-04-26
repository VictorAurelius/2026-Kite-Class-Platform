package com.kitehub.subscription.service.migration;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.outbox.MigrationOutboxEvent;
import com.kitehub.subscription.outbox.MigrationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MigrationEventEmitter")
class MigrationEventEmitterTest {

    @Mock
    private MigrationOutboxRepository outboxRepository;

    private MigrationEventEmitter emitter;
    private Instance instance;

    @BeforeEach
    void setUp() {
        emitter = new MigrationEventEmitter(outboxRepository);
        instance = new Instance();
        instance.setId(UUID.randomUUID());
    }

    @Test
    void emit_persists_outbox_row_with_event_fields() {
        emitter.emit(instance, "TRIAL_UPGRADE_INITIATED", "kite.migration.events", "{\"k\":\"v\"}");

        ArgumentCaptor<MigrationOutboxEvent> captor = ArgumentCaptor.forClass(MigrationOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        MigrationOutboxEvent saved = captor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getInstanceId()).isEqualTo(instance.getId());
        assertThat(saved.getEventType()).isEqualTo("TRIAL_UPGRADE_INITIATED");
        assertThat(saved.getTopic()).isEqualTo("kite.migration.events");
        assertThat(saved.getPayload()).isEqualTo("{\"k\":\"v\"}");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void escape_null_returns_empty_string() {
        assertThat(MigrationEventEmitter.escape(null)).isEmpty();
    }

    @Test
    void escape_backslash_and_quote() {
        assertThat(MigrationEventEmitter.escape("a\\b\"c")).isEqualTo("a\\\\b\\\"c");
    }

    @Test
    void escape_plain_string_unchanged() {
        assertThat(MigrationEventEmitter.escape("plain")).isEqualTo("plain");
    }
}
