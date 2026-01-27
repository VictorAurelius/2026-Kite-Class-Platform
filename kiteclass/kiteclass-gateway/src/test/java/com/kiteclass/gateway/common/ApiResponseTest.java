package com.kiteclass.gateway.common;

import com.kiteclass.gateway.common.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ApiResponse.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@DisplayName("ApiResponse Tests")
class ApiResponseTest {

    @Test
    @DisplayName("success() should create response with success=true and data")
    void success_shouldCreateSuccessResponse() {
        // given
        String data = "test data";

        // when
        ApiResponse<String> response = ApiResponse.success(data);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("success() with message should include message")
    void success_withMessage_shouldIncludeMessage() {
        // given
        String data = "test data";
        String message = "Operation successful";

        // when
        ApiResponse<String> response = ApiResponse.success(data, message);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("error() should create response with success=false")
    void error_shouldCreateErrorResponse() {
        // given
        String errorMessage = "Something went wrong";

        // when
        ApiResponse<Void> response = ApiResponse.error(errorMessage);

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo(errorMessage);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("success() with null data should be allowed")
    void success_withNullData_shouldBeAllowed() {
        // when
        ApiResponse<String> response = ApiResponse.success(null);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
    }
}
