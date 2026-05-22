package com.kitehub.subscription.beta.repository;

import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Wave 105 Bucket A (failure-mode A1 hardening) — verifies V55 partial unique
 * index {@code uq_beta_access_request_email_pending} enforces "one open
 * PENDING request per email" at the DB layer.
 *
 * <p>Unlike sibling {@link BetaAccessRequestRepositoryPostgresIT} (which uses
 * Hibernate {@code create-drop} to materialize the schema from JPA entities),
 * this test runs Flyway migrations end-to-end so the V55 SQL DDL is actually
 * exercised. Without Flyway in the loop, JPA's {@code @Table indexes=} attribute
 * only emits the non-unique status + email indexes from V28 — the partial
 * unique we ship in V55 is invisible.</p>
 *
 * @since Wave 105 Bucket A
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("BetaAccessRequest V55 partial unique index — PENDING idempotency (Wave 105 Bucket A)")
class BetaAccessRequestPendingUniqueIndexIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("kitehub_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // Flyway drives schema (not Hibernate create-drop) so V55 partial unique is real.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "false");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private BetaAccessRequestRepository repository;

    @BeforeEach
    void cleanState() {
        repository.deleteAll();
        repository.flush();
    }

    private static BetaAccessRequest pendingFor(String email) {
        OffsetDateTime now = OffsetDateTime.now();
        return BetaAccessRequest.builder()
                .email(email)
                .name("Trần Thị Hồng")
                .orgName("Trung tâm Sky Education")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.PENDING)
                .consentGiven(true)
                .consentAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    @DisplayName("Second PENDING for same email rejected with DataIntegrityViolationException")
    void duplicate_pending_same_email_rejected() {
        repository.saveAndFlush(pendingFor("hong.tran@skyedu.vn"));

        assertThatThrownBy(() -> repository.saveAndFlush(pendingFor("hong.tran@skyedu.vn")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("PENDING then APPROVED frees slot for new PENDING (same email)")
    void approved_releases_pending_slot() {
        BetaAccessRequest first = repository.saveAndFlush(pendingFor("hong.tran@skyedu.vn"));
        first.setStatus(BetaAccessRequestStatus.APPROVED);
        first.setInviteToken(UUID.randomUUID());
        first.setInviteTokenExpiry(OffsetDateTime.now().plusHours(24));
        repository.saveAndFlush(first);

        // Same email, fresh PENDING is now allowed (e.g. tenant reapplies after
        // approval but never signs up + token expires).
        BetaAccessRequest second = repository.saveAndFlush(pendingFor("hong.tran@skyedu.vn"));
        assertThat(second.getId()).isNotNull().isNotEqualTo(first.getId());
    }

    @Test
    @DisplayName("PENDING then REJECTED frees slot for new PENDING (same email)")
    void rejected_releases_pending_slot() {
        BetaAccessRequest first = repository.saveAndFlush(pendingFor("hong.tran@skyedu.vn"));
        first.setStatus(BetaAccessRequestStatus.REJECTED);
        first.setRejectedAt(OffsetDateTime.now());
        first.setRejectionReason("Out of capacity");
        repository.saveAndFlush(first);

        BetaAccessRequest second = repository.saveAndFlush(pendingFor("hong.tran@skyedu.vn"));
        assertThat(second.getId()).isNotNull().isNotEqualTo(first.getId());
    }

    @Test
    @DisplayName("Different emails can both be PENDING simultaneously (no false positive)")
    void distinct_emails_both_pending() {
        repository.saveAndFlush(pendingFor("hong.tran@skyedu.vn"));
        BetaAccessRequest other = repository.saveAndFlush(pendingFor("tam.nguyen@quangminh.edu.vn"));

        assertThat(other.getId()).isNotNull();
        assertThat(repository.count()).isEqualTo(2);
    }
}
