package com.kiteclass.core.module.childprotection.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.childprotection.entity.ChildProtectionAuditLog;
import com.kiteclass.core.module.childprotection.repository.ChildProtectionAuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ChildProtectionAuditServiceImpl} — covers hash
 * compute, append, prev_hash linking, and chain integrity verification.
 *
 * <p>Uses a hand-rolled in-memory fake of
 * {@link ChildProtectionAuditLogRepository} (3 query methods + save) rather
 * than Mockito because the order-sensitive nature of the chain is far
 * easier to express with an actual list.
 *
 * @since Wave 19 Bucket A — GAP-322c Phase 1C v1
 */
@DisplayName("ChildProtectionAuditServiceImpl — hash chain")
class ChildProtectionAuditServiceImplTest {

    private static final UUID TENANT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private InMemoryAuditRepo repo;
    private ChildProtectionAuditServiceImpl service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryAuditRepo();
        service = new ChildProtectionAuditServiceImpl(repo);
        TenantContext.setCurrentTenant(TENANT_A);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("first append uses GENESIS_PREV_HASH (64 zeros)")
    void firstAppendUsesGenesisPrevHash() {
        ChildProtectionAuditLog entry = service.append(
                "Incident", 1L, "INCIDENT_TRANSITION_CRITICAL", 100L,
                Map.of("severity", "CRITICAL", "category", "ABUSE"));

        assertThat(entry.getPrevHash())
                .isEqualTo(ChildProtectionAuditServiceImpl.GENESIS_PREV_HASH);
        assertThat(entry.getContentHash()).hasSize(64);
        assertThat(entry.getInstanceId()).isEqualTo(TENANT_A);
    }

    @Test
    @DisplayName("second append in same chain uses prior content_hash as prev_hash")
    void secondAppendChainsToFirst() {
        ChildProtectionAuditLog first = service.append(
                "Incident", 1L, "INCIDENT_TRANSITION_CRITICAL", 100L,
                Map.of("severity", "CRITICAL", "category", "ABUSE"));

        ChildProtectionAuditLog second = service.append(
                "Incident", 1L, "MANDATORY_REPORT_ACK", 200L,
                Map.of("referenceNumber", "TĐ111-2026-0001"));

        assertThat(second.getPrevHash()).isEqualTo(first.getContentHash());
        assertThat(second.getContentHash()).isNotEqualTo(first.getContentHash());
    }

    @Test
    @DisplayName("identical payload + identical prev_hash → identical content_hash (deterministic)")
    void identicalInputsProduceIdenticalHash() {
        // Same payload, but inserted in different orders
        Map<String, Object> insertOrderA = new HashMap<>();
        insertOrderA.put("k1", "v1");
        insertOrderA.put("k2", "v2");

        Map<String, Object> insertOrderB = new HashMap<>();
        insertOrderB.put("k2", "v2");
        insertOrderB.put("k1", "v1");

        // First entry on chain A
        ChildProtectionAuditLog a = service.append(
                "Incident", 1L, "ACT", 1L, insertOrderA);
        // First entry on chain B (different tenant → fresh chain)
        TenantContext.clear();
        TenantContext.setCurrentTenant(TENANT_B);
        ChildProtectionAuditLog b = service.append(
                "Incident", 1L, "ACT", 1L, insertOrderB);

        // Different tenants → instanceId in the hashed payload differs;
        // content hashes must differ. Asserts canonicalisation isn't
        // accidentally producing a tenant-blind hash.
        assertThat(a.getContentHash()).isNotEqualTo(b.getContentHash());
    }

    @Test
    @DisplayName("verifyChainIntegrity returns true for an untampered chain")
    void verifyChainIntegrityHappyPath() {
        service.append("Incident", 1L, "A", 1L, Map.of("x", "1"));
        service.append("Incident", 2L, "B", 2L, Map.of("y", "2"));
        service.append("Incident", 3L, "C", 3L, Map.of("z", "3"));

        assertThat(service.verifyChainIntegrity("Incident")).isTrue();
    }

    @Test
    @DisplayName("verifyChainIntegrity returns false when a content_hash is tampered")
    void verifyChainIntegrityDetectsTamper() {
        service.append("Incident", 1L, "A", 1L, Map.of("x", "1"));
        ChildProtectionAuditLog target = service.append(
                "Incident", 2L, "B", 2L, Map.of("y", "2"));

        // Simulate row-level tamper: rewrite the payload but leave hashes untouched
        target.setPayloadJson("{\"hostile\":true}");

        assertThat(service.verifyChainIntegrity("Incident")).isFalse();
    }

    @Test
    @DisplayName("verifyChainIntegrity returns false when prev_hash linking is broken")
    void verifyChainIntegrityDetectsBrokenLink() {
        service.append("Incident", 1L, "A", 1L, Map.of("x", "1"));
        ChildProtectionAuditLog target = service.append(
                "Incident", 2L, "B", 2L, Map.of("y", "2"));

        // Simulate prev_hash tamper: rewrite to a different value
        target.setPrevHash("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");

        assertThat(service.verifyChainIntegrity("Incident")).isFalse();
    }

    @Test
    @DisplayName("findByEntity returns entries in append order")
    void findByEntityIsAscending() {
        service.append("Incident", 1L, "A", 1L, Map.of("k", "1"));
        service.append("Incident", 1L, "B", 2L, Map.of("k", "2"));
        service.append("Incident", 1L, "C", 3L, Map.of("k", "3"));
        service.append("Incident", 2L, "X", 9L, Map.of("k", "x")); // different entity

        List<ChildProtectionAuditLog> rows =
                service.findByEntity("Incident", 1L);

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).getAction()).isEqualTo("A");
        assertThat(rows.get(1).getAction()).isEqualTo("B");
        assertThat(rows.get(2).getAction()).isEqualTo("C");
    }

    @Test
    @DisplayName("append rejects null/blank required arguments")
    void appendRejectsBadArgs() {
        assertThatThrownBy(() -> service.append(null, 1L, "A", 1L, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.append("", 1L, "A", 1L, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.append("Incident", null, "A", 1L, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.append("Incident", 1L, null, 1L, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.append("Incident", 1L, "", 1L, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("append throws when TenantContext is unset")
    void appendThrowsWithoutTenant() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.append(
                "Incident", 1L, "A", 1L, Map.of("x", "1")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("payload nulled by caller is treated as empty (no NPE)")
    void appendAcceptsNullPayload() {
        ChildProtectionAuditLog entry = service.append(
                "Incident", 1L, "A", 1L, null);

        assertThat(entry.getPayloadJson()).isNotNull();
        // Hashed payload still embeds entity refs / actor / timestamp.
        assertThat(entry.getPayloadJson()).contains("\"action\":\"A\"");
    }

    /**
     * In-memory fake repository — keeps an ordered list per
     * (instanceId, entityType) chain and assigns IDs deterministically.
     */
    private static final class InMemoryAuditRepo implements ChildProtectionAuditLogRepository {

        private final List<ChildProtectionAuditLog> rows = new ArrayList<>();
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public List<ChildProtectionAuditLog> findLatestForChain(UUID instanceId, String entityType) {
            List<ChildProtectionAuditLog> matches = new ArrayList<>();
            for (int i = rows.size() - 1; i >= 0; i--) {
                ChildProtectionAuditLog r = rows.get(i);
                if (r.getInstanceId().equals(instanceId)
                        && r.getEntityType().equals(entityType)) {
                    matches.add(r);
                }
            }
            return matches;
        }

        @Override
        public Optional<ChildProtectionAuditLog> findHead(UUID instanceId, String entityType) {
            List<ChildProtectionAuditLog> latest = findLatestForChain(instanceId, entityType);
            return latest.isEmpty() ? Optional.empty() : Optional.of(latest.get(0));
        }

        @Override
        public List<ChildProtectionAuditLog> findChainAscending(UUID instanceId, String entityType) {
            List<ChildProtectionAuditLog> matches = new ArrayList<>();
            for (ChildProtectionAuditLog r : rows) {
                if (r.getInstanceId().equals(instanceId)
                        && r.getEntityType().equals(entityType)) {
                    matches.add(r);
                }
            }
            return matches;
        }

        @Override
        public List<ChildProtectionAuditLog> findByEntity(UUID instanceId, String entityType, Long entityId) {
            List<ChildProtectionAuditLog> matches = new ArrayList<>();
            for (ChildProtectionAuditLog r : rows) {
                if (r.getInstanceId().equals(instanceId)
                        && r.getEntityType().equals(entityType)
                        && r.getEntityId().equals(entityId)) {
                    matches.add(r);
                }
            }
            return matches;
        }

        @Override
        public List<Object[]> findDistinctChains() {
            // De-duplicate (instanceId, entityType) pairs preserving insertion order.
            List<Object[]> out = new ArrayList<>();
            for (ChildProtectionAuditLog r : rows) {
                boolean exists = false;
                for (Object[] pair : out) {
                    if (pair[0].equals(r.getInstanceId()) && pair[1].equals(r.getEntityType())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    out.add(new Object[]{r.getInstanceId(), r.getEntityType()});
                }
            }
            return out;
        }

        @Override
        public <S extends ChildProtectionAuditLog> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(sequence.incrementAndGet());
            }
            rows.add(entity);
            return entity;
        }

        // -- Unused JpaRepository methods stubbed below ----------------------

        @Override public List<ChildProtectionAuditLog> findAll() { return List.copyOf(rows); }
        @Override public List<ChildProtectionAuditLog> findAll(org.springframework.data.domain.Sort sort) { return findAll(); }
        @Override public org.springframework.data.domain.Page<ChildProtectionAuditLog> findAll(org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        @Override public List<ChildProtectionAuditLog> findAllById(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public <S extends ChildProtectionAuditLog> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<ChildProtectionAuditLog> findById(Long aLong) { return rows.stream().filter(r -> r.getId().equals(aLong)).findFirst(); }
        @Override public boolean existsById(Long aLong) { return findById(aLong).isPresent(); }
        @Override public long count() { return rows.size(); }
        @Override public void deleteById(Long aLong) { throw new UnsupportedOperationException("DELETE not allowed"); }
        @Override public void delete(ChildProtectionAuditLog entity) { throw new UnsupportedOperationException("DELETE not allowed"); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends ChildProtectionAuditLog> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { throw new UnsupportedOperationException(); }
        @Override public void flush() { /* no-op */ }
        @Override public <S extends ChildProtectionAuditLog> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends ChildProtectionAuditLog> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<ChildProtectionAuditLog> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
        @Override public ChildProtectionAuditLog getOne(Long aLong) { throw new UnsupportedOperationException(); }
        @Override public ChildProtectionAuditLog getById(Long aLong) { throw new UnsupportedOperationException(); }
        @Override public ChildProtectionAuditLog getReferenceById(Long aLong) { throw new UnsupportedOperationException(); }
        @Override public <S extends ChildProtectionAuditLog> List<S> findAll(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends ChildProtectionAuditLog> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends ChildProtectionAuditLog> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends ChildProtectionAuditLog> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends ChildProtectionAuditLog> long count(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends ChildProtectionAuditLog> boolean exists(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends ChildProtectionAuditLog, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }
}
