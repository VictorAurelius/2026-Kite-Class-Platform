package com.kiteclass.gateway.common.exception;

import com.kiteclass.gateway.common.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST endpoints.
 *
 * <p>Handles:
 * <ul>
 *   <li>Business exceptions with proper HTTP status</li>
 *   <li>Validation errors with field-level details</li>
 *   <li>Unexpected exceptions with generic error response</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles BusinessException and returns appropriate HTTP status.
     *
     * @param ex       the business exception
     * @param exchange the server web exchange
     * @return error response with business error details
     */
    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBusinessException(
            BusinessException ex,
            ServerWebExchange exchange) {
        log.warn("Business exception: {} - {}", ex.getCode(), ex.getMessage());

        String path = exchange.getRequest().getPath().value();
        ErrorResponse response = ErrorResponse.of(ex.getCode(), ex.getMessage(), path);

        return Mono.just(ResponseEntity.status(ex.getStatus()).body(response));
    }

    /**
     * Handles validation errors from @Valid annotations.
     *
     * @param ex       the validation exception
     * @param exchange the server web exchange
     * @return error response with field-level validation errors
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
            WebExchangeBindException ex,
            ServerWebExchange exchange) {
        log.warn("Validation exception: {}", ex.getMessage());

        Map<String, List<String>> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getFieldErrors()) {
            fieldErrors.computeIfAbsent(error.getField(), k -> new java.util.ArrayList<>())
                    .add(error.getDefaultMessage());
        }

        String path = exchange.getRequest().getPath().value();
        ErrorResponse response = ErrorResponse.withFieldErrors(
                "VALIDATION_ERROR",
                "Dữ liệu không hợp lệ",
                path,
                fieldErrors
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

    /**
     * Handles all unexpected exceptions.
     *
     * @param ex       the unexpected exception
     * @param exchange the server web exchange
     * @return generic error response
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUnexpectedException(
            Exception ex,
            ServerWebExchange exchange) {
        log.error("Unexpected exception", ex);

        String path = exchange.getRequest().getPath().value();
        ErrorResponse response = ErrorResponse.of(
                "INTERNAL_ERROR",
                "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.",
                path
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }
}
