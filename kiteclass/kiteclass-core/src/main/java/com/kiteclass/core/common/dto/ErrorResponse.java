package com.kiteclass.core.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standard error response for API errors.
 *
 * <p>Provides structured error information:
 * <ul>
 *   <li>Error code for programmatic handling</li>
 *   <li>Human-readable message</li>
 *   <li>Field-level validation errors</li>
 *   <li>Request path and timestamp</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String code;
    private String message;
    private String path;
    private Instant timestamp;
    private Map<String, List<String>> fieldErrors;

    /**
     * Creates a simple error response.
     *
     * @param code    error code
     * @param message error message
     * @param path    request path
     * @return ErrorResponse
     */
    public static ErrorResponse of(String code, String message, String path) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates an error response with field validation errors.
     *
     * @param code        error code
     * @param message     error message
     * @param path        request path
     * @param fieldErrors map of field names to error messages
     * @return ErrorResponse with field errors
     */
    public static ErrorResponse withFieldErrors(
            String code,
            String message,
            String path,
            Map<String, List<String>> fieldErrors) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .fieldErrors(fieldErrors)
                .build();
    }
}
