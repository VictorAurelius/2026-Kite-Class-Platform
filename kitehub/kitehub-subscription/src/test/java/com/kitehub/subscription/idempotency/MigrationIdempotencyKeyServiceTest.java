package com.kitehub.subscription.idempotency;

import com.kitehub.platform.domain.enums.MigrationPhase;
import com.kitehub.subscription.dto.UpgradeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MigrationIdempotencyKeyService} (GAP-192 Phase 4b-i).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MigrationIdempotencyKeyService")
class MigrationIdempotencyKeyServiceTest {

    @Mock
    private MigrationIdempotencyKeyRepository repository;

    private MigrationIdempotencyKeyService service;

    private UUID instanceId;
    private String key;

    @BeforeEach
    void setUp() {
        service = new MigrationIdempotencyKeyService(repository);
        ReflectionTestUtils.setField(service, "ttlMinutes", 10);
        instanceId = UUID.randomUUID();
        key = UUID.randomUUID().toString();
    }

    private UpgradeResponse sampleResponse() {
        return UpgradeResponse.builder()
            .instanceId(instanceId)
            .migrationPhase(MigrationPhase.PAYMENT_PENDING)
            .startedAt(LocalDateTime.now())
            .estimatedCompletionSeconds(5)
            .pollUrl("/api/platform/instances/" + instanceId + "/trial-status")
            .build();
    }

    @Test
    @DisplayName("findExisting returns empty when key absent")
    void findExistingAbsent() {
        when(repository.findByIdempotencyKeyAndInstanceId(key, instanceId))
            .thenReturn(Optional.empty());

        assertThat(service.findExisting(key, instanceId)).isEmpty();
    }

    @Test
    @DisplayName("findExisting returns cached response when key is fresh")
    void findExistingFresh() {
        MigrationIdempotencyKey stored = MigrationIdempotencyKey.builder()
            .id(UUID.randomUUID())
            .idempotencyKey(key)
            .instanceId(instanceId)
            .responsePhase(MigrationPhase.PAYMENT_PENDING)
            .responseStartedAt(LocalDateTime.now().minusSeconds(3))
            .responsePollUrl("/api/platform/instances/" + instanceId + "/trial-status")
            .responseEstimatedCompletionSeconds(5)
            .createdAt(LocalDateTime.now().minusSeconds(3))
            .expiresAt(LocalDateTime.now().plusMinutes(9))
            .build();
        when(repository.findByIdempotencyKeyAndInstanceId(key, instanceId))
            .thenReturn(Optional.of(stored));

        Optional<UpgradeResponse> result = service.findExisting(key, instanceId);

        assertThat(result).isPresent();
        assertThat(result.get().getMigrationPhase()).isEqualTo(MigrationPhase.PAYMENT_PENDING);
        assertThat(result.get().getInstanceId()).isEqualTo(instanceId);
    }

    @Test
    @DisplayName("findExisting returns empty when key expired")
    void findExistingExpired() {
        MigrationIdempotencyKey stale = MigrationIdempotencyKey.builder()
            .id(UUID.randomUUID())
            .idempotencyKey(key)
            .instanceId(instanceId)
            .responsePhase(MigrationPhase.PAYMENT_PENDING)
            .responseStartedAt(LocalDateTime.now().minusMinutes(15))
            .responsePollUrl("/poll")
            .responseEstimatedCompletionSeconds(5)
            .createdAt(LocalDateTime.now().minusMinutes(15))
            .expiresAt(LocalDateTime.now().minusMinutes(5))
            .build();
        when(repository.findByIdempotencyKeyAndInstanceId(key, instanceId))
            .thenReturn(Optional.of(stale));

        assertThat(service.findExisting(key, instanceId)).isEmpty();
    }

    @Test
    @DisplayName("findExisting short-circuits on null/blank key")
    void findExistingNullKey() {
        assertThat(service.findExisting(null, instanceId)).isEmpty();
        assertThat(service.findExisting("   ", instanceId)).isEmpty();
        verify(repository, never()).findByIdempotencyKeyAndInstanceId(any(), any());
    }

    @Test
    @DisplayName("persist stores new row with TTL = ttlMinutes")
    void persistNew() {
        when(repository.findByIdempotencyKeyAndInstanceId(key, instanceId))
            .thenReturn(Optional.empty());

        service.persist(key, sampleResponse());

        ArgumentCaptor<MigrationIdempotencyKey> cap = ArgumentCaptor.forClass(MigrationIdempotencyKey.class);
        verify(repository).save(cap.capture());
        MigrationIdempotencyKey saved = cap.getValue();
        assertThat(saved.getIdempotencyKey()).isEqualTo(key);
        assertThat(saved.getInstanceId()).isEqualTo(instanceId);
        // expiresAt should be roughly 10 minutes in the future
        long ttlSeconds = java.time.Duration.between(saved.getCreatedAt(), saved.getExpiresAt()).toSeconds();
        assertThat(ttlSeconds).isBetween(595L, 605L);
    }

    @Test
    @DisplayName("persist is no-op when row already exists (concurrent race)")
    void persistDuplicateNoop() {
        MigrationIdempotencyKey existing = MigrationIdempotencyKey.builder()
            .id(UUID.randomUUID())
            .idempotencyKey(key)
            .instanceId(instanceId)
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .createdAt(LocalDateTime.now())
            .responsePhase(MigrationPhase.PAYMENT_PENDING)
            .responseStartedAt(LocalDateTime.now())
            .responsePollUrl("/poll")
            .responseEstimatedCompletionSeconds(5)
            .build();
        when(repository.findByIdempotencyKeyAndInstanceId(key, instanceId))
            .thenReturn(Optional.of(existing));

        service.persist(key, sampleResponse());

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("persist swallows DataIntegrityViolationException from a concurrent insert (GAP-1271)")
    void persistConcurrentInsertSwallowed() {
        // Existence check passes (no row yet) but a concurrent winner inserts the same
        // (key, instanceId) before our save → UNIQUE violation. persist must NOT propagate
        // it (REQUIRES_NEW isolates the rollback; caller still returns its 202 envelope).
        when(repository.findByIdempotencyKeyAndInstanceId(key, instanceId))
            .thenReturn(Optional.empty());
        when(repository.save(any(MigrationIdempotencyKey.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate key (idempotency_key, instance_id)"));

        assertThatCode(() -> service.persist(key, sampleResponse())).doesNotThrowAnyException();

        verify(repository).save(any(MigrationIdempotencyKey.class));
    }

    @Test
    @DisplayName("persist is no-op on null/blank key")
    void persistNullKey() {
        service.persist(null, sampleResponse());
        service.persist("", sampleResponse());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("purgeExpired delegates to repository and returns row count")
    void purgeExpired() {
        when(repository.deleteExpired(any())).thenReturn(7);
        assertThat(service.purgeExpired()).isEqualTo(7);
        verify(repository).deleteExpired(any(LocalDateTime.class));
    }
}
