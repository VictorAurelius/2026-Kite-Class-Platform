package com.kitehub.subscription.service.migration;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.outbox.SubscriptionOutboxEvent;
import com.kitehub.subscription.outbox.SubscriptionOutboxRepository;
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
@DisplayName("SubscriptionEventEmitter")
class SubscriptionEventEmitterTest {

    @Mock
    private SubscriptionOutboxRepository outboxRepository;

    private SubscriptionEventEmitter emitter;
    private Instance instance;

    @BeforeEach
    void setUp() {
        emitter = new SubscriptionEventEmitter(outboxRepository);
        instance = new Instance();
        instance.setId(UUID.randomUUID());
    }

    @Test
    void emit_with_instance_persists_outbox_row_with_event_fields() {
        emitter.emit(instance, "TRIAL_UPGRADE_INITIATED", "kite.migration.events", "{\"k\":\"v\"}");

        ArgumentCaptor<SubscriptionOutboxEvent> captor = ArgumentCaptor.forClass(SubscriptionOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        SubscriptionOutboxEvent saved = captor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getInstanceId()).isEqualTo(instance.getId());
        assertThat(saved.getEventType()).isEqualTo("TRIAL_UPGRADE_INITIATED");
        assertThat(saved.getTopic()).isEqualTo("kite.migration.events");
        assertThat(saved.getPayload()).isEqualTo("{\"k\":\"v\"}");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void emit_with_explicit_instance_id_persists_outbox_row() {
        UUID instanceId = UUID.randomUUID();
        emitter.emit(instanceId, "PURGE_REQUESTED", "kitehub.purge", "{\"id\":\"x\"}");

        ArgumentCaptor<SubscriptionOutboxEvent> captor = ArgumentCaptor.forClass(SubscriptionOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        SubscriptionOutboxEvent saved = captor.getValue();
        assertThat(saved.getInstanceId()).isEqualTo(instanceId);
        assertThat(saved.getEventType()).isEqualTo("PURGE_REQUESTED");
    }

    @Test
    void emit_with_null_instance_id_persists_outbox_row_for_orphan_email() {
        emitter.emit((UUID) null, "EMAIL_QUEUED", "kitehub.email", "{\"to\":\"x@y.com\"}");

        ArgumentCaptor<SubscriptionOutboxEvent> captor = ArgumentCaptor.forClass(SubscriptionOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        SubscriptionOutboxEvent saved = captor.getValue();
        assertThat(saved.getInstanceId()).isNull();
        assertThat(saved.getEventType()).isEqualTo("EMAIL_QUEUED");
        assertThat(saved.getPayload()).isEqualTo("{\"to\":\"x@y.com\"}");
    }

    @Test
    void escape_null_returns_empty_string() {
        assertThat(SubscriptionEventEmitter.escape(null)).isEmpty();
    }

    @Test
    void escape_backslash_and_quote() {
        assertThat(SubscriptionEventEmitter.escape("a\\b\"c")).isEqualTo("a\\\\b\\\"c");
    }

    @Test
    void escape_plain_string_unchanged() {
        assertThat(SubscriptionEventEmitter.escape("plain")).isEqualTo("plain");
    }
}
