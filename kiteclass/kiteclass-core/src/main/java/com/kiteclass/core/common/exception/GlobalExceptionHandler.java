package com.kiteclass.core.common.exception;

import com.kiteclass.core.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
 * <p>Integrates with MessageSource for i18n support:
 * <ul>
 *   <li>Error messages resolved from messages.properties</li>
 *   <li>Supports multiple languages (EN, VI)</li>
 *   <li>Locale determined from Accept-Language header</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    /**
     * Handles BusinessException and returns appropriate HTTP status.
     *
     * <p>Resolves error message from MessageSource using error code and args.
     * Supports i18n based on Accept-Language header.
     *
     * @param ex      the business exception
     * @param request the HTTP request
     * @return error response with localized message
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        Locale locale = LocaleContextHolder.getLocale();
        String message = resolveMessage(ex.getCode(), ex.getArgs(), locale);

        log.warn("Business exception: {} - {} (locale: {})", ex.getCode(), message, locale);

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of(ex.getCode(), message, path);

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    /**
     * Handles EntityNotFoundException (subclass of BusinessException).
     *
     * <p>Resolves error message from MessageSource using error code and args.
     *
     * @param ex      the entity not found exception
     * @param request the HTTP request
     * @return error response with 404 status and localized message
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        Locale locale = LocaleContextHolder.getLocale();
        String message = resolveMessage(ex.getCode(), ex.getArgs(), locale);

        log.warn("Entity not found: {} - {} (locale: {})", ex.getCode(), message, locale);

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of(ex.getCode(), message, path);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles DuplicateResourceException (subclass of BusinessException).
     *
     * <p>Resolves error message from MessageSource using error code and args.
     *
     * @param ex      the duplicate resource exception
     * @param request the HTTP request
     * @return error response with 409 status and localized message
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        Locale locale = LocaleContextHolder.getLocale();
        String message = resolveMessage(ex.getCode(), ex.getArgs(), locale);

        log.warn("Duplicate resource: {} - {} (locale: {})", ex.getCode(), message, locale);

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of(ex.getCode(), message, path);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles ValidationException (subclass of BusinessException).
     *
     * <p>Resolves error message from MessageSource using error code and args.
     *
     * @param ex      the validation exception
     * @param request the HTTP request
     * @return error response with 400 status and localized message
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex,
            HttpServletRequest request) {

        Locale locale = LocaleContextHolder.getLocale();
        String message = resolveMessage(ex.getCode(), ex.getArgs(), locale);

        log.warn("Validation exception: {} - {} (locale: {})", ex.getCode(), message, locale);

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of(ex.getCode(), message, path);

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
     * @return generic error response with 500 status and localized message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected exception at {}", request.getRequestURI(), ex);

        Locale locale = LocaleContextHolder.getLocale();
        String message = resolveMessage("SYSTEM_INTERNAL_ERROR", null, locale);

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of("SYSTEM_INTERNAL_ERROR", message, path);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Resolves error message from MessageSource.
     *
     * <p>Falls back to error code if message not found in properties files.
     *
     * @param code   error code to resolve
     * @param args   arguments for message formatting ({0}, {1}, etc.)
     * @param locale locale for message resolution
     * @return resolved message or error code as fallback
     */
    private String resolveMessage(String code, Object[] args, Locale locale) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            log.warn("Failed to resolve message for code: {} (locale: {})", code, locale);
            return code; // Fallback to code if message not found
        }
    }
}
