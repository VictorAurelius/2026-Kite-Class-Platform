package com.kitehub.branding.lifecycle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.lifecycle.entity.BrandingInstanceState;
import com.kitehub.branding.lifecycle.entity.BrandingLifecycleEvent;
import com.kitehub.branding.lifecycle.repository.BrandingInstanceStateRepository;
import com.kitehub.branding.lifecycle.repository.BrandingLifecycleEventRepository;
import com.kitehub.branding.outbox.BrandingEventEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InstanceLifecycleServiceTest {

    @Mock
    private BrandingInstanceStateRepository stateRepo;

    @Mock
    private BrandingLifecycleEventRepository eventRepo;

    @Mock
    private BrandingEventEmitter outboxEmitter;

    private InstanceLifecycleService service;
    private UUID instanceId;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        service = new InstanceLifecycleService(stateRepo, eventRepo, outboxEmitter, mapper);
        instanceId = UUID.randomUUID();
        when(stateRepo.save(any(BrandingInstanceState.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepo.save(any(BrandingLifecycleEvent.class))).thenAnswer(inv -> {
            BrandingLifecycleEvent e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(UUID.randomUUID());
            }
            return e;
        });
    }

    @Test
    void firstTransitionToInitializingSucceedsAndPublishesEvent() {
        when(stateRepo.findById(instanceId)).thenReturn(Optional.empty());

        BrandingInstanceState result = service.transition(
            instanceId, LifecycleState.INITIALIZING,
            InstanceLifecycleService.Actor.system("test"), Map.of());

        assertThat(result.getState()).isEqualTo(LifecycleState.INITIALIZING);
        verify(eventRepo).save(any(BrandingLifecycleEvent.class));
        verify(outboxEmitter).emit(
            eq(instanceId), eq(instanceId), eq("branding.lifecycle.transition"),
            anyString(), anyString(), any());
    }

    @Test
    void invalidTransitionThrowsIllegalState() {
        BrandingInstanceState current = BrandingInstanceState.builder()
            .instanceId(instanceId).state(LifecycleState.NOT_STARTED)
            .brandingVersion(0).regenerateCount(0).build();
        when(stateRepo.findById(instanceId)).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.transition(
            instanceId, LifecycleState.DEPLOYED,
            InstanceLifecycleService.Actor.system("test"), Map.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("INVALID_TRANSITION");

        verify(eventRepo, never()).save(any());
        verify(outboxEmitter, never()).emit(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void deployedToRegeneratingIncrementsRegenerateCount() {
        BrandingInstanceState current = BrandingInstanceState.builder()
            .instanceId(instanceId).state(LifecycleState.DEPLOYED)
            .brandingVersion(1).regenerateCount(0).build();
        when(stateRepo.findById(instanceId)).thenReturn(Optional.of(current));

        BrandingInstanceState result = service.transition(
            instanceId, LifecycleState.REGENERATING,
            InstanceLifecycleService.Actor.user("u1"), Map.of());

        assertThat(result.getState()).isEqualTo(LifecycleState.REGENERATING);
        assertThat(result.getRegenerateCount()).isEqualTo(1);
    }

    @Test
    void regeneratingToDeployedBumpsBrandingVersion() {
        BrandingInstanceState current = BrandingInstanceState.builder()
            .instanceId(instanceId).state(LifecycleState.REGENERATING)
            .brandingVersion(1).regenerateCount(1).build();
        when(stateRepo.findById(instanceId)).thenReturn(Optional.of(current));

        BrandingInstanceState result = service.transition(
            instanceId, LifecycleState.DEPLOYED,
            InstanceLifecycleService.Actor.system("test"), Map.of());

        assertThat(result.getBrandingVersion()).isEqualTo(2);
    }

    @Test
    void failedTransitionCapturesReason() {
        BrandingInstanceState current = BrandingInstanceState.builder()
            .instanceId(instanceId).state(LifecycleState.GENERATING)
            .brandingVersion(0).regenerateCount(0).build();
        when(stateRepo.findById(instanceId)).thenReturn(Optional.of(current));

        BrandingInstanceState result = service.transition(
            instanceId, LifecycleState.FAILED,
            InstanceLifecycleService.Actor.system("test"),
            Map.of("reason", "AI provider down"));

        assertThat(result.getState()).isEqualTo(LifecycleState.FAILED);
        assertThat(result.getLastFailureReason()).isEqualTo("AI provider down");
    }

    @Test
    void failedToInitializingRetryPath() {
        BrandingInstanceState current = BrandingInstanceState.builder()
            .instanceId(instanceId).state(LifecycleState.FAILED)
            .brandingVersion(0).regenerateCount(0).build();
        when(stateRepo.findById(instanceId)).thenReturn(Optional.of(current));

        BrandingInstanceState result = service.transition(
            instanceId, LifecycleState.INITIALIZING,
            InstanceLifecycleService.Actor.system("retry"), Map.of());

        assertThat(result.getState()).isEqualTo(LifecycleState.INITIALIZING);
    }

    @Test
    void recordMarkerAppendsEventWithoutChangingState() {
        BrandingInstanceState current = BrandingInstanceState.builder()
            .instanceId(instanceId).state(LifecycleState.GENERATING)
            .brandingVersion(0).regenerateCount(0).build();
        when(stateRepo.findById(instanceId)).thenReturn(Optional.of(current));

        BrandingLifecycleEvent ev = service.recordMarker(
            instanceId, "quality-score-computed",
            InstanceLifecycleService.Actor.system("quality-gate"),
            Map.of("score", 92));

        assertThat(ev.getEventType()).isEqualTo("quality-score-computed");
        assertThat(ev.getFromState()).isEqualTo(LifecycleState.GENERATING);
        assertThat(ev.getToState()).isEqualTo(LifecycleState.GENERATING);
        verify(stateRepo, never()).save(any());
        verify(outboxEmitter, never()).emit(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void recordMarkerIsolatedInRequiresNewTransaction() throws Exception {
        // GAP-1107 #1 / audit-service-isolation.md §3.11: a marker is a best-effort
        // audit side-effect — it MUST run in its own physical transaction so a failed
        // marker INSERT cannot poison a calling transaction (UnexpectedRollbackException).
        java.lang.reflect.Method method = InstanceLifecycleService.class
            .getMethod("recordMarker", UUID.class, String.class,
                InstanceLifecycleService.Actor.class, Map.class);
        org.springframework.transaction.annotation.Transactional tx =
            method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);

        assertThat(tx).isNotNull();
        assertThat(tx.propagation())
            .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
    }

    @Test
    void transitionRecordsFromAndToStatesInEvent() {
        BrandingInstanceState current = BrandingInstanceState.builder()
            .instanceId(instanceId).state(LifecycleState.INITIALIZING)
            .brandingVersion(0).regenerateCount(0).build();
        when(stateRepo.findById(instanceId)).thenReturn(Optional.of(current));

        ArgumentCaptor<BrandingLifecycleEvent> captor =
            ArgumentCaptor.forClass(BrandingLifecycleEvent.class);

        service.transition(instanceId, LifecycleState.GENERATING,
            InstanceLifecycleService.Actor.system("test"), Map.of());

        verify(eventRepo, times(1)).save(captor.capture());
        BrandingLifecycleEvent e = captor.getValue();
        assertThat(e.getFromState()).isEqualTo(LifecycleState.INITIALIZING);
        assertThat(e.getToState()).isEqualTo(LifecycleState.GENERATING);
        assertThat(e.getActorKind()).isEqualTo("system");
    }
}
