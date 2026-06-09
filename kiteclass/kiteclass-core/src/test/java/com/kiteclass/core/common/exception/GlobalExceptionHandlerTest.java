package com.kiteclass.core.common.exception;

import com.kiteclass.core.common.dto.ErrorResponse;
import com.kiteclass.core.module.instance.approval.ConcurrentRebrandException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.http.HttpHeaders;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(messageSource);
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
        EntityNotFoundException ex = new EntityNotFoundException("STUDENT_NOT_FOUND", (Object) 123L);
        when(messageSource.getMessage(eq("STUDENT_NOT_FOUND"), any(), any(Locale.class)))
            .thenReturn("Student not found with ID: 123");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleEntityNotFoundException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("STUDENT_NOT_FOUND");
        assertThat(response.getBody().getMessage()).contains("Student");
        assertThat(response.getBody().getMessage()).contains("123");
    }

    @Test
    void handleDuplicateResourceException_shouldReturn409() {
        // Given
        DuplicateResourceException ex = new DuplicateResourceException("STUDENT_EMAIL_EXISTS", (Object) "test@example.com");
        when(messageSource.getMessage(eq("STUDENT_EMAIL_EXISTS"), any(), any(Locale.class)))
            .thenReturn("Student email already exists: test@example.com");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateResourceException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("STUDENT_EMAIL_EXISTS");
        assertThat(response.getBody().getMessage()).contains("email");
        assertThat(response.getBody().getMessage()).contains("test@example.com");
    }

    @Test
    void handleValidationException_shouldReturn400() {
        // Given
        ValidationException ex = new ValidationException("COURSE_CANNOT_DELETE_STATUS", "PUBLISHED");
        when(messageSource.getMessage(eq("COURSE_CANNOT_DELETE_STATUS"), any(), any(Locale.class)))
            .thenReturn("Cannot delete course with status PUBLISHED. Only DRAFT courses can be deleted.");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("COURSE_CANNOT_DELETE_STATUS");
        assertThat(response.getBody().getMessage()).contains("Cannot delete");
        assertThat(response.getBody().getMessage()).contains("PUBLISHED");
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
    void handleMissingRequestHeader_shouldReturn400() {
        // GAP-1117: thieu required @RequestHeader (vi du X-User-Id) phai tra 400,
        // khong phai roi vao catch-all 500.
        MethodParameter parameter = mock(MethodParameter.class);
        org.springframework.web.bind.MissingRequestHeaderException ex =
                new org.springframework.web.bind.MissingRequestHeaderException("X-User-Id", parameter);

        ResponseEntity<ErrorResponse> response = handler.handleMissingRequestHeader(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("MISSING_HEADER");
        assertThat(response.getBody().getMessage()).contains("X-User-Id");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");
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
        when(messageSource.getMessage(eq("SYSTEM_INTERNAL_ERROR"), any(), any(Locale.class)))
            .thenReturn("An unexpected error occurred. Please try again later.");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleUnexpectedException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("SYSTEM_INTERNAL_ERROR");
        assertThat(response.getBody().getMessage()).contains("unexpected error");
        // Should not expose internal exception details
        assertThat(response.getBody().getMessage()).doesNotContain("Something went wrong");
    }

    @Test
    void handleTenantNotSet_shouldReturn400WithStructuredBody() {
        // GAP-777: thieu tenant context (vi du owner.test co tenant_id IS NULL)
        // phai tra 400 voi structured JSON body (code + message), khong phai 500 / empty body.
        TenantNotSetException ex = new TenantNotSetException(
                "Tenant context not set for current thread");
        when(messageSource.getMessage(eq("TENANT_NOT_SET"), any(), any(Locale.class)))
                .thenReturn("Khong tim thay ngu canh tenant. Vui long cung cap header X-Tenant-Id.");

        ResponseEntity<ErrorResponse> response = handler.handleTenantNotSet(ex, request);

        // Semantic: thieu tenant context la client error (400), khong phai server error (500)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // Non-empty structured body (closes GAP-777 empty-body symptom)
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("TENANT_NOT_SET");
        assertThat(response.getBody().getMessage()).isNotBlank();
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleTenantNotSet_shouldFallBackToCodeWhenMessageMissing() {
        // GAP-777: ke ca khi message bundle thieu key, body van non-empty (code lam fallback)
        TenantNotSetException ex = new TenantNotSetException("missing tenant");
        when(messageSource.getMessage(eq("TENANT_NOT_SET"), any(), any(Locale.class)))
                .thenThrow(new RuntimeException("no such message key"));

        ResponseEntity<ErrorResponse> response = handler.handleTenantNotSet(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("TENANT_NOT_SET");
        // resolveMessage fallback tra ve chinh code khi key missing -> van non-empty
        assertThat(response.getBody().getMessage()).isEqualTo("TENANT_NOT_SET");
    }

    @Test
    void handleNoHandlerFound_shouldReturn404() {
        // GAP-796: route khong ton tai phai tra 404, khong phai 500
        NoHandlerFoundException ex = new NoHandlerFoundException(
                "GET", "/api/v1/does-not-exist", new HttpHeaders());

        ResponseEntity<ErrorResponse> response = handler.handleNoHandlerFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");
    }

    @Test
    void handleMethodNotSupported_shouldReturn405() {
        // GAP-796: HTTP method khong ho tro phai tra 405, khong phai 500
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException(
                "DELETE", List.of("GET", "POST"));

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("METHOD_NOT_ALLOWED");
        assertThat(response.getBody().getMessage()).contains("DELETE");
    }

    @Test
    void handleConcurrentRebrand_shouldReturn409WithStructuredBody() {
        // GAP-777 cross-flow sweep: ConcurrentRebrandException javadoc mandate
        // 409 Conflict — without dedicated handler it fell into catch-all 500.
        ConcurrentRebrandException ex = new ConcurrentRebrandException(
                "Another rebrand is already running for tenant T1");

        ResponseEntity<ErrorResponse> response = handler.handleConcurrentRebrand(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("REBRAND_CONFLICT");
        assertThat(response.getBody().getMessage()).contains("Another rebrand");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");
    }

    @Test
    void handleConcurrentRebrand_shouldFallBackWhenMessageNull() {
        ConcurrentRebrandException ex = new ConcurrentRebrandException(null);

        ResponseEntity<ErrorResponse> response = handler.handleConcurrentRebrand(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("REBRAND_CONFLICT");
        assertThat(response.getBody().getMessage()).contains("rebrand operation is already in progress");
    }
}
