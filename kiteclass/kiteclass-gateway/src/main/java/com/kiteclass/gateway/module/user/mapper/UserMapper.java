package com.kiteclass.gateway.module.user.mapper;

import com.kiteclass.gateway.common.constant.UserStatus;
import com.kiteclass.gateway.module.user.dto.request.CreateUserRequest;
import com.kiteclass.gateway.module.user.dto.response.RoleResponse;
import com.kiteclass.gateway.module.user.dto.response.UserResponse;
import com.kiteclass.gateway.module.user.entity.Role;
import com.kiteclass.gateway.module.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.Instant;
import java.util.List;

/**
 * MapStruct mapper for User entity and DTOs.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    /**
     * Maps CreateUserRequest to User entity.
     *
     * @param request CreateUserRequest
     * @return User entity with default values
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", source = "password")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "emailVerified", constant = "false")
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "failedLoginAttempts", constant = "0")
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "deletedAt", ignore = true)
    User toEntity(CreateUserRequest request);

    /**
     * Maps User entity to UserResponse without roles.
     *
     * @param user User entity
     * @return UserResponse without roles
     */
    @Mapping(target = "roles", ignore = true)
    UserResponse toResponse(User user);

    /**
     * Maps User entity to UserResponse with roles.
     *
     * @param user  User entity
     * @param roles List of Role entities
     * @return UserResponse with roles
     */
    default UserResponse toResponseWithRoles(User user, List<Role> roles) {
        UserResponse response = toResponse(user);
        response.setRoles(roles.stream().map(this::toRoleResponse).toList());
        return response;
    }

    /**
     * Maps Role entity to RoleResponse.
     *
     * @param role Role entity
     * @return RoleResponse
     */
    RoleResponse toRoleResponse(Role role);
}
