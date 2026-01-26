package com.kiteclass.gateway.module.user.service.impl;

import com.kiteclass.gateway.common.exception.DuplicateResourceException;
import com.kiteclass.gateway.common.exception.EntityNotFoundException;
import com.kiteclass.gateway.module.user.dto.request.CreateUserRequest;
import com.kiteclass.gateway.module.user.dto.request.UpdateUserRequest;
import com.kiteclass.gateway.module.user.dto.response.UserResponse;
import com.kiteclass.gateway.module.user.entity.User;
import com.kiteclass.gateway.module.user.entity.UserRole;
import com.kiteclass.gateway.module.user.mapper.UserMapper;
import com.kiteclass.gateway.module.user.repository.RoleRepository;
import com.kiteclass.gateway.module.user.repository.UserRepository;
import com.kiteclass.gateway.module.user.repository.UserRoleRepository;
import com.kiteclass.gateway.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Implementation of UserService.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Mono<UserResponse> createUser(CreateUserRequest request) {
        log.debug("Creating user with email: {}", request.getEmail());

        return userRepository.existsByEmailAndDeletedFalse(request.getEmail())
            .flatMap(exists -> {
                if (Boolean.TRUE.equals(exists)) {
                    return Mono.error(new DuplicateResourceException("Email", request.getEmail()));
                }

                User user = userMapper.toEntity(request);
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

                return userRepository.save(user)
                    .flatMap(savedUser -> assignRoles(savedUser.getId(), request.getRoleIds())
                        .then(loadUserWithRoles(savedUser.getId())))
                    .doOnSuccess(u -> log.info("User created successfully: id={}, email={}", u.getId(), u.getEmail()));
            });
    }

    @Override
    public Mono<UserResponse> getUserById(Long id) {
        log.debug("Fetching user by id: {}", id);
        return loadUserWithRoles(id);
    }

    @Override
    public Mono<UserResponse> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmailAndDeletedFalse(email)
            .switchIfEmpty(Mono.error(new EntityNotFoundException("User", "email", email)))
            .flatMap(user -> loadUserWithRoles(user.getId()));
    }

    @Override
    public Flux<UserResponse> getUsers(String searchTerm, int page, int size) {
        log.debug("Fetching users with search term: {}, page: {}, size: {}", searchTerm, page, size);
        String search = searchTerm != null ? "%" + searchTerm + "%" : "%%";
        long offset = (long) page * size;

        return userRepository.findBySearchCriteria(search, size, offset)
            .flatMap(user -> userRoleRepository.findRolesByUserId(user.getId())
                .collectList()
                .map(roles -> userMapper.toResponseWithRoles(user, roles)));
    }

    @Override
    public Mono<Long> countUsers(String searchTerm) {
        String search = searchTerm != null ? "%" + searchTerm + "%" : "%%";
        return userRepository.countBySearchCriteria(search);
    }

    @Override
    @Transactional
    public Mono<UserResponse> updateUser(Long id, UpdateUserRequest request) {
        log.debug("Updating user: {}", id);

        return userRepository.findByIdAndDeletedFalse(id)
            .switchIfEmpty(Mono.error(new EntityNotFoundException("User", id)))
            .flatMap(user -> {
                if (request.getName() != null) {
                    user.setName(request.getName());
                }
                if (request.getPhone() != null) {
                    user.setPhone(request.getPhone());
                }
                if (request.getAvatarUrl() != null) {
                    user.setAvatarUrl(request.getAvatarUrl());
                }
                if (request.getStatus() != null) {
                    user.setStatus(request.getStatus());
                }
                user.setUpdatedAt(Instant.now());

                return userRepository.save(user)
                    .flatMap(savedUser -> {
                        if (request.getRoleIds() != null) {
                            return userRoleRepository.deleteByUserId(savedUser.getId())
                                .then(assignRoles(savedUser.getId(), request.getRoleIds()))
                                .then(loadUserWithRoles(savedUser.getId()));
                        } else {
                            return loadUserWithRoles(savedUser.getId());
                        }
                    })
                    .doOnSuccess(u -> log.info("User updated successfully: id={}", u.getId()));
            });
    }

    @Override
    @Transactional
    public Mono<Void> deleteUser(Long id) {
        log.debug("Soft deleting user: {}", id);

        return userRepository.findByIdAndDeletedFalse(id)
            .switchIfEmpty(Mono.error(new EntityNotFoundException("User", id)))
            .flatMap(user -> {
                user.setDeleted(true);
                user.setDeletedAt(Instant.now());
                return userRepository.save(user);
            })
            .doOnSuccess(u -> log.info("User soft deleted successfully: id={}", id))
            .then();
    }

    /**
     * Load user with roles.
     */
    private Mono<UserResponse> loadUserWithRoles(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
            .switchIfEmpty(Mono.error(new EntityNotFoundException("User", userId)))
            .flatMap(user -> userRoleRepository.findRolesByUserId(userId)
                .collectList()
                .map(roles -> userMapper.toResponseWithRoles(user, roles)));
    }

    /**
     * Assign roles to a user.
     */
    private Mono<Void> assignRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(roleIds)
            .flatMap(roleId -> roleRepository.findById(roleId)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Role", roleId)))
                .map(role -> UserRole.builder()
                    .userId(userId)
                    .roleId(roleId)
                    .assignedAt(Instant.now())
                    .build()))
            .flatMap(userRoleRepository::save)
            .then();
    }
}
