package com.kitehub.subscription.repository;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link InstanceRepository#findByOwnerIdAndDeletedFalse} deterministic
 * ordering on real PostgreSQL (GAP-1306).
 *
 * <p>Regression guard for the JWT {@code tenantId} non-determinism class. Before GAP-1306 the
 * derived query returned a {@code List<Instance>} with no defined order, so Postgres heap/index
 * scan order decided which instance {@code .stream().findFirst()} picked for an owner with &gt;1
 * non-deleted instance — making the minted {@code tenantId} / {@code tier} JWT claim
 * non-deterministic (cross-tenant exposure risk). The fix adds an explicit
 * {@code ORDER BY i.createdAt ASC, i.id ASC} to the repository query so the OLDEST non-deleted
 * instance always wins, deterministically.</p>
 *
 * <p>A mock-based test (e.g. {@code AuthServiceJwtTenantIdClaimTest}) CANNOT prove ordering —
 * it stubs the repository to return a fixed list. This {@code @DataJpaTest} exercises the actual
 * JPQL {@code ORDER BY} against a real Postgres container, which is the only meaningful proof of
 * determinism. Per {@code .claude/rules/postgres-specific-type-testcontainers.md} +
 * {@code .claude/rules/pre-handoff-self-test-completeness.md} §3.</p>
 *
 * <p>{@code created_at} is populated by JPA auditing ({@code @CreatedDate}) on persist, so the
 * test persists first, then issues a native UPDATE to assign distinct, controlled timestamps in
 * an order that does NOT match insertion order — that way a query without the ORDER BY (the old
 * bug) would surface insertion/heap order, while the fixed query must surface createdAt order.</p>
 *
 * @since GAP-1306
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("InstanceRepository.findByOwnerIdAndDeletedFalse — deterministic ORDER BY (GAP-1306)")
class InstanceOwnerOrderingPostgresTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("kitehub_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private InstanceRepository instanceRepository;

    @PersistenceContext
    private EntityManager em;

    @BeforeEach
    void clean() {
        instanceRepository.deleteAll();
        instanceRepository.flush();
    }

    @Test
    @DisplayName("owner with ≥2 non-deleted instances → oldest createdAt wins, deletes + other owners excluded")
    void multiInstanceOwner_oldestCreatedAtWinsDeterministically() {
        UUID owner = UUID.randomUUID();
        UUID otherOwner = UUID.randomUUID();

        // Insertion order ≠ intended createdAt order — proves the ORDER BY, not heap order.
        UUID firstInserted = persist(owner, "ord-a", false).getId();
        UUID secondInserted = persist(owner, "ord-b", false).getId();
        UUID thirdInserted = persist(owner, "ord-c", false).getId();
        UUID deletedForOwner = persist(owner, "ord-del", true).getId();
        UUID otherOwnersInstance = persist(otherOwner, "ord-other", false).getId();

        LocalDateTime base = LocalDateTime.now();
        // thirdInserted is the OLDEST → must come first under ORDER BY createdAt ASC even though
        // it was inserted last (so a missing ORDER BY would have placed firstInserted first).
        setCreatedAt(thirdInserted, base.minusDays(3));   // oldest
        setCreatedAt(firstInserted, base.minusDays(2));   // middle
        setCreatedAt(secondInserted, base.minusDays(1));  // newest
        setCreatedAt(deletedForOwner, base.minusDays(10)); // even older, but deleted → excluded
        setCreatedAt(otherOwnersInstance, base.minusDays(20)); // even older, other owner → excluded
        em.flush();
        em.clear();

        List<Instance> result = instanceRepository.findByOwnerIdAndDeletedFalse(owner);

        assertThat(result)
                .as("deleted + other-owner instances must be excluded")
                .hasSize(3);
        assertThat(result.stream().map(Instance::getId).toList())
                .as("ordered by createdAt ASC → [oldest, middle, newest]")
                .containsExactly(thirdInserted, firstInserted, secondInserted);
        assertThat(result.get(0).getId())
                .as("oldest non-deleted instance wins (deterministic primary)")
                .isEqualTo(thirdInserted);
    }

    @Test
    @DisplayName("repeated calls return identical ordering → stable across N mints (AC#2)")
    void orderingStableAcrossRepeatedCalls() {
        UUID owner = UUID.randomUUID();

        UUID a = persist(owner, "stab-a", false).getId();
        UUID b = persist(owner, "stab-b", false).getId();
        UUID c = persist(owner, "stab-c", false).getId();

        LocalDateTime base = LocalDateTime.now();
        setCreatedAt(c, base.minusHours(3)); // oldest
        setCreatedAt(a, base.minusHours(2));
        setCreatedAt(b, base.minusHours(1)); // newest
        em.flush();
        em.clear();

        List<UUID> expected = List.of(c, a, b);
        // Mint resolution happens many times (every login + SSO exchange). The picked instance
        // (result.get(0)) must be identical on every call — this is the determinism AC#2 proves.
        for (int i = 0; i < 5; i++) {
            em.clear();
            List<UUID> ids = instanceRepository.findByOwnerIdAndDeletedFalse(owner)
                    .stream().map(Instance::getId).toList();
            assertThat(ids)
                    .as("call #%d must reproduce the same deterministic order", i)
                    .containsExactlyElementsOf(expected);
        }
    }

    private Instance persist(UUID ownerId, String key, boolean deleted) {
        Instance instance = new Instance();
        instance.setSubdomain(key);
        instance.setOrganizationName("Ordering Org " + key);
        instance.setSlug("ordering-org-" + key);
        instance.setOwnerId(ownerId);
        instance.setTier(PricingTier.FREE);
        instance.setStatus(InstanceStatus.TRIAL);
        instance.setDatabaseUrl("pending");
        instance.setDatabaseUsername("pending");
        instance.setDatabasePassword("pending");
        instance.setDeleted(deleted);
        Instance saved = instanceRepository.saveAndFlush(instance);
        return saved;
    }

    /**
     * Override the auditing-populated {@code created_at} via native SQL. {@code created_at} is
     * {@code updatable = false} at the JPA mapping level, so a JPA save cannot change it — a
     * native UPDATE writes the column directly, letting the test assign deterministic timestamps.
     */
    private void setCreatedAt(UUID id, LocalDateTime ts) {
        em.createNativeQuery("UPDATE instances SET created_at = :ts WHERE id = :id")
                .setParameter("ts", ts)
                .setParameter("id", id)
                .executeUpdate();
    }
}
