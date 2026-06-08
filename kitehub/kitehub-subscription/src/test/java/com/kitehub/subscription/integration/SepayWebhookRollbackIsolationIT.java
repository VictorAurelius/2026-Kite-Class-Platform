package com.kitehub.subscription.integration;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PaymentMethod;
import com.kitehub.platform.domain.enums.PaymentStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.PaymentRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import com.kitehub.subscription.service.PaymentService;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Regression IT for GAP-1062 — {@code applyPendingUpgrade} failure must NOT roll back
 * the captured payment (transaction-isolation / rollback-poisoning).
 *
 * <p>Reproduces the production-equivalent scenario surfaced during SePay Test Mode
 * verify (2026-06-08): a SePay webhook completes a payment, then calls
 * {@code SubscriptionService.applyPendingUpgrade}. When that downstream best-effort
 * step throws (here: the subscription was soft-deleted, so the custom
 * {@code SubscriptionRepository.findById(... AND deleted = false)} returns empty and
 * {@code applyPendingUpgrade} throws), the payment capture must still commit.</p>
 *
 * <p>Before the fix ({@code applyPendingUpgrade} joined the parent transaction with
 * default {@code Propagation.REQUIRED}), the throw set the shared transaction
 * rollback-only; {@code processSepayWebhook}'s try/catch swallowed the exception but
 * the parent commit then threw {@code UnexpectedRollbackException} and the
 * {@code payment.complete()} write was rolled back — payment stuck PENDING. With
 * {@code Propagation.REQUIRES_NEW} the upgrade failure is isolated to its own
 * transaction and the payment commits COMPLETED. Same class as the 2026-05-16
 * admin-login 500 incident (see {@code audit-service-isolation.md}).</p>
 *
 * <p>Real Postgres (Testcontainers) — not H2: the app's tenant interceptor runs the
 * Postgres-specific {@code set_config()} on every connection, and transaction
 * propagation / rollback-only is a Spring {@code PlatformTransactionManager} mechanism
 * a mocked {@code applyPendingUpgrade} would never exercise.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("GAP-1062 — SePay webhook payment capture survives applyPendingUpgrade failure")
class SepayWebhookRollbackIsolationIT {

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
    private PaymentService paymentService;

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("applyPendingUpgrade throw → payment still COMPLETED (no rollback poisoning)")
    void paymentCaptureSurvivesApplyPendingUpgradeFailure() {
        // 1. Seed instance + subscription directly via repositories (bypasses controller
        //    @PreAuthorize; platform tables are not FORCE-RLS so the Flyway owner bypasses).
        Instance instance = new Instance();
        instance.setSubdomain("gap1062-rollback");
        instance.setOrganizationName("GAP-1062 Rollback Test");
        instance.setOwnerId(UUID.randomUUID());
        instance.setTier(PricingTier.BASIC);
        instance.setStatus(InstanceStatus.ACTIVE);
        instance.setDatabaseUrl("jdbc:postgresql://localhost:5432/tenant_gap1062");
        instance.setDatabaseUsername("tenant_gap1062");
        instance.setDatabasePassword("not-a-secret-test");
        UUID instanceId = instanceRepository.save(instance).getId();

        Subscription subscription = new Subscription();
        subscription.setInstanceId(instanceId);
        subscription.setTier(PricingTier.BASIC);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setPriceVnd(PricingTier.BASIC.getPrice(BillingCycle.MONTHLY));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartedAt(LocalDateTime.now());
        subscription.setExpiresAt(LocalDateTime.now().plusMonths(1));
        UUID subscriptionId = subscriptionRepository.save(subscription).getId();

        // 2. Seed a PENDING SePay payment matching the subscription, with a txnRef.
        String txnRef = "KH3SUBDEAD0001";
        long amount = 100_000L;
        Payment payment = new Payment();
        payment.setSubscriptionId(subscriptionId);
        payment.setInstanceId(instanceId);
        payment.setAmountVnd(amount);
        payment.setCurrency("VND");
        payment.setPaymentMethod(PaymentMethod.VIETQR);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTxnRef(txnRef);
        UUID paymentId = paymentRepository.save(payment).getId();

        // 3. Soft-delete the subscription → SubscriptionRepository.findById (which filters
        //    `deleted = false`) returns empty → applyPendingUpgrade orElseThrow fires.
        subscription.softDelete();
        subscriptionRepository.save(subscription);
        assertThat(subscriptionRepository.findById(subscriptionId)).isEmpty();

        // 4. Process the webhook. applyPendingUpgrade WILL throw internally; the payment
        //    capture must still commit and the call must NOT surface UnexpectedRollbackException.
        assertThatCode(() ->
            paymentService.processSepayWebhook("SEPAY-GAP1062", amount, "Thanh toan " + txnRef))
            .doesNotThrowAnyException();

        // 5. Payment captured despite the upgrade failure (the documented contract:
        //    "Payment is still completed, but subscription update failed → admin/retry").
        Payment reloaded = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(reloaded.getTransactionId()).isEqualTo("SEPAY-GAP1062");
    }
}
