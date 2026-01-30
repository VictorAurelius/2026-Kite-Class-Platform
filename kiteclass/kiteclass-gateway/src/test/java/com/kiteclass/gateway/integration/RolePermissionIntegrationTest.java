package com.kiteclass.gateway.integration;

import com.kiteclass.gateway.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Collections;
import java.util.List;

/**
 * Integration tests for role-based access control (RBAC).
 * Tests that endpoints are properly protected with role requirements.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DisplayName("Role & Permission Integration Tests")
@org.junit.jupiter.api.Disabled("Requires PostgreSQL Testcontainers - Docker not available in WSL")
class RolePermissionIntegrationTest {

    @SuppressWarnings("resource") // Managed by Testcontainers framework


    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("OWNER role should have access to all user endpoints")
    void ownerShouldHaveAccessToAllEndpoints() {
        // Given
        String ownerToken = jwtTokenProvider.generateAccessToken(
                1L,
                "owner@example.com",
                Collections.singletonList("OWNER")
        );

        // When/Then - OWNER can list users
        webTestClient.get()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isOk();

        // OWNER can get user by ID
        webTestClient.get()
                .uri("/api/v1/users/1")
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isOk();

        // OWNER can delete users
        webTestClient.delete()
                .uri("/api/v1/users/999") // Non-existent user (404 expected, not 403)
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isNotFound(); // Has permission, but user doesn't exist
    }

    @Test
    @DisplayName("ADMIN role should have access to user management except DELETE")
    void adminShouldHaveLimitedAccess() {
        // Given
        String adminToken = jwtTokenProvider.generateAccessToken(
                2L,
                "admin@example.com",
                Collections.singletonList("ADMIN")
        );

        // When/Then - ADMIN can list users
        webTestClient.get()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk();

        // ADMIN can get user by ID
        webTestClient.get()
                .uri("/api/v1/users/1")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk();

        // ADMIN cannot delete users (OWNER only)
        webTestClient.delete()
                .uri("/api/v1/users/1")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("STAFF role should have read-only access to users")
    void staffShouldHaveReadOnlyAccess() {
        // Given
        String staffToken = jwtTokenProvider.generateAccessToken(
                3L,
                "staff@example.com",
                Collections.singletonList("STAFF")
        );

        // When/Then - STAFF can list users
        webTestClient.get()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + staffToken)
                .exchange()
                .expectStatus().isOk();

        // STAFF can get user by ID
        webTestClient.get()
                .uri("/api/v1/users/1")
                .header("Authorization", "Bearer " + staffToken)
                .exchange()
                .expectStatus().isOk();

        // STAFF cannot create users
        String createUserJson = "{\"email\":\"new@example.com\",\"name\":\"New User\",\"password\":\"Test@123\",\"roleId\":4}";
        webTestClient.post()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + staffToken)
                .header("Content-Type", "application/json")
                .bodyValue(createUserJson)
                .exchange()
                .expectStatus().isForbidden();

        // STAFF cannot delete users
        webTestClient.delete()
                .uri("/api/v1/users/1")
                .header("Authorization", "Bearer " + staffToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("TEACHER role should not have access to user management")
    void teacherShouldNotHaveUserAccess() {
        // Given
        String teacherToken = jwtTokenProvider.generateAccessToken(
                4L,
                "teacher@example.com",
                Collections.singletonList("TEACHER")
        );

        // When/Then - TEACHER cannot list users
        webTestClient.get()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + teacherToken)
                .exchange()
                .expectStatus().isForbidden();

        // TEACHER cannot get user by ID
        webTestClient.get()
                .uri("/api/v1/users/1")
                .header("Authorization", "Bearer " + teacherToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("PARENT role should not have access to user management")
    void parentShouldNotHaveUserAccess() {
        // Given
        String parentToken = jwtTokenProvider.generateAccessToken(
                5L,
                "parent@example.com",
                Collections.singletonList("PARENT")
        );

        // When/Then - PARENT cannot list users
        webTestClient.get()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + parentToken)
                .exchange()
                .expectStatus().isForbidden();

        // PARENT cannot create users
        String createUserJson = "{\"email\":\"new@example.com\",\"name\":\"New User\",\"password\":\"Test@123\",\"roleId\":4}";
        webTestClient.post()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + parentToken)
                .header("Content-Type", "application/json")
                .bodyValue(createUserJson)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("User with multiple roles should have combined permissions")
    void multipleRolesShouldCombinePermissions() {
        // Given - User with both ADMIN and STAFF roles
        String multiRoleToken = jwtTokenProvider.generateAccessToken(
                6L,
                "multi@example.com",
                List.of("ADMIN", "STAFF")
        );

        // When/Then - Should have ADMIN permissions (highest role)
        webTestClient.get()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + multiRoleToken)
                .exchange()
                .expectStatus().isOk();

        // Can create users (ADMIN permission)
        String createUserJson = "{\"email\":\"new@example.com\",\"name\":\"New User\",\"password\":\"Test@123\",\"roleId\":4}";
        webTestClient.post()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + multiRoleToken)
                .header("Content-Type", "application/json")
                .bodyValue(createUserJson)
                .exchange()
                .expectStatus().is2xxSuccessful(); // Should succeed (201 Created)
    }

    @Test
    @DisplayName("Auth endpoints should be accessible without role (public)")
    void authEndpointsShouldBePublic() {
        // When/Then - Login endpoint should be accessible without token
        String loginJson = "{\"email\":\"owner@kiteclass.local\",\"password\":\"Admin@123\"}";

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .header("Content-Type", "application/json")
                .bodyValue(loginJson)
                .exchange()
                .expectStatus().isOk();

        // Forgot password should be accessible
        String forgotPasswordJson = "{\"email\":\"owner@kiteclass.local\"}";
        webTestClient.post()
                .uri("/api/v1/auth/forgot-password")
                .header("Content-Type", "application/json")
                .bodyValue(forgotPasswordJson)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Invalid role should not grant any access")
    void invalidRoleShouldNotGrantAccess() {
        // Given - Token with invalid/unknown role
        String invalidRoleToken = jwtTokenProvider.generateAccessToken(
                99L,
                "invalid@example.com",
                Collections.singletonList("INVALID_ROLE")
        );

        // When/Then - Should not have access to protected endpoints
        webTestClient.get()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + invalidRoleToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Token without roles should not grant access to protected endpoints")
    void tokenWithoutRolesShouldNotGrantAccess() {
        // Given - Token with empty roles list
        String noRoleToken = jwtTokenProvider.generateAccessToken(
                99L,
                "norole@example.com",
                Collections.emptyList()
        );

        // When/Then - Should not have access
        webTestClient.get()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + noRoleToken)
                .exchange()
                .expectStatus().isForbidden();
    }
}
