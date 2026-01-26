package com.kiteclass.gateway.common;

import com.kiteclass.gateway.common.dto.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ErrorResponse.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@DisplayName("ErrorResponse Tests")
class ErrorResponseTest {

    @Test
    @DisplayName("of() should create error response with code, message, and path")
    void of_shouldCreateErrorResponse() {
        // given
        String code = "NOT_FOUND";
        String message = "Resource not found";
        String path = "/api/v1/users/123";

        // when
        ErrorResponse response = ErrorResponse.of(code, message, path);

        // then
        assertThat(response.getCode()).isEqualTo(code);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getPath()).isEqualTo(path);
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getFieldErrors()).isNull();
    }

    @Test
    @DisplayName("withFieldErrors() should include field validation errors")
    void withFieldErrors_shouldIncludeFieldErrors() {
        // given
        String code = "VALIDATION_ERROR";
        String message = "Validation failed";
        String path = "/api/v1/users";
        Map<String, List<String>> fieldErrors = Map.of(
                "email", List.of("Email không hợp lệ"),
                "name", List.of("Tên là bắt buộc", "Tên phải có ít nhất 2 ký tự")
        );

        // when
        ErrorResponse response = ErrorResponse.withFieldErrors(code, message, path, fieldErrors);

        // then
        assertThat(response.getCode()).isEqualTo(code);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getPath()).isEqualTo(path);
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getFieldErrors()).isNotNull();
        assertThat(response.getFieldErrors()).hasSize(2);
        assertThat(response.getFieldErrors().get("email")).containsExactly("Email không hợp lệ");
        assertThat(response.getFieldErrors().get("name")).hasSize(2);
    }

    @Test
    @DisplayName("timestamp should be set automatically")
    void timestamp_shouldBeSetAutomatically() {
        // when
        ErrorResponse response = ErrorResponse.of("ERROR", "Error message", "/api");

        // then
        assertThat(response.getTimestamp()).isNotNull();
    }
}
