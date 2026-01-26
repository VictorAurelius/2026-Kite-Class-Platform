package com.kiteclass.gateway.module.user.service;

import com.kiteclass.gateway.common.exception.DuplicateResourceException;
import com.kiteclass.gateway.common.exception.EntityNotFoundException;
import com.kiteclass.gateway.module.user.dto.request.CreateUserRequest;
import com.kiteclass.gateway.module.user.dto.request.UpdateUserRequest;
import com.kiteclass.gateway.module.user.dto.response.UserResponse;
import com.kiteclass.gateway.module.user.entity.Role;
import com.kiteclass.gateway.module.user.entity.User;
import com.kiteclass.gateway.module.user.entity.UserRole;
import com.kiteclass.gateway.module.user.mapper.UserMapper;
import com.kiteclass.gateway.module.user.repository.RoleRepository;
import com.kiteclass.gateway.module.user.repository.UserRepository;
import com.kiteclass.gateway.module.user.repository.UserRoleRepository;
import com.kiteclass.gateway.module.user.service.impl.UserServiceImpl;
import com.kiteclass.gateway.testutil.UserTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private Role testRole;
    private CreateUserRequest createRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        testUser = UserTestDataBuilder.createUser(1L, "test@example.com", "Test User");
        testRole = UserTestDataBuilder.createRole(1L, "ADMIN", "Administrator");
        createRequest = UserTestDataBuilder.createUserRequest("test@example.com", "Test User");
        userResponse = UserResponse.builder()
            .id(1L)
            .email("test@example.com")
            .name("Test User")
            .build();
    }

    @Test
    @DisplayName("createUser() should create user successfully")
    void createUser_shouldCreateUserSuccessfully() {
        // given
        when(userRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(Mono.just(false));
        when(userMapper.toEntity(any(CreateUserRequest.class))).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));
        when(roleRepository.findById(anyLong())).thenReturn(Mono.just(testRole));
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(Mono.just(new UserRole()));
        when(userRoleRepository.findRolesByUserId(anyLong())).thenReturn(Flux.just(testRole));
        when(userMapper.toResponseWithRoles(any(User.class), anyList())).thenReturn(userResponse);

        // when
        Mono<UserResponse> result = userService.createUser(createRequest);

        // then
        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response).isNotNull();
                assertThat(response.getEmail()).isEqualTo("test@example.com");
            })
            .verifyComplete();

        verify(userRepository).existsByEmailAndDeletedFalse("test@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser() should throw DuplicateResourceException when email exists")
    void createUser_shouldThrowDuplicateException() {
        // given
        when(userRepository.existsByEmailAndDeletedFalse(anyString())).thenReturn(Mono.just(true));

        // when
        Mono<UserResponse> result = userService.createUser(createRequest);

        // then
        StepVerifier.create(result)
            .expectError(DuplicateResourceException.class)
            .verify();

        verify(userRepository).existsByEmailAndDeletedFalse("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("getUserById() should return user when found")
    void getUserById_shouldReturnUser() {
        // given
        when(userRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Mono.just(testUser));
        when(userRoleRepository.findRolesByUserId(anyLong())).thenReturn(Flux.just(testRole));
        when(userMapper.toResponseWithRoles(any(User.class), anyList())).thenReturn(userResponse);

        // when
        Mono<UserResponse> result = userService.getUserById(1L);

        // then
        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response).isNotNull();
                assertThat(response.getId()).isEqualTo(1L);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("getUserById() should throw EntityNotFoundException when not found")
    void getUserById_shouldThrowNotFoundException() {
        // given
        when(userRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Mono.empty());

        // when
        Mono<UserResponse> result = userService.getUserById(999L);

        // then
        StepVerifier.create(result)
            .expectError(EntityNotFoundException.class)
            .verify();
    }

    @Test
    @DisplayName("updateUser() should update user successfully")
    void updateUser_shouldUpdateSuccessfully() {
        // given
        UpdateUserRequest updateRequest = UserTestDataBuilder.updateUserRequest("Updated Name", "0987654321");
        when(userRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Mono.just(testUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));
        when(userRoleRepository.findRolesByUserId(anyLong())).thenReturn(Flux.just(testRole));
        when(userMapper.toResponseWithRoles(any(User.class), anyList())).thenReturn(userResponse);

        // when
        Mono<UserResponse> result = userService.updateUser(1L, updateRequest);

        // then
        StepVerifier.create(result)
            .assertNext(response -> assertThat(response).isNotNull())
            .verifyComplete();

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("deleteUser() should soft delete user")
    void deleteUser_shouldSoftDeleteUser() {
        // given
        when(userRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Mono.just(testUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

        // when
        Mono<Void> result = userService.deleteUser(1L);

        // then
        StepVerifier.create(result)
            .verifyComplete();

        verify(userRepository).save(argThat(user ->
            Boolean.TRUE.equals(user.getDeleted()) && user.getDeletedAt() != null
        ));
    }

    @Test
    @DisplayName("getUsers() should return paginated users")
    void getUsers_shouldReturnPaginatedUsers() {
        // given
        when(userRepository.findBySearchCriteria(anyString(), anyInt(), anyLong()))
            .thenReturn(Flux.just(testUser));
        when(userRoleRepository.findRolesByUserId(anyLong())).thenReturn(Flux.just(testRole));
        when(userMapper.toResponseWithRoles(any(User.class), anyList())).thenReturn(userResponse);

        // when
        Flux<UserResponse> result = userService.getUsers("test", 0, 20);

        // then
        StepVerifier.create(result)
            .assertNext(response -> assertThat(response).isNotNull())
            .verifyComplete();
    }

    @Test
    @DisplayName("countUsers() should return total count")
    void countUsers_shouldReturnCount() {
        // given
        when(userRepository.countBySearchCriteria(anyString())).thenReturn(Mono.just(5L));

        // when
        Mono<Long> result = userService.countUsers("test");

        // then
        StepVerifier.create(result)
            .assertNext(count -> assertThat(count).isEqualTo(5L))
            .verifyComplete();
    }
}
