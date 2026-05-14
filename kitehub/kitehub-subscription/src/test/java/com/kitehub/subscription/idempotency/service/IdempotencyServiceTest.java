package com.kitehub.subscription.idempotency.service;

import com.kitehub.subscription.idempotency.entity.IdempotencyKey;
import com.kitehub.subscription.idempotency.exception.IdempotencyConflictException;
import com.kitehub.subscription.idempotency.repository.IdempotencyKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link IdempotencyService} (GAP-536 Wave 77 Bucket D).
 */
@DisplayName("IdempotencyService")
class IdempotencyServiceTest {

    private IdempotencyKeyRepository repository;
    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        repository = mock(IdempotencyKeyRepository.class);
        service = new IdempotencyService(repository);
    }

    @Test
    @DisplayName("hashRequest — deterministic + identical for null/empty")
    void hashIsDeterministic() {
        String a = IdempotencyService.hashRequest("{\"slug\":\"hoa-mai\"}");
        String b = IdempotencyService.hashRequest("{\"slug\":\"hoa-mai\"}");
        assertThat(a).isEqualTo(b).hasSize(64);

        // Different body → different hash.
        String c = IdempotencyService.hashRequest("{\"slug\":\"hoa-mai-2\"}");
        assertThat(c).isNotEqualTo(a);

        // Null treated as empty string.
        assertThat(IdempotencyService.hashRequest(null))
                .isEqualTo(IdempotencyService.hashRequest(""));
    }

    @Test
    @DisplayName("findValidReplay — cache miss → empty")
    void cacheMiss() {
        when(repository.findByKeyAndEndpoint("k1", "POST_instances")).thenReturn(Optional.empty());
        assertThat(service.findValidReplay("k1", "POST_instances", "hash")).isEmpty();
    }

    @Test
    @DisplayName("findValidReplay — fresh row + matching hash → returns row")
    void cacheHitMatchingHash() {
        IdempotencyKey row = IdempotencyKey.builder()
                .key("k1").endpoint("POST_instances").requestHash("abc")
                .responseStatus(201).responseBody("{\"id\":\"1\"}")
                .createdAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build();
        when(repository.findByKeyAndEndpoint("k1", "POST_instances")).thenReturn(Optional.of(row));

        Optional<IdempotencyKey> result = service.findValidReplay("k1", "POST_instances", "abc");
        assertThat(result).isPresent();
        assertThat(result.get().getResponseStatus()).isEqualTo(201);
    }

    @Test
    @DisplayName("findValidReplay — expired row → empty (treat as cache miss)")
    void expiredTreatedAsMiss() {
        IdempotencyKey row = IdempotencyKey.builder()
                .key("k1").endpoint("POST_instances").requestHash("abc")
                .responseStatus(201).responseBody("{}")
                .createdAt(OffsetDateTime.now().minusDays(2))
                .expiresAt(OffsetDateTime.now().minusDays(1))
                .build();
        when(repository.findByKeyAndEndpoint("k1", "POST_instances")).thenReturn(Optional.of(row));

        assertThat(service.findValidReplay("k1", "POST_instances", "abc")).isEmpty();
    }

    @Test
    @DisplayName("findValidReplay — hash mismatch → 422 conflict")
    void hashMismatchThrows422() {
        IdempotencyKey row = IdempotencyKey.builder()
                .key("k1").endpoint("POST_instances").requestHash("abc")
                .responseStatus(201).responseBody("{}")
                .createdAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build();
        when(repository.findByKeyAndEndpoint("k1", "POST_instances")).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.findValidReplay("k1", "POST_instances", "different-hash"))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    @DisplayName("cacheResponse — writes row with TTL")
    void cachePersistsRow() {
        service.cacheResponse("k1", "POST_instances", "abc", 201, "{\"id\":\"1\"}");
        verify(repository).saveAndFlush(any(IdempotencyKey.class));
    }

    @Test
    @DisplayName("cacheResponse — race (unique-key collision) → swallowed, no throw")
    void cacheRaceIsBenign() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));
        assertThatNoException().isThrownBy(() ->
                service.cacheResponse("k1", "POST_instances", "abc", 201, "{}"));
    }
}
