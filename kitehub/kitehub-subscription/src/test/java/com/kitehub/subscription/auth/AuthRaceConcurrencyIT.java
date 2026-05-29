package com.kitehub.subscription.auth;

import com.kitehub.subscription.dto.RegisterRequest;
import com.kitehub.subscription.dto.RegisterResponse;
import com.kitehub.subscription.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Auth race-condition IT — verify 2 concurrent register() cùng subdomain
 * không cho phép duplicate-tenant row via race window.
 *
 * <p>Wave beta-prep-1 Bucket B item 2 AC: "2 concurrent signup cùng subdomain
 * → 1 success + 1 fail (atomicity preserved via unique constraint)".
 *
 * <p>Pattern reused từ {@code ConcurrentConsentWritesIT} (Wave br-4 Bucket B
 * GAP-353b precedent — Testcontainers Postgres + CountDownLatch + ExecutorService).
 *
 * <p>Setup: 2 threads → {@code authService.register(...)} cùng subdomain
 * (different emails). Expected:
 * <ul>
 *   <li>1 thread returns {@code RegisterResponse} successfully</li>
 *   <li>Other thread throws {@code IllegalArgumentException} (duplicate subdomain
 *       per `existsBy...AndDeletedFalse` check) hoặc DB-level unique-constraint
 *       violation surfaced via {@code DataIntegrityViolationException}</li>
 *   <li>DB has exactly 1 instance row</li>
 * </ul>
 *
 * <p>Real Postgres mandatory per `postgres-specific-type-testcontainers.md` —
 * H2 doesn't enforce unique-constraint timing the same way under concurrent insert.
 *
 * @since Wave beta-prep-1 Bucket B (security-beta-min)
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("AuthRaceConcurrencyIT — 2 concurrent signups same subdomain race (Wave beta-prep-1 Bucket B)")
class AuthRaceConcurrencyIT {

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
    private AuthService authService;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("2 concurrent register() same subdomain -> 1 success + 1 fail (atomic)")
    void concurrentRegisterSameSubdomain_atomicOutcome() throws InterruptedException {
        String sharedSubdomain = "racetest-" + UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest req1 = buildRequest(sharedSubdomain, "owner1@racetest.kitehub.me");
        RegisterRequest req2 = buildRequest(sharedSubdomain, "owner2@racetest.kitehub.me");

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Runnable t1 = () -> runAttempt(req1, startGate, doneGate, successCount, failureCount);
            Runnable t2 = () -> runAttempt(req2, startGate, doneGate, successCount, failureCount);
            executor.submit(t1);
            executor.submit(t2);

            startGate.countDown();
            boolean completed = doneGate.await(30, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            int totalSuccess = successCount.get();
            int totalFailure = failureCount.get();
            assertThat(totalSuccess + totalFailure).isEqualTo(2);
            assertThat(totalSuccess).isEqualTo(1).withFailMessage(
                    "Expected exactly 1 success but got %d — atomicity violated", totalSuccess);
            assertThat(totalFailure).isEqualTo(1).withFailMessage(
                    "Expected exactly 1 failure but got %d — race window allowed double-create",
                    totalFailure);
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private void runAttempt(RegisterRequest request,
                            CountDownLatch startGate,
                            CountDownLatch doneGate,
                            AtomicInteger successCount,
                            AtomicInteger failureCount) {
        try {
            startGate.await();
            RegisterResponse resp = authService.register(request);
            if (resp != null) {
                successCount.incrementAndGet();
            }
        } catch (IllegalArgumentException e) {
            failureCount.incrementAndGet();
        } catch (Exception e) {
            failureCount.incrementAndGet();
        } finally {
            doneGate.countDown();
        }
    }

    private static RegisterRequest buildRequest(String subdomain, String email) {
        RegisterRequest r = new RegisterRequest();
        r.setOrganizationName("Trung tam Race Test " + subdomain);
        r.setSubdomain(subdomain);
        r.setOwnerEmail(email);
        r.setOwnerPassword("SecurePass123!");
        return r;
    }
}
