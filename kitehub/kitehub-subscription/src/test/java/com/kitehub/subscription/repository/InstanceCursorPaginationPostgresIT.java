package com.kitehub.subscription.repository;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration test for {@link InstanceRepository#findAfterCursor} keyset pagination on real
 * PostgreSQL (GAP-1106).
 *
 * <p>Regression guard for the 42P18 ("could not determine data type of parameter") class.
 * The prior single JPQL bound an untyped null {@code :cursorId} in the {@code IS NULL} position
 * on the first page, which Postgres can reject at PREPARE time; H2 (test) hid it. The split fix
 * (first-page query without cursor + after-cursor query with a typed param, branched by the
 * repository default method) must execute cleanly on real Postgres for both the first page
 * (cursorId = null) and subsequent pages. Per
 * {@code .claude/rules/postgres-specific-type-testcontainers.md} +
 * {@code .claude/rules/pre-handoff-self-test-completeness.md} §3.</p>
 *
 * @since GAP-1106
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("InstanceRepository.findAfterCursor — Postgres keyset pagination (GAP-1106)")
class InstanceCursorPaginationPostgresIT {

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

    @BeforeEach
    void clean() {
        instanceRepository.deleteAll();
    }

    @Test
    @DisplayName("first page (cursorId = null) → executes on Postgres without 42P18")
    void firstPage_nullCursor_noUntypedNullParamError() {
        seed(5);
        // The originating bug: null cursor → `:cursorId IS NULL` → 42P18 at PREPARE on
        // Postgres. The split fix binds no untyped null, so this must execute cleanly.
        assertThatCode(() -> instanceRepository.findAfterCursor(null, PageRequest.of(0, 3)))
                .doesNotThrowAnyException();

        List<Instance> firstPage = instanceRepository.findAfterCursor(null, PageRequest.of(0, 3));
        assertThat(firstPage).hasSize(3);
    }

    @Test
    @DisplayName("subsequent page (cursorId set) → disjoint from first page, no 42P18")
    void subsequentPage_afterCursor_returnsRemaining() {
        seed(5);
        Pageable page = PageRequest.of(0, 3);

        List<Instance> firstPage = instanceRepository.findAfterCursor(null, page);
        UUID cursor = firstPage.get(firstPage.size() - 1).getId();

        List<Instance> secondPage = instanceRepository.findAfterCursor(cursor, page);

        assertThat(secondPage).hasSize(2);
        // Keyset pages are disjoint — order-semantics-agnostic (Postgres orders uuid by
        // unsigned bytes, which differs from Java UUID.compareTo; don't assert on that).
        assertThat(secondPage)
                .noneMatch(s -> firstPage.stream().anyMatch(f -> f.getId().equals(s.getId())));
    }

    @Test
    @DisplayName("full keyset traversal covers every row once + matches single-query order")
    void fullTraversal_coversAllRows() {
        seed(5);

        // Canonical DB order via one large page (cursor-free branch).
        List<UUID> single = instanceRepository.findAfterCursor(null, PageRequest.of(0, 100))
                .stream().map(Instance::getId).toList();
        assertThat(single).hasSize(5);

        // Paginated keyset traversal must reproduce the same order exactly — proves the
        // after-cursor branch is consistent with the first-page branch on Postgres uuid order.
        List<UUID> traversed = new ArrayList<>();
        UUID cursor = null;
        Pageable page = PageRequest.of(0, 2);
        while (true) {
            List<Instance> batch = instanceRepository.findAfterCursor(cursor, page);
            if (batch.isEmpty()) {
                break;
            }
            batch.forEach(i -> traversed.add(i.getId()));
            cursor = batch.get(batch.size() - 1).getId();
            if (batch.size() < 2) {
                break;
            }
        }
        assertThat(traversed).containsExactlyElementsOf(single);
    }

    private void seed(int count) {
        for (int i = 0; i < count; i++) {
            Instance instance = new Instance();
            instance.setSubdomain("cur" + i);
            instance.setOrganizationName("Cursor Org " + i);
            instance.setSlug("cursor-org-" + i);
            instance.setOwnerId(UUID.randomUUID());
            instance.setTier(PricingTier.FREE);
            instance.setStatus(InstanceStatus.TRIAL);
            instance.setDatabaseUrl("pending");
            instance.setDatabaseUsername("pending");
            instance.setDatabasePassword("pending");
            instanceRepository.save(instance);
        }
        instanceRepository.flush();
    }
}
