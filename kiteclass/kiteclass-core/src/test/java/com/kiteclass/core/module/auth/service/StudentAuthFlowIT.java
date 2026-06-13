package com.kiteclass.core.module.auth.service;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.auth.dto.LoginRequest;
import com.kiteclass.core.module.auth.dto.LoginResponse;
import com.kiteclass.core.module.auth.entity.AuthCredential;
import com.kiteclass.core.module.auth.repository.AuthCredentialRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * KC-9 student-auth end-to-end flow against REAL PostgreSQL via Testcontainers
 * (real Flyway V1..V89 schema, NOT Hibernate ddl-auto) — mirrors
 * {@link com.kiteclass.core.module.auth.repository.AuthCredentialPostgresIT}.
 *
 * <p>Proves STUDENT login works the same KC-native way parent/teacher do (GAP-1277):
 * <ul>
 *   <li>provision a STUDENT credential (entity_type=STUDENT) — V89 CHECK accepts it</li>
 *   <li>login happy path → JWT carries {@code role=STUDENT} + referenceId + tenantId</li>
 *   <li>wrong password → uniform 401 INVALID_CREDENTIALS</li>
 *   <li>STUDENT entity_type round-trips through the real Postgres CHECK constraint</li>
 * </ul>
 *
 * <p>{@link AuthCredentialProvisioningService} / {@link AuthService} / {@link AuthTokenService}
 * are plain constructor-injected services, instantiated manually here against the
 * Testcontainers-backed repository (this test lives in the same package so it can call
 * the package-private {@code AuthTokenService.init()}).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",            // run Flyway manually in @BeforeAll (not Boot)
        "spring.jpa.hibernate.ddl-auto=none",     // schema comes from Flyway, NOT Hibernate
        "spring.jpa.properties.hibernate.default_schema=public"
})
@DisplayName("KC-9 student-auth flow — real Flyway V89 schema (Testcontainers)")
class StudentAuthFlowIT {

    /** HS512 needs ≥64 bytes — fixed test secret (NOT production). */
    private static final String JWT_SECRET =
            "kc9-student-auth-test-secret-kc9-student-auth-test-secret-0123456789";

    @SuppressWarnings("resource") // lifecycle managed by @Testcontainers
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("student_auth_it")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeAll
    static void runFlyway() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @Autowired
    private AuthCredentialRepository repository;

    private AuthCredentialProvisioningService provisioning;
    private AuthService authService;

    private static final String EMAIL = "student.kc9@example.com";
    private static final String PASSWORD = "Password1!";

    @BeforeEach
    void wireServices() {
        provisioning = new AuthCredentialProvisioningService(repository);
        AuthTokenService tokenService = new AuthTokenService(JWT_SECRET, Duration.ofHours(12));
        tokenService.init(); // package-private @PostConstruct — invoked manually (no Spring proxy)
        authService = new AuthService(repository, tokenService);
    }

    @Test
    @DisplayName("provision STUDENT → login happy path → JWT role=STUDENT + referenceId + tenant")
    void provisionThenLogin_returnsStudentRoleToken() {
        UUID tenant = UUID.randomUUID();
        Long studentId = 77L;

        provisioning.setPassword(
                AuthCredentialProvisioningService.ROLE_STUDENT, studentId, EMAIL, tenant, PASSWORD);
        repository.flush();

        LoginResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

        assertThat(response.role()).isEqualTo("STUDENT");
        assertThat(response.referenceId()).isEqualTo(studentId);
        assertThat(response.tenantId()).isEqualTo(tenant.toString());
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("wrong password → uniform 401 INVALID_CREDENTIALS")
    void login_wrongPassword_returns401() {
        UUID tenant = UUID.randomUUID();
        provisioning.setPassword(
                AuthCredentialProvisioningService.ROLE_STUDENT, 78L, EMAIL, tenant, PASSWORD);
        repository.flush();

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "WrongPass9#")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_CREDENTIALS")
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("STUDENT entity_type round-trips through real Postgres V89 CHECK constraint")
    void studentEntityType_roundTrips() {
        UUID tenant = UUID.randomUUID();
        AuthCredential saved = provisioning.setPassword(
                AuthCredentialProvisioningService.ROLE_STUDENT, 79L, EMAIL, tenant, PASSWORD);
        repository.flush();

        Optional<AuthCredential> reloaded = repository.findByEmailIgnoreCase(EMAIL);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getEntityType()).isEqualTo("STUDENT");
        assertThat(reloaded.get().getEntityId()).isEqualTo(79L);
        assertThat(reloaded.get().getInstanceId()).isEqualTo(tenant);
        assertThat(reloaded.get().isEnabled()).isTrue();
        assertThat(saved.getId()).isNotNull();
    }
}
