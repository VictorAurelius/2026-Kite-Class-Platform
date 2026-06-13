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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

/**
 * Regression IT for GAP-1273 — the paid-upgrade revenue path must NOT split-brain when the
 * post-upgrade owner notification / email-log side-effect fails.
 *
 * <p>Reproduces the production-equivalent scenario surfaced by the G3 walk (2026-06-13): an admin
 * confirms a PENDING create-flow payment. {@code PaymentService.confirmPayment} captures the
 * payment, then {@code SubscriptionService.applyPendingUpgrade} (REQUIRES_NEW, GAP-1062) flips the
 * tier FREE → PREMIUM and fires {@code EmailServiceClient.sendSubscriptionCreatedEmail} with the
 * instance's {@code contact_email}. When that email is NULL (seed artifact; production populates it
 * via AuthService), the {@code email_sent_log.recipient} NOT-NULL INSERT only fails at the tier-flip
 * transaction's COMMIT — OUTSIDE the inner try/catch.</p>
 *
 * <p><strong>Pre-fix (the bug):</strong> the failing EmailSentLog INSERT ran in the SAME tier-flip
 * transaction (D1 coupling) → the whole {@code applyPendingUpgrade} commit rolled back (tier-flip
 * reverted) → {@code confirmPayment} swallowed the exception (D2) and still returned a COMPLETED
 * response → split-brain: payment COMPLETED + subscription stuck PENDING + instances.tier stuck FREE.</p>
 *
 * <p><strong>Post-fix:</strong> D1 isolates the EmailSentLog side-effect (REQUIRES_NEW on the email
 * methods + a NULL-recipient guard in {@code dispatchEmail}), so a notification failure can never
 * roll back the paid-upgrade tier-flip. The tier-flip commits consistently: payment COMPLETED +
 * subscription ACTIVE/PREMIUM + instances.tier=PREMIUM. NO split-brain.</p>
 *
 * <p>Runs in the test profile's direct-send mode ({@code kitehub.email.use-queue=false}) so the full
 * app context loads without a RabbitMQ {@code ConnectionFactory} (queue mode wires a listener
 * factory that needs a broker absent in tests). To still drive the email path to {@code
 * recordEmailSent} (the NULL-recipient INSERT that triggered the bug), the {@code RestTemplate} is
 * mocked to return HTTP 200 so {@code sendEmailRequestDirect} "succeeds" and the audit-log INSERT is
 * attempted. Real Postgres (Testcontainers) — H2 does not enforce/defer the NOT-NULL violation at
 * commit the same way, and transaction propagation is a Spring {@code PlatformTransactionManager}
 * mechanism a mock can't exercise.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("GAP-1273 — admin payment-confirm does not split-brain on NULL-recipient email-log")
class ConfirmPaymentSplitBrainIT {

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

    // Direct-send mode HTTP client — mocked to a 200 so dispatchEmail() reaches recordEmailSent()
    // (the NULL-recipient EmailSentLog INSERT that triggered the G3-walk bug). Without this the HTTP
    // send throws first and the INSERT never runs, masking the pre-fix bug.
    @MockitoBean
    private RestTemplate restTemplate;

    @Test
    @DisplayName("NULL contact_email upgrade confirm → tier-flip COMMITS (payment COMPLETED + sub ACTIVE/PREMIUM + instance PREMIUM)")
    void adminConfirmDoesNotSplitBrainWhenContactEmailNull() {
        // The email-service HTTP call "succeeds" (200) so the email path runs through to the
        // EmailSentLog INSERT — which has a NULL recipient (the trigger). Untyped doReturn keeps the
        // package-private EmailResponse out of this test package.
        doReturn(ResponseEntity.ok().build())
                .when(restTemplate).postForEntity(anyString(), any(), any(Class.class));

        // 1. Seed a TRIAL/FREE instance with NULL contact_email — the exact G3-walk trigger.
        Instance instance = new Instance();
        instance.setSubdomain("gap1273-split-brain");
        instance.setOrganizationName("GAP-1273 Split-Brain Test");
        instance.setOwnerId(UUID.randomUUID());
        instance.setTier(PricingTier.FREE);
        instance.setStatus(InstanceStatus.TRIAL);
        instance.setContactEmail(null); // ← THE TRIGGER: no recipient for subscription-created email
        instance.setDatabaseUrl("jdbc:postgresql://localhost:5432/tenant_gap1273");
        instance.setDatabaseUsername("tenant_gap1273");
        instance.setDatabasePassword("not-a-secret-test");
        UUID instanceId = instanceRepository.save(instance).getId();

        // 2. Seed a PENDING create-flow subscription with a scheduled FREE → PREMIUM upgrade.
        Subscription subscription = new Subscription();
        subscription.setInstanceId(instanceId);
        subscription.setTier(PricingTier.FREE);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setPriceVnd(PricingTier.FREE.getPrice(BillingCycle.MONTHLY));
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setPendingTier(PricingTier.PREMIUM);
        UUID subscriptionId = subscriptionRepository.save(subscription).getId();

        // 3. Seed the matching PENDING payment, then point the subscription's pendingPaymentId at it.
        Payment payment = new Payment();
        payment.setSubscriptionId(subscriptionId);
        payment.setInstanceId(instanceId);
        payment.setAmountVnd(PricingTier.PREMIUM.getPrice(BillingCycle.MONTHLY));
        payment.setCurrency("VND");
        payment.setPaymentMethod(PaymentMethod.VIETQR);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTxnRef("KH3SUBC0FFEE01");
        UUID paymentId = paymentRepository.save(payment).getId();

        subscription.setPendingPaymentId(paymentId);
        subscriptionRepository.save(subscription);

        // 4. Admin confirms the payment. The subscription-created email has a NULL recipient; the
        //    tier-flip must still commit and the call must NOT surface an error (D1 isolates the
        //    email-log side-effect; D2 would only re-throw on a GENUINE tier-flip failure).
        assertThatCode(() -> paymentService.confirmPayment(paymentId, "ADMIN-CONFIRM-GAP1273"))
                .doesNotThrowAnyException();

        // 5. Assert NO split-brain — all three sides consistent.
        Payment reloadedPayment = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(reloadedPayment.getStatus())
                .as("payment captured")
                .isEqualTo(PaymentStatus.COMPLETED);

        Subscription reloadedSub = subscriptionRepository.findById(subscriptionId).orElseThrow();
        assertThat(reloadedSub.getStatus())
                .as("subscription activated (NOT stuck PENDING — the split-brain symptom)")
                .isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(reloadedSub.getTier())
                .as("subscription tier flipped to the paid tier")
                .isEqualTo(PricingTier.PREMIUM);
        assertThat(reloadedSub.getPendingTier())
                .as("pending tier cleared after apply")
                .isNull();

        Instance reloadedInstance = instanceRepository.findById(instanceId).orElseThrow();
        assertThat(reloadedInstance.getTier())
                .as("instances.tier synced to PREMIUM (NOT stuck FREE — the split-brain symptom)")
                .isEqualTo(PricingTier.PREMIUM);
    }
}
