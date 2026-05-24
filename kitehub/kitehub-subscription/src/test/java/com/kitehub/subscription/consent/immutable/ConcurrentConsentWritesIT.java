package com.kitehub.subscription.consent.immutable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrent write IT — verifies hash chain preservation under contention.
 *
 * <p>Wave br-4 Bucket B (GAP-353b) AC: "concurrent_audit_log_writes_preserve_hash_chain".
 *
 * <p>Setup: 2 threads each call {@code recordConsent} cho cùng userId 4 lần (8 inserts
 * total). With SERIALIZABLE isolation + REQUIRES_NEW propagation + service-level retry
 * loop, all 8 inserts succeed AND form valid linear hash chain (every prev_hash matches
 * the immediately previous current_hash). Hash chain validator passes.
 *
 * <p>Without retry-on-serialization-failure: ~50% inserts would fail with
 * {@code ConcurrencyFailureException} (Postgres serialization_failure SQLSTATE 40001).
 *
 * @since Wave beta-readiness-4 Bucket B — GAP-353b
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("ConcurrentConsentWritesIT — hash chain preservation (GAP-353b)")
class ConcurrentConsentWritesIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("kitehub_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private ConsentService consentService;

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        new JdbcTemplate(dataSource).execute("TRUNCATE TABLE consent_record_immutable RESTART IDENTITY");
    }

    @Test
    @DisplayName("2 concurrent threads × 4 inserts same userId → 8 rows + linear chain")
    void concurrent_writes_preserve_chain() throws Exception {
        final Long userId = 9999L;
        final int threadsCount = 2;
        final int insertsPerThread = 4;
        final int expectedRows = threadsCount * insertsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadsCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        for (int t = 0; t < threadsCount; t++) {
            final int threadIdx = t;
            executor.submit(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < insertsPerThread; i++) {
                        try {
                            consentService.recordConsent(
                                    userId,
                                    7L,
                                    Map.of("essential", true,
                                            "analytics", (i % 2 == 0),
                                            "marketing", (i % 2 == 1)),
                                    "203.0.113." + (threadIdx + 1),
                                    "concurrent-ua-thread-" + threadIdx);
                            successCount.incrementAndGet();
                        } catch (Exception ex) {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean finished = doneGate.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).as("all worker threads finished within 30s").isTrue();
        assertThat(successCount.get())
                .as("all %d concurrent inserts must succeed (retry-on-serialization-failure)",
                        expectedRows)
                .isEqualTo(expectedRows);
        assertThat(errorCount.get())
                .as("zero permanent failures expected after retry loop")
                .isZero();

        // Hash chain integrity validation — fails throw, success returns.
        consentService.verifyChainIntegrity(userId);

        List<ConsentRecordImmutable> history = consentService.findHistory(userId);
        assertThat(history).hasSize(expectedRows);
        // Each consecutive row's prev_hash MUST equal previous row's current_hash.
        for (int i = 1; i < history.size(); i++) {
            assertThat(history.get(i).getPrevHash())
                    .as("row %d prev_hash chain link", i)
                    .isEqualTo(history.get(i - 1).getCurrentHash());
        }
        // Head row prev_hash null.
        assertThat(history.get(0).getPrevHash()).isNull();
    }
}
