package com.kiteclass.gateway.module.user.service;

import com.kiteclass.gateway.module.user.dto.request.CreateUserRequest;
import com.kiteclass.gateway.module.user.dto.request.UpdateUserRequest;
import com.kiteclass.gateway.module.user.dto.response.UserResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service interface for user management operations.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
public interface UserService {

    /**
     * Create a new user with roles.
     *
     * @param request CreateUserRequest
     * @return Mono of UserResponse
     */
    Mono<UserResponse> createUser(CreateUserRequest request);

    /**
     * Get user by ID.
     *
     * @param id user ID
     * @return Mono of UserResponse
     */
    Mono<UserResponse> getUserById(Long id);

    /**
     * Get user by email.
     *
     * @param email user email
     * @return Mono of UserResponse
     */
    Mono<UserResponse> getUserByEmail(String email);

    /**
     * Get paginated list of users with optional search.
     *
     * @param searchTerm optional search term (name or email)
     * @param page       page number (0-indexed)
     * @param size       page size
     * @return Flux of UserResponse
     */
    Flux<UserResponse> getUsers(String searchTerm, int page, int size);

    /**
     * Count users matching search criteria.
     *
     * @param searchTerm optional search term
     * @return Mono of total count
     */
    Mono<Long> countUsers(String searchTerm);

    /**
     * Update user information and roles.
     *
     * @param id      user ID
     * @param request UpdateUserRequest
     * @return Mono of updated UserResponse
     */
    Mono<UserResponse> updateUser(Long id, UpdateUserRequest request);

    /**
     * Soft delete a user.
     *
     * @param id user ID
     * @return Mono of Void
     */
    Mono<Void> deleteUser(Long id);
}
