package com.kiteclass.gateway.common;

import com.kiteclass.gateway.common.constant.MessageCodes;
import com.kiteclass.gateway.common.dto.ErrorResponse;
import com.kiteclass.gateway.common.exception.BusinessException;
import com.kiteclass.gateway.common.exception.EntityNotFoundException;
import com.kiteclass.gateway.common.exception.GlobalExceptionHandler;
import com.kiteclass.gateway.common.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Mock
    private MessageService messageService;

    private GlobalExceptionHandler handler;
    private MockServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(messageService);
        ServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/123").build();
        exchange = MockServerWebExchange.from((MockServerHttpRequest) request);
    }

    @Test
    @DisplayName("handleBusinessException should return correct status and error response")
    void handleBusinessException_shouldReturnCorrectStatusAndResponse() {
        // given
        BusinessException ex = new BusinessException(
                MessageCodes.AUTH_INVALID_CREDENTIALS,
                HttpStatus.UNAUTHORIZED
        );
        when(messageService.getMessage(MessageCodes.AUTH_INVALID_CREDENTIALS))
                .thenReturn("Email hoặc mật khẩu không đúng");

        // when
        Mono<ResponseEntity<ErrorResponse>> result = handler.handleBusinessException(ex, exchange);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getCode()).isEqualTo(MessageCodes.AUTH_INVALID_CREDENTIALS);
                    assertThat(response.getBody().getMessage()).isEqualTo("Email hoặc mật khẩu không đúng");
                    assertThat(response.getBody().getPath()).isEqualTo("/api/v1/users/123");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("handleBusinessException for EntityNotFoundException should return 404")
    void handleEntityNotFoundException_shouldReturn404() {
        // given
        EntityNotFoundException ex = new EntityNotFoundException("User", 123L);
        when(messageService.getMessage(eq(MessageCodes.ENTITY_NOT_FOUND), any(Object[].class)))
                .thenReturn("User với ID 123 không tồn tại");

        // when
        Mono<ResponseEntity<ErrorResponse>> result = handler.handleBusinessException(ex, exchange);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getCode()).isEqualTo(MessageCodes.ENTITY_NOT_FOUND);
                    assertThat(response.getBody().getMessage()).contains("User", "123");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("handleUnexpectedException should return 500 with generic message")
    void handleUnexpectedException_shouldReturn500() {
        // given
        RuntimeException ex = new RuntimeException("Unexpected error");
        when(messageService.getMessage(MessageCodes.INTERNAL_ERROR))
                .thenReturn("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");

        // when
        Mono<ResponseEntity<ErrorResponse>> result = handler.handleUnexpectedException(ex, exchange);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getCode()).isEqualTo(MessageCodes.INTERNAL_ERROR);
                    assertThat(response.getBody().getMessage()).contains("lỗi hệ thống");
                })
                .verifyComplete();
    }
}
