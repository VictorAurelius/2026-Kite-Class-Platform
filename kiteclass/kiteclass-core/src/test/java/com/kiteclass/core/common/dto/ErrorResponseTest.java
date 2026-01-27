package com.kiteclass.core.common.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ErrorResponse}.
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
class ErrorResponseTest {

    @Test
    void of_shouldCreateErrorResponseWithCodeMessageAndPath() {
        // Given
        String code = "ENTITY_NOT_FOUND";
        String message = "Student not found";
        String path = "/api/v1/students/123";

        // When
        ErrorResponse response = ErrorResponse.of(code, message, path);

        // Then
        assertThat(response.getCode()).isEqualTo(code);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getPath()).isEqualTo(path);
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getFieldErrors()).isNull();
    }

    @Test
    void withFieldErrors_shouldCreateErrorResponseWithFieldErrors() {
        // Given
        String code = "VALIDATION_ERROR";
        String message = "Validation failed";
        String path = "/api/v1/students";
        Map<String, List<String>> fieldErrors = Map.of(
                "email", List.of("Email is required", "Email must be valid"),
                "firstName", List.of("First name is required")
        );

        // When
        ErrorResponse response = ErrorResponse.withFieldErrors(code, message, path, fieldErrors);

        // Then
        assertThat(response.getCode()).isEqualTo(code);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getPath()).isEqualTo(path);
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getFieldErrors()).isEqualTo(fieldErrors);
        assertThat(response.getFieldErrors()).containsKey("email");
        assertThat(response.getFieldErrors()).containsKey("firstName");
        assertThat(response.getFieldErrors().get("email")).hasSize(2);
    }

    @Test
    void builder_shouldCreateErrorResponseWithAllFields() {
        // Given / When
        ErrorResponse response = ErrorResponse.builder()
                .code("TEST_ERROR")
                .message("Test message")
                .path("/test")
                .fieldErrors(Map.of("field1", List.of("error1")))
                .build();

        // Then
        assertThat(response.getCode()).isEqualTo("TEST_ERROR");
        assertThat(response.getMessage()).isEqualTo("Test message");
        assertThat(response.getPath()).isEqualTo("/test");
        assertThat(response.getFieldErrors()).containsKey("field1");
    }
}
