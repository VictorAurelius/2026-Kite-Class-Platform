package com.kiteclass.gateway.module.user.repository;

import com.kiteclass.gateway.module.user.entity.User;
import com.kiteclass.gateway.testutil.UserTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UserRepository using Testcontainers.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@DataR2dbcTest
@Testcontainers
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

    @Container
    @SuppressWarnings("resource") // Managed by Testcontainers
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://"
            + postgres.getHost() + ":" + postgres.getFirstMappedPort()
            + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll().block();
    }

    @Test
    @DisplayName("save() should persist user to database")
    void save_shouldPersistUser() {
        // given
        User user = UserTestDataBuilder.createUser(null, "test@example.com", "Test User");

        // when
        StepVerifier.create(userRepository.save(user))
            // then
            .assertNext(savedUser -> {
                assertThat(savedUser.getId()).isNotNull();
                assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
                assertThat(savedUser.getName()).isEqualTo("Test User");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("findByEmailAndDeletedFalse() should return user when exists")
    void findByEmail_shouldReturnUser() {
        // given
        User user = UserTestDataBuilder.createUser(null, "find@example.com", "Find User");
        userRepository.save(user).block();

        // when
        StepVerifier.create(userRepository.findByEmailAndDeletedFalse("find@example.com"))
            // then
            .assertNext(foundUser -> {
                assertThat(foundUser.getEmail()).isEqualTo("find@example.com");
                assertThat(foundUser.getName()).isEqualTo("Find User");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("findByEmailAndDeletedFalse() should not return deleted user")
    void findByEmail_shouldNotReturnDeletedUser() {
        // given
        User user = UserTestDataBuilder.createUser(null, "deleted@example.com", "Deleted User");
        user.setDeleted(true);
        userRepository.save(user).block();

        // when & then
        StepVerifier.create(userRepository.findByEmailAndDeletedFalse("deleted@example.com"))
            .verifyComplete();
    }

    @Test
    @DisplayName("existsByEmailAndDeletedFalse() should return true when email exists")
    void existsByEmail_shouldReturnTrue() {
        // given
        User user = UserTestDataBuilder.createUser(null, "exists@example.com", "Exists User");
        userRepository.save(user).block();

        // when & then
        StepVerifier.create(userRepository.existsByEmailAndDeletedFalse("exists@example.com"))
            .assertNext(exists -> assertThat(exists).isTrue())
            .verifyComplete();
    }

    @Test
    @DisplayName("existsByEmailAndDeletedFalse() should return false when email not exists")
    void existsByEmail_shouldReturnFalse() {
        // when & then
        StepVerifier.create(userRepository.existsByEmailAndDeletedFalse("notexists@example.com"))
            .assertNext(exists -> assertThat(exists).isFalse())
            .verifyComplete();
    }

    @Test
    @DisplayName("findByIdAndDeletedFalse() should return user when exists")
    void findById_shouldReturnUser() {
        // given
        User user = UserTestDataBuilder.createUser(null, "id@example.com", "ID User");
        User savedUser = userRepository.save(user).block();

        // when & then
        StepVerifier.create(userRepository.findByIdAndDeletedFalse(savedUser.getId()))
            .assertNext(foundUser -> {
                assertThat(foundUser.getId()).isEqualTo(savedUser.getId());
                assertThat(foundUser.getEmail()).isEqualTo("id@example.com");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("findBySearchCriteria() should return matching users")
    void findBySearchCriteria_shouldReturnMatchingUsers() {
        // given
        User user1 = UserTestDataBuilder.createUser(null, "john@example.com", "John Doe");
        User user2 = UserTestDataBuilder.createUser(null, "jane@example.com", "Jane Smith");
        userRepository.save(user1).block();
        userRepository.save(user2).block();

        // when & then
        StepVerifier.create(userRepository.findBySearchCriteria("%john%", 10, 0))
            .assertNext(user -> assertThat(user.getName()).contains("John"))
            .verifyComplete();
    }

    @Test
    @DisplayName("countBySearchCriteria() should return correct count")
    void countBySearchCriteria_shouldReturnCount() {
        // given
        User user1 = UserTestDataBuilder.createUser(null, "count1@example.com", "Count User 1");
        User user2 = UserTestDataBuilder.createUser(null, "count2@example.com", "Count User 2");
        userRepository.save(user1).block();
        userRepository.save(user2).block();

        // when & then
        StepVerifier.create(userRepository.countBySearchCriteria("%%"))
            .assertNext(count -> assertThat(count).isEqualTo(2L))
            .verifyComplete();
    }
}
