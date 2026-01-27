package com.kiteclass.core.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ApiResponse}.
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
class ApiResponseTest {

    @Test
    void success_shouldCreateSuccessResponseWithData() {
        // Given
        String data = "test data";

        // When
        ApiResponse<String> response = ApiResponse.success(data);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void success_shouldCreateSuccessResponseWithDataAndMessage() {
        // Given
        String data = "test data";
        String message = "Operation successful";

        // When
        ApiResponse<String> response = ApiResponse.success(data, message);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void error_shouldCreateErrorResponseWithMessage() {
        // Given
        String message = "Error occurred";

        // When
        ApiResponse<Void> response = ApiResponse.error(message);

        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void builder_shouldCreateResponseWithAllFields() {
        // Given / When
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .data("test")
                .message("message")
                .build();

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("test");
        assertThat(response.getMessage()).isEqualTo("message");
    }
}
