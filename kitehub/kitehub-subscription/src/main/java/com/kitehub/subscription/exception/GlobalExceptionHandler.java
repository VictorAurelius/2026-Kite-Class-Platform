package com.kitehub.subscription.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
}
