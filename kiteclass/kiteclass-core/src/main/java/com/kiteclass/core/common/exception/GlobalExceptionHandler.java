package com.kiteclass.core.common.exception;

import com.kiteclass.core.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * <p>Returns consistent {@link ErrorResponse} structure with:
 * <ul>
 *   <li>Error code for programmatic handling</li>
 *   <li>Human-readable message</li>
 *   <li>Request path for debugging</li>
 *   <li>Timestamp</li>
 *   <li>Field errors for validation failures</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles BusinessException and returns appropriate HTTP status.
     *
     * @param ex      the business exception
     * @param request the HTTP request
     * @return error response with business error details
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        log.warn("Business exception: {} - {}", ex.getCode(), ex.getMessage());

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of(ex.getCode(), ex.getMessage(), path);

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    /**
     * Handles EntityNotFoundException (subclass of BusinessException).
     *
     * @param ex      the entity not found exception
     * @param request the HTTP request
     * @return error response with 404 status
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Entity not found: {}", ex.getMessage());

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of(ex.getCode(), ex.getMessage(), path);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles DuplicateResourceException (subclass of BusinessException).
     *
     * @param ex      the duplicate resource exception
     * @param request the HTTP request
     * @return error response with 409 status
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        log.warn("Duplicate resource: {}", ex.getMessage());

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of(ex.getCode(), ex.getMessage(), path);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles ValidationException (subclass of BusinessException).
     *
     * @param ex      the validation exception
     * @param request the HTTP request
     * @return error response with 400 status
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex,
            HttpServletRequest request) {

        log.warn("Validation exception: {}", ex.getMessage());

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of(ex.getCode(), ex.getMessage(), path);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles validation errors from @Valid annotations.
     *
     * <p>Collects all field-level validation errors and returns them
     * in a structured format for client-side display.
     *
     * @param ex      the validation exception
     * @param request the HTTP request
     * @return error response with field-level validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.warn("Validation exception: {} validation error(s)", ex.getErrorCount());

        Map<String, List<String>> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.computeIfAbsent(error.getField(), k -> new ArrayList<>())
                    .add(error.getDefaultMessage());
        }

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.withFieldErrors(
                "VALIDATION_ERROR",
                "Validation failed for one or more fields",
                path,
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles IllegalArgumentException.
     *
     * @param ex      the illegal argument exception
     * @param request the HTTP request
     * @return error response with 400 status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of("INVALID_ARGUMENT", ex.getMessage(), path);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles all unexpected exceptions.
     *
     * <p>Logs the full stack trace and returns a generic error message
     * to avoid exposing internal details.
     *
     * @param ex      the unexpected exception
     * @param request the HTTP request
     * @return generic error response with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected exception at {}", request.getRequestURI(), ex);

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of(
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.",
                path
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
