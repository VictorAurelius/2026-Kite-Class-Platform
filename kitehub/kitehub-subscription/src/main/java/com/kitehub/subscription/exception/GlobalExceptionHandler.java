package com.kitehub.subscription.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler for REST controllers.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle IllegalArgumentException (validation errors, business logic errors).
     *
     * @param ex Exception
     * @param request Web request
     * @return Problem detail
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFoundException(
        EntityNotFoundException ex,
        WebRequest request
    ) {
        log.warn("Not found: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problemDetail.setTitle("Not Found");
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(
        IllegalArgumentException ex,
        WebRequest request
    ) {
        log.warn("Bad request: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problemDetail.setTitle("Bad Request");
        return problemDetail;
    }

    /**
     * Handle validation errors (Jakarta Bean Validation).
     *
     * @param ex Exception
     * @param request Web request
     * @return Problem detail
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(
        MethodArgumentNotValidException ex,
        WebRequest request
    ) {
        log.warn("Validation error: {}", ex.getMessage());
        StringBuilder errors = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.append(error.getField())
                .append(": ")
                .append(error.getDefaultMessage())
                .append("; ")
        );

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            errors.toString()
        );
        problemDetail.setTitle("Validation Error");
        return problemDetail;
    }

    /**
     * Handle trial-to-paid migration exceptions (GAP-192).
     * Maps {@link MigrationException.Code} to the HTTP status per api-contract.md.
     *
     * @param ex Migration exception
     * @param request Web request
     * @return Problem detail with stable error code
     */
    @ExceptionHandler(MigrationException.class)
    public ProblemDetail handleMigrationException(
        MigrationException ex,
        WebRequest request
    ) {
        HttpStatus status = switch (ex.getCode()) {
            case MIGRATION_IN_FLIGHT, INVALID_PHASE_TRANSITION -> HttpStatus.CONFLICT;
            case REVERSAL_WINDOW_EXPIRED, RESCUE_WINDOW_EXPIRED -> HttpStatus.GONE;
            case MIGRATION_FAILED_LOCKED -> HttpStatus.LOCKED;
            case PAYMENT_DECLINED -> HttpStatus.PAYMENT_REQUIRED;
        };
        log.warn("Migration error [{}]: {}", ex.getCode(), ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle("Migration Error");
        problemDetail.setProperty("errorCode", ex.getCode().name());
        return problemDetail;
    }

    /**
     * Handle all other exceptions.
     *
     * @param ex Exception
     * @param request Web request
     * @return Problem detail
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(
        Exception ex,
        WebRequest request
    ) {
        log.error("Internal server error", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
        );
        problemDetail.setTitle("Internal Server Error");
        return problemDetail;
    }

    /**
     * Handle Spring Security authorization failures from {@code @PreAuthorize}.
     *
     * <p>Without this handler the catch-all {@link #handleGenericException} would map
     * {@link AuthorizationDeniedException} to HTTP 500 — masking forbidden access as
     * an internal error. GAP-384 (Wave 35) added {@code @PreAuthorize} guards on the
     * beta admin endpoints, which raise this exception when the caller's role does
     * not include {@code PLATFORM_ADMIN}.</p>
     *
     * @param ex Authorization failure
     * @return 403 ProblemDetail
     */
    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ProblemDetail handleAuthorizationDenied(Exception ex) {
        log.warn("Forbidden: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "Access denied"
        );
        problemDetail.setTitle("Forbidden");
        return problemDetail;
    }

    /**
     * Handle Spring Security authentication failures (anonymous access to protected endpoint).
     *
     * @param ex Authentication failure
     * @return 401 ProblemDetail
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        log.warn("Unauthorized: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Authentication required"
        );
        problemDetail.setTitle("Unauthorized");
        return problemDetail;
    }
}
