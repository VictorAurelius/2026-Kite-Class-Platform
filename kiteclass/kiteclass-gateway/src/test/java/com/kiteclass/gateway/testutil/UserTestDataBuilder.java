package com.kiteclass.gateway.testutil;

import com.kiteclass.gateway.common.constant.UserStatus;
import com.kiteclass.gateway.module.user.dto.request.CreateUserRequest;
import com.kiteclass.gateway.module.user.dto.request.UpdateUserRequest;
import com.kiteclass.gateway.module.user.entity.Role;
import com.kiteclass.gateway.module.user.entity.User;
import com.kiteclass.gateway.module.user.entity.UserRole;

import java.time.Instant;
import java.util.List;

/**
 * Test data builder for User module tests.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
public class UserTestDataBuilder {

    public static User createUser(Long id, String email, String name) {
        return User.builder()
            .id(id)
            .email(email)
            .passwordHash("$2a$10$dummyHash")
            .name(name)
            .phone("0123456789")
            .status(UserStatus.ACTIVE)
            .emailVerified(true)
            .failedLoginAttempts(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .deleted(false)
            .build();
    }

    public static User createPendingUser(Long id, String email) {
        return User.builder()
            .id(id)
            .email(email)
            .passwordHash("$2a$10$dummyHash")
            .name("Test User")
            .status(UserStatus.PENDING)
            .emailVerified(false)
            .failedLoginAttempts(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .deleted(false)
            .build();
    }

    public static CreateUserRequest createUserRequest(String email, String name) {
        return CreateUserRequest.builder()
            .email(email)
            .password("Test@123")
            .name(name)
            .phone("0123456789")
            .roleIds(List.of(1L))
            .build();
    }

    public static CreateUserRequest createUserRequestWithInvalidPassword() {
        return CreateUserRequest.builder()
            .email("test@example.com")
            .password("weak")
            .name("Test User")
            .build();
    }

    public static UpdateUserRequest updateUserRequest(String name, String phone) {
        return UpdateUserRequest.builder()
            .name(name)
            .phone(phone)
            .build();
    }

    public static Role createRole(Long id, String code, String name) {
        return Role.builder()
            .id(id)
            .code(code)
            .name(name)
            .description("Description for " + name)
            .isSystem(true)
            .createdAt(Instant.now())
            .build();
    }

    public static UserRole createUserRole(Long userId, Long roleId) {
        return UserRole.builder()
            .id(1L)
            .userId(userId)
            .roleId(roleId)
            .assignedAt(Instant.now())
            .build();
    }
}
