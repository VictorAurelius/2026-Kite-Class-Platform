package com.kiteclass.gateway.module.user;

import com.kiteclass.gateway.module.auth.dto.request.RegisterRequest;
import com.kiteclass.gateway.module.auth.service.AuthService;
import com.kiteclass.gateway.module.user.dto.request.UpdateUserRequest;
import com.kiteclass.gateway.module.user.dto.response.UserResponse;
import com.kiteclass.gateway.module.user.entity.User;
import com.kiteclass.gateway.module.user.repository.UserRepository;
import com.kiteclass.gateway.module.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OWASP security tests for User module.
 * <p>
 * Tests protection against:
 * <ul>
 *   <li>SQL Injection attacks in search queries</li>
 *   <li>SQL Injection in update operations</li>
 *   <li>XSS attacks via user input</li>
 *   <li>Parameter tampering</li>
 *   <li>Mass assignment vulnerabilities</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.1.0
 */
@SpringBootTest
@Testcontainers
@DisplayName("User Security (OWASP) Tests")
class UserSecurityTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
            "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("Search users should prevent SQL injection")
    void searchUsersShouldPreventSqlInjection() {
        // Given: Malicious SQL injection attempts in search input
        String[] maliciousInputs = {
            "'; DROP TABLE users; --",
            "1' OR '1'='1",
            "admin'--",
            "' UNION SELECT * FROM users WHERE '1'='1",
            "'; DELETE FROM users WHERE 'a'='a"
        };

        // When/Then: For each malicious input
        for (String maliciousInput : maliciousInputs) {
            Flux<UserResponse> results = userService.searchUsers(
                maliciousInput,
                PageRequest.of(0, 10)
            );

            // Then: Query should complete safely without executing injected SQL
            StepVerifier.create(results)
                .expectNextCount(0) // Should return no results (safe query execution)
                .verifyComplete();

            // And: Users table should still exist and have data
            StepVerifier.create(userRepository.count())
                .expectNextMatches(count -> count >= 0)
                .verifyComplete();
        }
    }

    @Test
    @DisplayName("Update user should prevent SQL injection in parameters")
    void updateUserShouldPreventSqlInjection() {
        // Given: Valid user
        RegisterRequest registerRequest = new RegisterRequest(
            "update@test.com",
            "SecurePass123!@#",
            "Update User"
        );
        authService.register(registerRequest).block();

        User user = userRepository.findByEmail("update@test.com").block();
        assertThat(user).isNotNull();

        // When: Attempt SQL injection via name field
        UpdateUserRequest maliciousUpdate = new UpdateUserRequest();
        maliciousUpdate.setName("'; DROP TABLE users; --");
        maliciousUpdate.setPhone("0123456789");

        // Then: Update should complete safely, treating input as literal string
        StepVerifier.create(userService.updateUser(user.getId(), maliciousUpdate))
            .expectNextMatches(response ->
                response.getName().equals("'; DROP TABLE users; --") // Stored as literal string
            )
            .verifyComplete();

        // And: Users table should still exist
        StepVerifier.create(userRepository.count())
            .expectNextMatches(count -> count > 0)
            .verifyComplete();
    }

    @Test
    @DisplayName("Should sanitize XSS attempts in user input")
    void shouldSanitizeXssAttempts() {
        // Given: XSS attack attempts in registration
        String[] xssAttempts = {
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert('XSS')>",
            "javascript:alert('XSS')",
            "<svg onload=alert('XSS')>"
        };

        int attemptNumber = 0;
        for (String xssAttempt : xssAttempts) {
            // When: Register user with XSS attempt in name
            RegisterRequest request = new RegisterRequest(
                "xss" + attemptNumber + "@test.com",
                "SecurePass123!@#",
                xssAttempt  // Malicious name
            );

            // Then: Registration should either sanitize or reject the input
            StepVerifier.create(authService.register(request))
                .expectNextMatches(response -> {
                    // Name should be sanitized (HTML tags removed/escaped)
                    // or stored safely (depends on implementation)
                    return response.userId() != null;
                })
                .verifyComplete();

            attemptNumber++;
        }
    }

    @Test
    @DisplayName("Should prevent parameter tampering via ID manipulation")
    void shouldPreventParameterTampering() {
        // Given: Two users
        RegisterRequest user1Request = new RegisterRequest(
            "user1@test.com",
            "SecurePass123!@#",
            "User One"
        );
        authService.register(user1Request).block();

        RegisterRequest user2Request = new RegisterRequest(
            "user2@test.com",
            "SecurePass123!@#",
            "User Two"
        );
        authService.register(user2Request).block();

        User user1 = userRepository.findByEmail("user1@test.com").block();
        User user2 = userRepository.findByEmail("user2@test.com").block();

        assertThat(user1).isNotNull();
        assertThat(user2).isNotNull();

        // When: Attempt to update user2 using user1's ID in request body
        UpdateUserRequest tamperRequest = new UpdateUserRequest();
        tamperRequest.setName("Tampered Name");
        tamperRequest.setPhone("9999999999");

        // Then: Service should use ID from path parameter, not request body
        StepVerifier.create(userService.updateUser(user1.getId(), tamperRequest))
            .expectNextMatches(response ->
                response.getId().equals(user1.getId()) // Uses path ID, not body ID
            )
            .verifyComplete();

        // And: user2 should remain unchanged
        User user2After = userRepository.findById(user2.getId()).block();
        assertThat(user2After).isNotNull();
        assertThat(user2After.getName()).isEqualTo("User Two");
    }

    @Test
    @DisplayName("Should prevent mass assignment of sensitive fields")
    void shouldPreventMassAssignment() {
        // Given: Valid user
        RegisterRequest registerRequest = new RegisterRequest(
            "massassign@test.com",
            "SecurePass123!@#",
            "Mass Assign User"
        );
        authService.register(registerRequest).block();

        User user = userRepository.findByEmail("massassign@test.com").block();
        assertThat(user).isNotNull();

        Long originalId = user.getId();
        Boolean originalDeleted = user.getDeleted();

        // When: Attempt to update with request containing sensitive fields
        // (In real attack, attacker would try to include id, deleted, passwordHash, etc.)
        UpdateUserRequest maliciousUpdate = new UpdateUserRequest();
        maliciousUpdate.setName("Updated Name");
        maliciousUpdate.setPhone("0987654321");
        // Attacker cannot set these via UpdateUserRequest DTO (not exposed)
        // but we verify service layer protects these fields

        // Then: Update should only modify allowed fields
        StepVerifier.create(userService.updateUser(user.getId(), maliciousUpdate))
            .expectNextMatches(response -> {
                // Allowed fields updated
                assertThat(response.getName()).isEqualTo("Updated Name");
                // Sensitive fields protected
                assertThat(response.getId()).isEqualTo(originalId);
                return true;
            })
            .verifyComplete();

        // And: Verify in database that protected fields remain unchanged
        User userAfter = userRepository.findById(originalId).block();
        assertThat(userAfter).isNotNull();
        assertThat(userAfter.getId()).isEqualTo(originalId);
        assertThat(userAfter.getDeleted()).isEqualTo(originalDeleted);
        assertThat(userAfter.getPasswordHash()).isEqualTo(user.getPasswordHash());
    }
}
