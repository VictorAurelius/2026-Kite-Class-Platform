package com.kitehub.subscription.repository;

import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.enums.PaymentMethod;
import com.kitehub.platform.domain.enums.PaymentStatus;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration test for {@link PaymentRepository#findAfterCursor} +
 * {@link PaymentRepository#findByStatusAfterCursor} keyset pagination on real PostgreSQL
 * (GAP-1106).
 *
 * <p>Regression guard for the 42P18 ("could not determine data type of parameter") class.
 * Both prior cursor queries bound an untyped null {@code :cursorId} in the {@code IS NULL}
 * position on the first page, which Postgres can reject at PREPARE time; H2 (test) hid it.
 * The split fix (first-page query without cursor + after-cursor query with a typed param,
 * branched by repository default methods) must execute cleanly on real Postgres for the first
 * page (cursorId = null) and subsequent pages, for both the unfiltered and status-filtered
 * variants. Per {@code .claude/rules/postgres-specific-type-testcontainers.md} +
 * {@code .claude/rules/pre-handoff-self-test-completeness.md} §3.</p>
 *
 * @since GAP-1106
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("PaymentRepository cursor pagination — Postgres keyset (GAP-1106)")
class PaymentCursorPaginationPostgresIT {

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
    private PaymentRepository paymentRepository;

    @BeforeEach
    void clean() {
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("findAfterCursor first page (cursorId = null) → executes on Postgres without 42P18")
    void findAfterCursor_firstPage_nullCursor() {
        seed(5, PaymentStatus.COMPLETED);

        assertThatCode(() -> paymentRepository.findAfterCursor(null, PageRequest.of(0, 3)))
                .doesNotThrowAnyException();

        List<Payment> firstPage = paymentRepository.findAfterCursor(null, PageRequest.of(0, 3));
        assertThat(firstPage).hasSize(3);
    }

    @Test
    @DisplayName("findAfterCursor subsequent page → disjoint from first page, no 42P18")
    void findAfterCursor_subsequentPage() {
        seed(5, PaymentStatus.COMPLETED);
        Pageable page = PageRequest.of(0, 3);

        List<Payment> firstPage = paymentRepository.findAfterCursor(null, page);
        UUID cursor = firstPage.get(firstPage.size() - 1).getId();

        List<Payment> secondPage = paymentRepository.findAfterCursor(cursor, page);

        assertThat(secondPage).hasSize(2);
        // Disjoint — order-semantics-agnostic (Postgres orders uuid by unsigned bytes,
        // which differs from Java UUID.compareTo; don't assert on Java ordering).
        assertThat(secondPage)
                .noneMatch(s -> firstPage.stream().anyMatch(f -> f.getId().equals(s.getId())));
    }

    @Test
    @DisplayName("findByStatusAfterCursor first page (cursorId = null) → no 42P18, status-filtered")
    void findByStatusAfterCursor_firstPage_nullCursor() {
        seed(4, PaymentStatus.COMPLETED);
        seed(3, PaymentStatus.PENDING);
        Pageable page = PageRequest.of(0, 10);

        assertThatCode(() ->
                paymentRepository.findByStatusAfterCursor(PaymentStatus.COMPLETED, null, page))
                .doesNotThrowAnyException();

        List<Payment> completed =
                paymentRepository.findByStatusAfterCursor(PaymentStatus.COMPLETED, null, page);
        assertThat(completed).hasSize(4);
        assertThat(completed).allMatch(p -> p.getStatus() == PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("findByStatusAfterCursor subsequent page → status-filtered, disjoint from first")
    void findByStatusAfterCursor_subsequentPage() {
        seed(5, PaymentStatus.COMPLETED);
        seed(2, PaymentStatus.PENDING);
        Pageable page = PageRequest.of(0, 3);

        List<Payment> firstPage =
                paymentRepository.findByStatusAfterCursor(PaymentStatus.COMPLETED, null, page);
        UUID cursor = firstPage.get(firstPage.size() - 1).getId();

        List<Payment> secondPage =
                paymentRepository.findByStatusAfterCursor(PaymentStatus.COMPLETED, cursor, page);

        assertThat(secondPage).hasSize(2);
        assertThat(secondPage).allMatch(p -> p.getStatus() == PaymentStatus.COMPLETED);
        assertThat(secondPage)
                .noneMatch(s -> firstPage.stream().anyMatch(f -> f.getId().equals(s.getId())));
    }

    private void seed(int count, PaymentStatus status) {
        for (int i = 0; i < count; i++) {
            Payment payment = new Payment();
            payment.setSubscriptionId(UUID.randomUUID());
            payment.setInstanceId(UUID.randomUUID());
            payment.setAmountVnd(100_000L + i);
            payment.setCurrency("VND");
            payment.setPaymentMethod(PaymentMethod.VIETQR);
            payment.setStatus(status);
            paymentRepository.save(payment);
        }
        paymentRepository.flush();
    }
}
