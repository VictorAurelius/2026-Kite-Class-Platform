package com.kitehub.subscription.repository;

import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
 * GAP-942 / GAP-1054 regression guard — proves V62 made
 * {@code subscriptions.started_at} + {@code expires_at} nullable for the
 * PENDING state (SUB-20 manual VietQR upgrade gate).
 *
 * <p>Before V62 those two columns were {@code NOT NULL} (designed when only the
 * ACTIVE state existed). PR #2151 SUB-20 fix creates a PENDING subscription with
 * both columns null until {@code applyPendingUpgrade} activates it — which threw
 * {@code SQLState 23502} (not-null violation), mis-mapped by GlobalExceptionHandler
 * to HTTP 409 RESOURCE_CONFLICT. V62 drops the NOT NULL on both columns and extends
 * {@code chk_subscription_status} to include PENDING.</p>
 *
 * <p>Like sibling {@link BetaAccessRequestPendingUniqueIndexIT}, this test runs
 * Flyway migrations end-to-end against a real PostgreSQL container so the V62 DDL
 * is actually exercised — Hibernate {@code create-drop} from the JPA entity would
 * materialise nullability from the {@code @Column(nullable = ...)} annotation
 * rather than the shipped migration, hiding any schema↔migration drift.</p>
 *
 * <p>{@code subscriptions.instance_id} is {@code NOT NULL} with FK
 * {@code fk_subscription_instance → instances(id)}, so a parent {@code instances}
 * row must be seeded (via {@link JdbcTemplate}) before any subscription INSERT.</p>
 *
 * @since GAP-1054 (Wave p0-prov-1)
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Subscription V62 nullable started_at/expires_at — PENDING regression guard (GAP-942/1054)")
class SubscriptionPendingNullableColumnsIT {

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
        // Flyway drives schema (not Hibernate create-drop) so V62 DDL is real.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "false");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    /** Fixed parent-instance id so {@link #pendingSubscription()} can satisfy the FK. */
    private static final UUID INSTANCE_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private SubscriptionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** RabbitMQ is excluded from the test profile; EmailServiceClient needs the bean. */
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void seedParentInstance() {
        // FK-safe cleanup: children (subscriptions) before parent (instances).
        jdbcTemplate.update("DELETE FROM subscriptions");
        jdbcTemplate.update("DELETE FROM instances");

        // Minimal parent instances row — only V1 NOT-NULL/no-default columns are
        // required; all later ALTER-added columns (vertical_type, migration_phase,
        // email_notifications, ...) carry DEFAULT clauses. tier/status have no CHECK
        // constraint on instances, so FREE/TRIAL are valid enum values.
        jdbcTemplate.update(
                "INSERT INTO instances (id, subdomain, organization_name, owner_id, tier, status, "
                        + "database_url, database_username, database_password, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())",
                INSTANCE_ID,
                "pending-nullable-it",
                "Trung tâm Sky Education",
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "FREE",
                "TRIAL",
                "jdbc:postgresql://localhost:5432/kiteclass_it",
                "it_user",
                "encrypted-test-password");
    }

    private static Subscription pendingSubscription() {
        Subscription sub = new Subscription();
        sub.setInstanceId(INSTANCE_ID);
        sub.setTier(PricingTier.PREMIUM);
        sub.setBillingCycle(BillingCycle.MONTHLY);
        sub.setPriceVnd(1_500_000L);
        sub.setStatus(SubscriptionStatus.PENDING);
        // Core of the V62 proof: PENDING persists with BOTH date columns null
        // until applyPendingUpgrade flips it to ACTIVE.
        sub.setStartedAt(null);
        sub.setExpiresAt(null);
        sub.setAutoRenew(true);
        return sub;
    }

    private static Subscription activatedSubscription() {
        Subscription sub = new Subscription();
        sub.setInstanceId(INSTANCE_ID);
        sub.setTier(PricingTier.PREMIUM);
        sub.setBillingCycle(BillingCycle.MONTHLY);
        sub.setPriceVnd(1_500_000L);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        sub.setStartedAt(now);
        sub.setExpiresAt(now.plusMonths(1));
        sub.setAutoRenew(true);
        return sub;
    }

    @Test
    @DisplayName("PENDING subscription persists with null started_at + expires_at (V62 NOT NULL drop)")
    void pendingSubscription_persistsWithNullStartedAtAndExpiresAt() {
        Subscription saved = repository.saveAndFlush(pendingSubscription());

        Subscription reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(reloaded.getStartedAt()).isNull();
        assertThat(reloaded.getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("ACTIVE subscription persists with non-null dates (no V62 regression)")
    void activatedSubscription_persistsWithNonNullDates() {
        Subscription saved = repository.saveAndFlush(activatedSubscription());

        Subscription reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(reloaded.getStartedAt()).isNotNull();
        assertThat(reloaded.getExpiresAt()).isNotNull();
    }
}
