package com.kiteclass.gateway.module.user.controller;

import com.kiteclass.gateway.common.constant.MessageCodes;
import com.kiteclass.gateway.common.exception.DuplicateResourceException;
import com.kiteclass.gateway.common.exception.EntityNotFoundException;
import com.kiteclass.gateway.common.service.MessageService;
import com.kiteclass.gateway.module.user.dto.request.CreateUserRequest;
import com.kiteclass.gateway.module.user.dto.request.UpdateUserRequest;
import com.kiteclass.gateway.module.user.dto.response.UserResponse;
import com.kiteclass.gateway.module.user.service.UserService;
import com.kiteclass.gateway.testutil.UserTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * WebFlux tests for UserController.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@WebFluxTest(controllers = UserController.class)
@ActiveProfiles("test")
@DisplayName("UserController Tests")
@WithMockUser
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private MessageService messageService;

    private UserResponse userResponse;
    private CreateUserRequest createRequest;

    @BeforeEach
    void setUp() {
        userResponse = UserResponse.builder()
            .id(1L)
            .email("test@example.com")
            .name("Test User")
            .phone("0123456789")
            .build();

        createRequest = UserTestDataBuilder.createUserRequest("test@example.com", "Test User");

        when(messageService.getMessage(MessageCodes.SUCCESS_CREATED))
            .thenReturn("Tạo người dùng thành công");
        when(messageService.getMessage(MessageCodes.SUCCESS_UPDATED))
            .thenReturn("Cập nhật thành công");
    }

    @Test
    @DisplayName("POST /api/v1/users should create user and return 201")
    void createUser_shouldReturn201() {
        // given
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(Mono.just(userResponse));

        // when & then
        webTestClient.post()
            .uri("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.id").isEqualTo(1)
            .jsonPath("$.data.email").isEqualTo("test@example.com")
            .jsonPath("$.message").isEqualTo("Tạo người dùng thành công");
    }

    @Test
    @DisplayName("POST /api/v1/users should return 409 when email exists")
    void createUser_shouldReturn409WhenDuplicate() {
        // given
        when(userService.createUser(any(CreateUserRequest.class)))
            .thenReturn(Mono.error(new DuplicateResourceException("Email", "test@example.com")));

        // when & then
        webTestClient.post()
            .uri("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} should return user")
    void getUserById_shouldReturnUser() {
        // given
        when(userService.getUserById(anyLong())).thenReturn(Mono.just(userResponse));

        // when & then
        webTestClient.get()
            .uri("/api/v1/users/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.id").isEqualTo(1)
            .jsonPath("$.data.email").isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} should return 404 when not found")
    void getUserById_shouldReturn404WhenNotFound() {
        // given
        when(userService.getUserById(anyLong()))
            .thenReturn(Mono.error(new EntityNotFoundException("User", 999L)));

        // when & then
        webTestClient.get()
            .uri("/api/v1/users/999")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GET /api/v1/users should return paginated users")
    void getUsers_shouldReturnPaginatedList() {
        // given
        when(userService.countUsers(anyString())).thenReturn(Mono.just(1L));
        when(userService.getUsers(anyString(), anyInt(), anyInt()))
            .thenReturn(Flux.just(userResponse));

        // when & then
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/v1/users")
                .queryParam("search", "test")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.content[0].email").isEqualTo("test@example.com")
            .jsonPath("$.data.page").isEqualTo(0)
            .jsonPath("$.data.size").isEqualTo(20)
            .jsonPath("$.data.totalElements").isEqualTo(1);
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} should update user")
    void updateUser_shouldUpdateSuccessfully() {
        // given
        UpdateUserRequest updateRequest = UserTestDataBuilder.updateUserRequest("Updated Name", "0987654321");
        UserResponse updatedResponse = UserResponse.builder()
            .id(1L)
            .email("test@example.com")
            .name("Updated Name")
            .phone("0987654321")
            .build();

        when(userService.updateUser(anyLong(), any(UpdateUserRequest.class)))
            .thenReturn(Mono.just(updatedResponse));

        // when & then
        webTestClient.put()
            .uri("/api/v1/users/1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.name").isEqualTo("Updated Name")
            .jsonPath("$.message").isEqualTo("Cập nhật thành công");
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} should delete user and return 204")
    void deleteUser_shouldReturn204() {
        // given
        when(userService.deleteUser(anyLong())).thenReturn(Mono.empty());

        // when & then
        webTestClient.delete()
            .uri("/api/v1/users/1")
            .exchange()
            .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("POST /api/v1/users should return 400 for invalid request")
    void createUser_shouldReturn400ForInvalidRequest() {
        // given
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
            .email("invalid-email")
            .password("weak")
            .build();

        // when & then
        webTestClient.post()
            .uri("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest();
    }
}
