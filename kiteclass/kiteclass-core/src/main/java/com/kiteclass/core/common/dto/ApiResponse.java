package com.kiteclass.core.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard API response wrapper for all endpoints.
 *
 * <p>Provides consistent response structure with:
 * <ul>
 *   <li>Success/failure status</li>
 *   <li>Data payload</li>
 *   <li>Optional message</li>
 *   <li>Timestamp</li>
 * </ul>
 *
 * @param <T> Type of the data payload
 * @author KiteClass Team
 * @since 2.2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;
    private Instant timestamp;

    /**
     * Creates a successful response with data.
     *
     * @param data the response payload
     * @param <T>  type of the data
     * @return ApiResponse with success=true
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates a successful response with data and message.
     *
     * @param data    the response payload
     * @param message success message
     * @param <T>     type of the data
     * @return ApiResponse with success=true and message
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates an error response with message.
     *
     * @param message error message
     * @param <T>     type of the data (typically Void)
     * @return ApiResponse with success=false
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}
