package com.kiteclass.core.common.exception;

import com.kiteclass.core.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Global exception handler for all REST endpoints.
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of("INVALID_ARGUMENT", ex.getMessage(), path);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            OptimisticLockingFailureException ex,
            HttpServletRequest request) {

        log.warn("Optimistic lock failure at {}: {}",
                request.getRequestURI(), ex.getMessage());

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of(
                "OPTIMISTIC_LOCK_CONFLICT",
                "The record was modified by another writer; refresh and retry.",
                path);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(
            RuntimeException ex,
            HttpServletRequest request) {

        log.warn("Authorization denied at {}: {}",
                request.getRequestURI(), ex.getMessage());

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of(
                "ACCESS_DENIED",
                "You do not have permission to access this resource.",
                path);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class
    })
    public ResponseEntity<ErrorResponse> handleClientInputException(
            Exception ex,
            HttpServletRequest request) {

        log.warn("Client input error at {}: {} - {}",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage());

        String message = "Invalid request payload";
        String code = "INVALID_REQUEST";
        if (ex instanceof HttpMessageNotReadableException) {
            message = "Request body cannot be parsed (malformed JSON or invalid enum value)";
            code = "MALFORMED_REQUEST_BODY";
        } else if (ex instanceof MethodArgumentTypeMismatchException matm) {
            message = String.format("Parameter '%s' has invalid type", matm.getName());
            code = "PARAM_TYPE_MISMATCH";
        } else if (ex instanceof HandlerMethodValidationException) {
            message = "Request parameter validation failed";
            code = "PARAM_VALIDATION_FAILED";
        }

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of(code, message, path);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xu ly route khong ton tai - tra HTTP 404 thay vi 500 (GAP-796).
     * Can spring.mvc.throw-exception-if-no-handler-found=true +
     * spring.web.resources.add-mappings=false. Phai dat TRUOC catch-all.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        log.warn("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of(
                "RESOURCE_NOT_FOUND",
                "The requested resource was not found.",
                path);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Xu ly HTTP method khong duoc ho tro tren route - tra HTTP 405 thay vi 500 (GAP-796).
     * Phai dat TRUOC catch-all handleUnexpectedException.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        log.warn("Method not supported at {}: {} - supported: {}",
                request.getRequestURI(), ex.getMethod(), ex.getSupportedHttpMethods());

        String path = request.getRequestURI();
        ErrorResponse response = ErrorResponse.of(
                "METHOD_NOT_ALLOWED",
                String.format("Request method '%s' is not supported for this endpoint.", ex.getMethod()),
                path);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

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

    private String resolveMessage(String code, Object[] args, Locale locale) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            log.warn("Failed to resolve message for code: {} (locale: {})", code, locale);
            return code;
        }
    }
}
