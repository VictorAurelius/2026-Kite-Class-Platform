package com.kiteclass.gateway.module.user.repository;

import com.kiteclass.gateway.module.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link UserRepository} with real database and Flyway migrations.
 * Tests repository operations with data from migrations (like default owner account).
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("UserRepository Integration Tests")
class UserRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // R2DBC configuration
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/test");
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);

        // Flyway configuration (uses JDBC)
        registry.add("spring.flyway.url", () ->
                "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/test");
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);

        // Disable Redis for tests
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("findByEmail() should find default owner account from V4 migration")
    void shouldFindDefaultOwnerAccount() {
        // When - Default owner created by V4__create_auth_module.sql
        StepVerifier.create(userRepository.findByEmail("owner@kiteclass.local"))
                // Then
                .assertNext(user -> {
                    assertThat(user.getEmail()).isEqualTo("owner@kiteclass.local");
                    assertThat(user.getName()).isEqualTo("System Owner");
                    assertThat(user.getDeleted()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("findByEmailAndDeletedFalse() should find owner account")
    void shouldFindOwnerAccountWhenNotDeleted() {
        // When
        StepVerifier.create(userRepository.findByEmailAndDeletedFalse("owner@kiteclass.local"))
                // Then
                .assertNext(user -> {
                    assertThat(user.getEmail()).isEqualTo("owner@kiteclass.local");
                    assertThat(user.getName()).isEqualTo("System Owner");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("existsByEmailAndDeletedFalse() should return true for owner account")
    void shouldReturnTrueForExistingEmail() {
        // When & Then
        StepVerifier.create(userRepository.existsByEmailAndDeletedFalse("owner@kiteclass.local"))
                .assertNext(exists -> assertThat(exists).isTrue())
                .verifyComplete();
    }

    @Test
    @DisplayName("existsByEmailAndDeletedFalse() should return false for non-existing email")
    void shouldReturnFalseForNonExistingEmail() {
        // When & Then
        StepVerifier.create(userRepository.existsByEmailAndDeletedFalse("notexists@example.com"))
                .assertNext(exists -> assertThat(exists).isFalse())
                .verifyComplete();
    }

    @Test
    @DisplayName("findByIdAndDeletedFalse() should find owner by ID")
    void shouldFindOwnerById() {
        // Given - Get owner ID first
        Long ownerId = userRepository.findByEmail("owner@kiteclass.local")
                .map(User::getId)
                .block();

        // When & Then
        StepVerifier.create(userRepository.findByIdAndDeletedFalse(ownerId))
                .assertNext(user -> {
                    assertThat(user.getId()).isEqualTo(ownerId);
                    assertThat(user.getEmail()).isEqualTo("owner@kiteclass.local");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("findBySearchCriteria() should find owner by name search")
    void shouldFindOwnerByNameSearch() {
        // When - Search for "Owner"
        StepVerifier.create(userRepository.findBySearchCriteria("%Owner%", 10, 0))
                .assertNext(user -> {
                    assertThat(user.getName()).contains("Owner");
                    assertThat(user.getEmail()).isEqualTo("owner@kiteclass.local");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("findBySearchCriteria() should find owner by email search")
    void shouldFindOwnerByEmailSearch() {
        // When - Search for "kiteclass"
        StepVerifier.create(userRepository.findBySearchCriteria("%kiteclass%", 10, 0))
                .assertNext(user -> {
                    assertThat(user.getEmail()).contains("kiteclass");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("countBySearchCriteria() should return at least 1 (owner account)")
    void shouldCountAtLeastOneUser() {
        // When & Then - At least owner account exists
        StepVerifier.create(userRepository.countBySearchCriteria("%%"))
                .assertNext(count -> assertThat(count).isGreaterThanOrEqualTo(1L))
                .verifyComplete();
    }

    @Test
    @DisplayName("save() should persist new user to database")
    void shouldSaveNewUser() {
        // Given
        User newUser = new User();
        newUser.setEmail("newuser@example.com");
        newUser.setName("New User");
        newUser.setPasswordHash("hashed-password");
        newUser.setStatus("ACTIVE");
        newUser.setDeleted(false);

        // When
        StepVerifier.create(userRepository.save(newUser))
                // Then
                .assertNext(savedUser -> {
                    assertThat(savedUser.getId()).isNotNull();
                    assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
                    assertThat(savedUser.getName()).isEqualTo("New User");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("save() should update existing user")
    void shouldUpdateExistingUser() {
        // Given - Get owner account
        User owner = userRepository.findByEmail("owner@kiteclass.local").block();
        assertThat(owner).isNotNull();

        // Update name
        owner.setName("Updated Owner Name");

        // When
        StepVerifier.create(userRepository.save(owner))
                // Then
                .assertNext(updatedUser -> {
                    assertThat(updatedUser.getId()).isEqualTo(owner.getId());
                    assertThat(updatedUser.getName()).isEqualTo("Updated Owner Name");
                    assertThat(updatedUser.getEmail()).isEqualTo("owner@kiteclass.local");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("findAll() should return at least owner account")
    void shouldFindAllUsers() {
        // When & Then
        StepVerifier.create(userRepository.findAll())
                .expectNextMatches(user -> user.getEmail().equals("owner@kiteclass.local"))
                .thenConsumeWhile(user -> true) // Consume any additional users
                .verifyComplete();
    }

    @Test
    @DisplayName("count() should return at least 1 (owner account)")
    void shouldCountAllUsers() {
        // When & Then
        StepVerifier.create(userRepository.count())
                .assertNext(count -> assertThat(count).isGreaterThanOrEqualTo(1L))
                .verifyComplete();
    }
}
