package com.kitehub.subscription.service;

import com.kitehub.subscription.audit.AdminAuditLog;
import com.kitehub.subscription.audit.AdminAuditLogRepository;
import com.kitehub.subscription.audit.TenantAuditService;
import com.kitehub.subscription.dto.RegisterResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-949 regression IT — Wave provisioning-1 Bucket B (KC-1 pre-walk batch fix).
 *
 * <p>Proves a successful beta-invite registration ACTUALLY persists a
 * {@code TENANT_PROVISIONED} {@link AdminAuditLog} row whose {@code admin_user_id}
 * equals the freshly-provisioned owner id.</p>
 *
 * <p><b>Why a Postgres Testcontainers IT (not Mockito):</b> the bug is a transaction
 * timing + FK-visibility issue, NOT a logic issue. {@link AuthService#registerFromBetaInvite}
 * saves the owner row inside its still-open {@code @Transactional} boundary, then the audit
 * helper writes via {@link TenantAuditService#recordTenantProvisioned} (which runs
 * {@code REQUIRES_NEW}). Before the fix the audit txn could not see the uncommitted owner
 * row under {@code READ COMMITTED} → {@code admin_user_id} FK → {@code users(id)} failed →
 * {@code TenantAuditService} swallowed the exception → the audit row was silently dropped
 * (PDPL Art 11 / OWASP A09 trail lost) while signup still "succeeded". Mockito mocks bypass
 * the real FK + txn boundary, so only a real Postgres round-trip with the real txn surfaces
 * the bug — per {@code .claude/rules/postgres-specific-type-testcontainers.md}.</p>
 *
 * <p>The fix defers the audit write to {@code afterCommit} of the parent registration txn,
 * so the owner row is committed + FK-visible by the time the audit's {@code REQUIRES_NEW}
 * insert runs. This IT FAILS before the fix (zero audit rows) and PASSES after (1 row,
 * correct owner id).</p>
 *
 * @since Wave provisioning-1 Bucket B — GAP-949
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("GAP-949 — beta-invite registration persists TENANT_PROVISIONED audit row")
class AuthServiceTenantProvisionedAuditPostgresIT {

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
    private AuthService authService;

    @Autowired
    private AdminAuditLogRepository adminAuditLogRepository;

    @Autowired
    private InstanceRepository instanceRepository;

    @Autowired
    private UserRepository userRepository;

    // Beta registration publishes tenant.created (outbox + best-effort RMQ fast-path).
    // Mock the broker so the IT runs without a live RabbitMQ.
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void cleanUp() {
        // FK order: audit_log.admin_user_id → users(id); instances.owner_id → users(id).
        adminAuditLogRepository.deleteAll();
        instanceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("registerFromBetaInvite → 1 TENANT_PROVISIONED audit row with owner admin_user_id")
    void betaInviteRegistration_persistsTenantProvisionedAuditRow() {
        RegisterResponse response = authService.registerFromBetaInvite(
                "Acme School", "acme949", "owner949@acme.test", "Sup3r$ecret-2026");

        assertThat(response).isNotNull();
        UUID ownerId = response.getUser().getId();
        UUID tenantId = response.getInstance().getId();
        assertThat(ownerId).isNotNull();
        assertThat(tenantId).isNotNull();

        // The audit write is deferred to afterCommit of the registration txn; by the time
        // this assertion runs the txn has committed (no @Transactional on the test method),
        // so the row must be present.
        Page<AdminAuditLog> rows = adminAuditLogRepository.findByActionOrderByCreatedAtDesc(
                TenantAuditService.ACTION_TENANT_PROVISIONED, PageRequest.of(0, 10));

        assertThat(rows.getContent())
                .as("exactly one TENANT_PROVISIONED audit row must be persisted (GAP-949)")
                .hasSize(1);

        AdminAuditLog row = rows.getContent().get(0);
        assertThat(row.getAdminUserId())
                .as("admin_user_id must equal the provisioned owner id (FK to users.id)")
                .isEqualTo(ownerId);
        assertThat(row.getTargetEntityId()).isEqualTo(tenantId.toString());
        assertThat(row.isSuccess()).isTrue();
        assertThat(row.getPayloadJson())
                .as("payload carries subdomain + owner email for the provisioning trail")
                .contains("acme949")
                .contains("owner949@acme.test");
    }
}
