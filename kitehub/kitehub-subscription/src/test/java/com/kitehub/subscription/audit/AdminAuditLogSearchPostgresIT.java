package com.kitehub.subscription.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link AdminAuditLogRepository#search} on real PostgreSQL (GAP-774).
 *
 * <p>Validates the filtered audit-log search backing {@code GET /api/v1/admin/audit-logs}.
 * The query uses the {@code (:param IS NULL OR a.field = :param)} optional-filter idiom —
 * its null-handling + ordering must be verified on real Postgres (H2 / Mockito cannot catch
 * binding or ordering bugs in this JPQL pattern). Per
 * {@code .claude/rules/pre-handoff-self-test-completeness.md} §3.</p>
 *
 * @since GAP-774
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("AdminAuditLogRepository.search — Postgres filtered query (GAP-774)")
class AdminAuditLogSearchPostgresIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("kitehub_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

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
    private AdminAuditLogRepository repository;

    private final UUID adminA = UUID.randomUUID();
    private final UUID adminB = UUID.randomUUID();

    @BeforeEach
    void seed() {
        repository.deleteAll();
        // 3 rows spanning 3 days, 2 admins, 2 actions
        repository.save(row(adminA, "BETA_REQUEST_APPROVE", LocalDateTime.now().minusDays(3)));
        repository.save(row(adminB, "BETA_REQUEST_REJECT", LocalDateTime.now().minusDays(2)));
        repository.save(row(adminA, "INSTANCE_SUSPEND", LocalDateTime.now().minusDays(1)));
        repository.flush();
    }

    @Test
    @DisplayName("no filters → all rows, newest first")
    void noFilters_returnsAllNewestFirst() {
        Page<AdminAuditLog> page = repository.search(null, null, null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(3);
        // ORDER BY created_at DESC — newest (INSTANCE_SUSPEND) first
        assertThat(page.getContent().get(0).getAction()).isEqualTo("INSTANCE_SUSPEND");
        assertThat(page.getContent().get(2).getAction()).isEqualTo("BETA_REQUEST_APPROVE");
    }

    @Test
    @DisplayName("action filter → only matching action")
    void actionFilter_returnsMatchingOnly() {
        Page<AdminAuditLog> page =
                repository.search("BETA_REQUEST_APPROVE", null, null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getAction()).isEqualTo("BETA_REQUEST_APPROVE");
    }

    @Test
    @DisplayName("adminUserId filter → only that admin's rows")
    void adminFilter_returnsMatchingOnly() {
        Page<AdminAuditLog> page =
                repository.search(null, adminA, null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(r -> r.getAdminUserId().equals(adminA));
    }

    @Test
    @DisplayName("date-range filter → only rows within window")
    void dateRangeFilter_returnsWithinWindow() {
        LocalDateTime from = LocalDateTime.now().minusDays(2).minusHours(1);
        LocalDateTime to = LocalDateTime.now();

        Page<AdminAuditLog> page = repository.search(null, null, from, to, PageRequest.of(0, 20));

        // excludes the 3-days-ago row
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(AdminAuditLog::getAction)
                .containsExactly("INSTANCE_SUSPEND", "BETA_REQUEST_REJECT");
    }

    @Test
    @DisplayName("combined filters (action + admin) → intersection")
    void combinedFilters_returnsIntersection() {
        Page<AdminAuditLog> page =
                repository.search("INSTANCE_SUSPEND", adminA, null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getAction()).isEqualTo("INSTANCE_SUSPEND");
        assertThat(page.getContent().get(0).getAdminUserId()).isEqualTo(adminA);
    }

    @Test
    @DisplayName("pagination → page size honored")
    void pagination_honorsPageSize() {
        Page<AdminAuditLog> page = repository.search(null, null, null, null, PageRequest.of(0, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    private AdminAuditLog row(UUID adminId, String action, LocalDateTime createdAt) {
        return AdminAuditLog.builder()
                .adminUserId(adminId)
                .action(action)
                .targetEntityType("beta_access_request")
                .targetEntityId("1")
                .requestIp("203.0.113.7")
                .payloadJson("{}")
                .success(true)
                .createdAt(createdAt)
                .build();
    }
}
