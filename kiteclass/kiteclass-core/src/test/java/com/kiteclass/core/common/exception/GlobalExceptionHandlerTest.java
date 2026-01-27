package com.kiteclass.core.common.exception;

import com.kiteclass.core.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    void handleBusinessException_shouldReturnErrorResponseWithCorrectStatus() {
        // Given
        BusinessException ex = new BusinessException("TEST_ERROR", HttpStatus.BAD_REQUEST);

        // When
        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("TEST_ERROR");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");
    }

    @Test
    void handleEntityNotFoundException_shouldReturn404() {
        // Given
        EntityNotFoundException ex = new EntityNotFoundException("Student", 123L);

        // When
        ResponseEntity<ErrorResponse> response = handler.handleEntityNotFoundException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("ENTITY_NOT_FOUND");
        assertThat(response.getBody().getMessage()).contains("Student");
        assertThat(response.getBody().getMessage()).contains("123");
    }

    @Test
    void handleDuplicateResourceException_shouldReturn409() {
        // Given
        DuplicateResourceException ex = new DuplicateResourceException("email", "test@example.com");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateResourceException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_RESOURCE");
        assertThat(response.getBody().getMessage()).contains("email");
        assertThat(response.getBody().getMessage()).contains("test@example.com");
    }

    @Test
    void handleValidationException_shouldReturn400() {
        // Given
        ValidationException ex = new ValidationException("Invalid input");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid input");
    }

    @Test
    void handleMethodArgumentNotValidException_shouldReturnFieldErrors() {
        // Given
        MethodParameter parameter = mock(MethodParameter.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError1 = new FieldError("student", "email", "Email is required");
        FieldError fieldError2 = new FieldError("student", "email", "Email must be valid");
        FieldError fieldError3 = new FieldError("student", "firstName", "First name is required");

        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2, fieldError3));
        when(bindingResult.getErrorCount()).thenReturn(3);

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        // When
        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValidException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getFieldErrors()).isNotNull();
        assertThat(response.getBody().getFieldErrors()).containsKey("email");
        assertThat(response.getBody().getFieldErrors()).containsKey("firstName");
        assertThat(response.getBody().getFieldErrors().get("email")).hasSize(2);
        assertThat(response.getBody().getFieldErrors().get("firstName")).hasSize(1);
    }

    @Test
    void handleIllegalArgumentException_shouldReturn400() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_ARGUMENT");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid argument");
    }

    @Test
    void handleUnexpectedException_shouldReturn500WithGenericMessage() {
        // Given
        Exception ex = new RuntimeException("Something went wrong");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleUnexpectedException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getMessage()).contains("unexpected error");
        // Should not expose internal exception details
        assertThat(response.getBody().getMessage()).doesNotContain("Something went wrong");
    }
}
