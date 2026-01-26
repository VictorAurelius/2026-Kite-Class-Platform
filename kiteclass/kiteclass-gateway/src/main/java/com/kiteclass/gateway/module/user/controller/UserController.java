package com.kiteclass.gateway.module.user.controller;

import com.kiteclass.gateway.common.constant.MessageCodes;
import com.kiteclass.gateway.common.dto.ApiResponse;
import com.kiteclass.gateway.common.dto.PageResponse;
import com.kiteclass.gateway.common.service.MessageService;
import com.kiteclass.gateway.module.user.dto.request.CreateUserRequest;
import com.kiteclass.gateway.module.user.dto.request.UpdateUserRequest;
import com.kiteclass.gateway.module.user.dto.response.UserResponse;
import com.kiteclass.gateway.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for user management operations.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MessageService messageService;

    /**
     * Create a new user.
     *
     * @param request CreateUserRequest
     * @return ApiResponse with created UserResponse
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("POST /api/v1/users - Creating user with email: {}", request.getEmail());
        return userService.createUser(request)
            .map(user -> {
                String message = messageService.getMessage(MessageCodes.SUCCESS_CREATED);
                return ApiResponse.success(user, message);
            });
    }

    /**
     * Get user by ID.
     *
     * @param id user ID
     * @return ApiResponse with UserResponse
     */
    @GetMapping("/{id}")
    public Mono<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        log.info("GET /api/v1/users/{} - Fetching user", id);
        return userService.getUserById(id)
            .map(ApiResponse::success);
    }

    /**
     * Get paginated list of users with optional search.
     *
     * @param search optional search term (name or email)
     * @param page   page number (default: 0)
     * @param size   page size (default: 20)
     * @return ApiResponse with PageResponse of users
     */
    @GetMapping
    public Mono<ApiResponse<PageResponse<UserResponse>>> getUsers(
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/v1/users - search: {}, page: {}, size: {}", search, page, size);

        return userService.countUsers(search)
            .flatMap(total -> userService.getUsers(search, page, size)
                .collectList()
                .map(users -> PageResponse.of(users, page, size, total))
                .map(ApiResponse::success));
    }

    /**
     * Update user information.
     *
     * @param id      user ID
     * @param request UpdateUserRequest
     * @return ApiResponse with updated UserResponse
     */
    @PutMapping("/{id}")
    public Mono<ApiResponse<UserResponse>> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UpdateUserRequest request) {

        log.info("PUT /api/v1/users/{} - Updating user", id);
        return userService.updateUser(id, request)
            .map(user -> {
                String message = messageService.getMessage(MessageCodes.SUCCESS_UPDATED);
                return ApiResponse.success(user, message);
            });
    }

    /**
     * Soft delete a user.
     *
     * @param id user ID
     * @return ApiResponse with success message
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteUser(@PathVariable Long id) {
        log.info("DELETE /api/v1/users/{} - Deleting user", id);
        return userService.deleteUser(id);
    }
}
