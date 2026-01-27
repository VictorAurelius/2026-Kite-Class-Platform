package com.kiteclass.gateway.common.exception;

import com.kiteclass.gateway.common.constant.MessageCodes;
import com.kiteclass.gateway.common.dto.ErrorResponse;
import com.kiteclass.gateway.common.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler for all REST endpoints.
 *
 * <p>Uses MessageService for i18n message resolution.
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
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

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

        String message = ex.getArgs() != null
                ? messageService.getMessage(ex.getCode(), ex.getArgs())
                : messageService.getMessage(ex.getCode());

        log.warn("Business exception: {} - {}", ex.getCode(), message);

        String path = exchange.getRequest().getPath().value();
        ErrorResponse response = ErrorResponse.of(ex.getCode(), message, path);

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
            fieldErrors.computeIfAbsent(error.getField(), k -> new ArrayList<>())
                    .add(error.getDefaultMessage());
        }

        String path = exchange.getRequest().getPath().value();
        String message = messageService.getMessage(MessageCodes.VALIDATION_DATA_INVALID);
        ErrorResponse response = ErrorResponse.withFieldErrors(
                MessageCodes.VALIDATION_DATA_INVALID,
                message,
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
        String message = messageService.getMessage(MessageCodes.INTERNAL_ERROR);
        ErrorResponse response = ErrorResponse.of(MessageCodes.INTERNAL_ERROR, message, path);

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }
}
