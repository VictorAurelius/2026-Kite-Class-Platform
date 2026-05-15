package com.kitehub.subscription.auth.twofactor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RecoveryCodeService} (GAP-516 Wave 72b Bucket A).
 *
 * <p>Uses a hand-rolled in-memory implementation of {@link RecoveryCodeRepository}
 * so we don't need a Spring context; the service's behaviour around generation,
 * single-use consumption, and regeneration is fully testable in isolation.</p>
 */
@DisplayName("RecoveryCodeService — generate / verify / regenerate")
class RecoveryCodeServiceTest {

    private InMemoryRepo repo;
    private RecoveryCodeService svc;

    @BeforeEach
    void setUp() {
        repo = new InMemoryRepo();
        svc = new RecoveryCodeService(repo);
    }

    @Test
    @DisplayName("generate produces 10 unique 8-char codes from the safe alphabet")
    void generate_tenUniqueCodes() {
        List<RecoveryCodeService.GeneratedCode> codes = svc.generate();
        assertThat(codes).hasSize(RecoveryCodeService.CODES_PER_USER);
        Set<String> plain = codes.stream()
            .map(RecoveryCodeService.GeneratedCode::plain)
            .collect(Collectors.toSet());
        assertThat(plain).hasSize(RecoveryCodeService.CODES_PER_USER);
        for (String p : plain) {
            assertThat(p).hasSize(RecoveryCodeService.CODE_LENGTH);
            assertThat(p).doesNotContain("0", "o", "1", "l");
            assertThat(p).matches("[abcdefghijkmnpqrstuvwxyz23456789]+");
        }
    }

    @Test
    @DisplayName("issueForUser persists 10 codes")
    void issueForUser_persists() {
        UUID userId = UUID.randomUUID();
        List<String> plain = svc.issueForUser(userId);
        assertThat(plain).hasSize(10);
        assertThat(repo.findByUserIdAndUsedAtIsNullOrderByIdAsc(userId)).hasSize(10);
    }

    @Test
    @DisplayName("verifyAndConsume single-use: 2nd attempt on same code fails")
    void verifyAndConsume_singleUse() {
        UUID userId = UUID.randomUUID();
        List<String> plain = svc.issueForUser(userId);

        // First use succeeds.
        var first = svc.verifyAndConsume(userId, plain.get(0));
        assertThat(first.success()).isTrue();
        assertThat(first.codesRemaining()).isEqualTo(9);

        // Second use of the SAME code fails.
        var second = svc.verifyAndConsume(userId, plain.get(0));
        assertThat(second.success()).isFalse();
        assertThat(second.codesRemaining()).isEqualTo(9);
    }

    @Test
    @DisplayName("verifyAndConsume wrong code returns success=false + correct remaining count")
    void verifyAndConsume_wrongCode() {
        UUID userId = UUID.randomUUID();
        svc.issueForUser(userId);
        var result = svc.verifyAndConsume(userId, "wrongone");
        assertThat(result.success()).isFalse();
        assertThat(result.codesRemaining()).isEqualTo(10);
    }

    @Test
    @DisplayName("regenerate invalidates all previous codes + issues 10 new ones")
    void regenerate_replacesAll() {
        UUID userId = UUID.randomUUID();
        List<String> firstBatch = svc.issueForUser(userId);

        RecoveryCodeService.RegenerateResult r = svc.regenerate(userId);
        assertThat(r.invalidatedCount()).isEqualTo(10);
        assertThat(r.plainCodes()).hasSize(10);

        // First-batch codes no longer work.
        for (String old : firstBatch) {
            assertThat(svc.verifyAndConsume(userId, old).success()).isFalse();
        }

        // One new code works.
        assertThat(svc.verifyAndConsume(userId, r.plainCodes().get(0)).success()).isTrue();
    }

    @Test
    @DisplayName("verifyAndConsume after all 10 codes used returns false + remaining=0")
    void verifyAndConsume_exhaustion() {
        UUID userId = UUID.randomUUID();
        List<String> plain = svc.issueForUser(userId);
        for (String p : plain) {
            svc.verifyAndConsume(userId, p);
        }
        var result = svc.verifyAndConsume(userId, plain.get(0));
        assertThat(result.success()).isFalse();
        assertThat(result.codesRemaining()).isEqualTo(0);
    }

    // ---- in-memory repo --------------------------------------------------

    static class InMemoryRepo implements RecoveryCodeRepository {
        private final Map<Long, RecoveryCode> store = new HashMap<>();
        private final AtomicLong seq = new AtomicLong(1);

        @Override
        public List<RecoveryCode> findByUserIdAndUsedAtIsNullOrderByIdAsc(UUID userId) {
            return store.values().stream()
                .filter(rc -> rc.getUserId().equals(userId) && rc.getUsedAt() == null)
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .toList();
        }

        @Override
        public List<RecoveryCode> findByUserIdOrderByIdAsc(UUID userId) {
            return store.values().stream()
                .filter(rc -> rc.getUserId().equals(userId))
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .toList();
        }

        @Override
        public long countByUserIdAndUsedAtIsNull(UUID userId) {
            return findByUserIdAndUsedAtIsNullOrderByIdAsc(userId).size();
        }

        @Override
        public int markAllUsed(UUID userId, LocalDateTime now) {
            int n = 0;
            for (RecoveryCode rc : store.values()) {
                if (rc.getUserId().equals(userId) && rc.getUsedAt() == null) {
                    rc.setUsedAt(now);
                    n++;
                }
            }
            return n;
        }

        @Override
        public <S extends RecoveryCode> S save(S entity) {
            if (entity.getId() == null) entity.setId(seq.getAndIncrement());
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public <S extends RecoveryCode> List<S> saveAll(Iterable<S> entities) {
            List<S> out = new ArrayList<>();
            for (S e : entities) out.add(save(e));
            return out;
        }

        // ---- JpaRepository unused boilerplate ----
        @Override public void flush() { }
        @Override public <S extends RecoveryCode> S saveAndFlush(S e) { return save(e); }
        @Override public <S extends RecoveryCode> List<S> saveAllAndFlush(Iterable<S> e) { return saveAll(e); }
        @Override public void deleteAllInBatch(Iterable<RecoveryCode> e) { e.forEach(r -> store.remove(r.getId())); }
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { ids.forEach(store::remove); }
        @Override public void deleteAllInBatch() { store.clear(); }
        @Override public RecoveryCode getOne(Long id) { return store.get(id); }
        @Override public RecoveryCode getById(Long id) { return store.get(id); }
        @Override public RecoveryCode getReferenceById(Long id) { return store.get(id); }
        @Override public <S extends RecoveryCode> java.util.Optional<S> findOne(org.springframework.data.domain.Example<S> ex) { throw new UnsupportedOperationException(); }
        @Override public <S extends RecoveryCode> List<S> findAll(org.springframework.data.domain.Example<S> ex) { throw new UnsupportedOperationException(); }
        @Override public <S extends RecoveryCode> List<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Sort s) { throw new UnsupportedOperationException(); }
        @Override public <S extends RecoveryCode> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Pageable p) { throw new UnsupportedOperationException(); }
        @Override public <S extends RecoveryCode> long count(org.springframework.data.domain.Example<S> ex) { throw new UnsupportedOperationException(); }
        @Override public <S extends RecoveryCode> boolean exists(org.springframework.data.domain.Example<S> ex) { throw new UnsupportedOperationException(); }
        @Override public <S extends RecoveryCode, R> R findBy(org.springframework.data.domain.Example<S> ex, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> fn) { throw new UnsupportedOperationException(); }
        @Override public java.util.Optional<RecoveryCode> findById(Long id) { return java.util.Optional.ofNullable(store.get(id)); }
        @Override public boolean existsById(Long id) { return store.containsKey(id); }
        @Override public List<RecoveryCode> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<RecoveryCode> findAll(org.springframework.data.domain.Sort s) { return findAll(); }
        @Override public org.springframework.data.domain.Page<RecoveryCode> findAll(org.springframework.data.domain.Pageable p) { throw new UnsupportedOperationException(); }
        @Override public List<RecoveryCode> findAllById(Iterable<Long> ids) { List<RecoveryCode> r = new ArrayList<>(); ids.forEach(i -> { RecoveryCode rc = store.get(i); if (rc != null) r.add(rc); }); return r; }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long id) { store.remove(id); }
        @Override public void delete(RecoveryCode rc) { store.remove(rc.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(store::remove); }
        @Override public void deleteAll(Iterable<? extends RecoveryCode> e) { e.forEach(r -> store.remove(r.getId())); }
        @Override public void deleteAll() { store.clear(); }
    }
}
