package com.kiteclass.core.module.auth.repository;

import com.kiteclass.core.module.auth.entity.AuthCredential;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for {@link AuthCredentialRepository} against REAL PostgreSQL via
 * Testcontainers, exercising the actual Flyway V89 schema (NOT Hibernate ddl-auto).
 *
 * <p><b>Why not the default test slice:</b> the kc-core {@code test} profile
 * ({@code application-test.yml}) disables Flyway and uses {@code ddl-auto: create-drop},
 * so a normal repository slice would assert against a Hibernate-generated schema — that
 * masks migration drift (per memory note + {@code postgres-specific-type-testcontainers.md}).
 * Here we run the real Flyway chain (V1..V89) once in {@code @BeforeAll}, point Spring at the
 * same container with {@code ddl-auto=none}, and exercise the DB-level constraints V89 declares
 * (global-unique email, unique user_uuid, entity_type CHECK).
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
@DisplayName("AuthCredentialRepository — real Flyway V89 schema (Testcontainers)")
class AuthCredentialPostgresIT {

    @SuppressWarnings("resource") // lifecycle managed by @Testcontainers
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("auth_credentials_it")
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
        // Apply the real production migration chain on the empty container so the
        // repository asserts against the V89-declared schema, not entity-derived DDL.
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @Autowired
    private AuthCredentialRepository repository;

    @Autowired
    private TestEntityManager em;

    private AuthCredential newCredential(String entityType, Long entityId, String email, UUID tenant) {
        return AuthCredential.builder()
                .userUuid(UUID.randomUUID())
                .entityType(entityType)
                .entityId(entityId)
                .email(email)
                .passwordHash("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUV0123")
                .instanceId(tenant)
                .enabled(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("CRUD round-trip persists + reloads (forces real SQL via flush)")
    void crudRoundTrip() {
        UUID tenant = UUID.randomUUID();
        AuthCredential saved = repository.saveAndFlush(
                newCredential("PARENT", 7L, "roundtrip@example.com", tenant));
        Long id = saved.getId();
        em.clear(); // drop persistence-context cache → next read hits the DB

        AuthCredential reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.getEntityType()).isEqualTo("PARENT");
        assertThat(reloaded.getEntityId()).isEqualTo(7L);
        assertThat(reloaded.getEmail()).isEqualTo("roundtrip@example.com");
        assertThat(reloaded.getInstanceId()).isEqualTo(tenant);
        assertThat(reloaded.isEnabled()).isTrue();
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByEmailIgnoreCase matches case-insensitively")
    void findByEmailIgnoreCase_caseInsensitive() {
        repository.saveAndFlush(newCredential("TEACHER", 11L, "Mixed.Case@Example.com", UUID.randomUUID()));
        em.clear();

        Optional<AuthCredential> found = repository.findByEmailIgnoreCase("mixed.case@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getEntityId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("findByEntityTypeAndEntityId resolves the owning credential (GAP-1013b)")
    void findByEntityTypeAndEntityId() {
        repository.saveAndFlush(newCredential("TEACHER", 99L, "entity-lookup@example.com", UUID.randomUUID()));
        em.clear();

        Optional<AuthCredential> found = repository.findByEntityTypeAndEntityId("TEACHER", 99L);
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("entity-lookup@example.com");

        assertThat(repository.findByEntityTypeAndEntityId("PARENT", 99L)).isEmpty();
    }

    @Test
    @DisplayName("global-unique email constraint (uk_auth_credentials_email) rejects duplicate")
    void uniqueEmail_enforced() {
        repository.saveAndFlush(newCredential("PARENT", 1L, "dup@example.com", UUID.randomUUID()));

        assertThatThrownBy(() ->
                repository.saveAndFlush(newCredential("TEACHER", 2L, "dup@example.com", UUID.randomUUID())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("entity_type CHECK constraint rejects value outside {PARENT,TEACHER,STUDENT}")
    void entityTypeCheck_enforced() {
        assertThatThrownBy(() ->
                repository.saveAndFlush(newCredential("OWNER", 5L, "owner@example.com", UUID.randomUUID())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
