package com.kitehub.subscription.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    /**
     * Handle account lockout (GAP-515 / OWASP A07) — returns HTTP 423 + Retry-After header.
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ProblemDetail> handleAccountLockedException(
        AccountLockedException ex,
        WebRequest request
    ) {
        log.warn("Account locked: until={}", ex.getLockedUntil());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.LOCKED,
            "Account is temporarily locked due to too many failed login attempts."
        );
        problemDetail.setTitle("Account Locked");
        problemDetail.setProperty("lockedUntil", ex.getLockedUntil().toString());
        problemDetail.setProperty("retryAfterSeconds", ex.retryAfterSeconds());
        return ResponseEntity
            .status(HttpStatus.LOCKED)
            .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds()))
            .body(problemDetail);
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

    // GAP-571 (Wave 83 Bucket A) — Spring web framework exceptions previously fell
    // through to handleGenericException → 500. Map to correct RFC 7807 statuses.

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing parameter: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Required parameter '" + ex.getParameterName() + "' is missing"
        );
        problemDetail.setTitle("Missing Parameter");
        problemDetail.setProperty("parameterName", ex.getParameterName());
        problemDetail.setProperty("parameterType", ex.getParameterType());
        return problemDetail;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not allowed: {} {}", ex.getMethod(), ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.METHOD_NOT_ALLOWED,
            "HTTP method " + ex.getMethod() + " not supported for this endpoint"
        );
        problemDetail.setTitle("Method Not Allowed");
        if (ex.getSupportedMethods() != null) {
            problemDetail.setProperty("supportedMethods", ex.getSupportedMethods());
        }
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .header(HttpHeaders.ALLOW, ex.getSupportedHttpMethods() == null ? ""
                : String.join(", ", ex.getSupportedMethods()))
            .body(problemDetail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request body is malformed or unreadable"
        );
        problemDetail.setTitle("Malformed Request");
        return problemDetail;
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type: {}", ex.getContentType());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Content type not supported: " + ex.getContentType()
        );
        problemDetail.setTitle("Unsupported Media Type");
        return problemDetail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        StringBuilder errors = new StringBuilder();
        ex.getConstraintViolations().forEach(v ->
            errors.append(v.getPropertyPath())
                .append(": ")
                .append(v.getMessage())
                .append("; ")
        );
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            errors.toString()
        );
        problemDetail.setTitle("Validation Error");
        return problemDetail;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("No resource found: {}", ex.getResourcePath());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            "Endpoint not found: " + ex.getResourcePath()
        );
        problemDetail.setTitle("Not Found");
        return problemDetail;
    }

    /**
     * Handle Spring legacy NoHandlerFoundException (GAP-570 Wave 83 Bucket B follow-up).
     *
     * <p>PR #1407 added {@link NoResourceFoundException} handler (Spring 6.1+ for static
     * resources) but Spring throws {@link NoHandlerFoundException} (legacy, from
     * DispatcherServlet) when {@code spring.mvc.throw-exception-if-no-handler-found=true}.
     * Different class — without this handler, unknown endpoints fall through to
     * {@link #handleGenericException} → 500.</p>
     *
     * @param ex Exception raised when no controller matches the request
     * @return 404 ProblemDetail
     */
    /**
     * Handle database UNIQUE constraint violations (Wave beta-prep-1 Bucket E — concurrency hardening).
     *
     * <p>Concurrent inserts that bypass app-level pre-check (e.g. tenant subdomain create
     * race per GAP-730) reach the DB unique constraint. Without this handler, Spring's
     * {@link DataIntegrityViolationException} falls through to {@link #handleGenericException}
     * → HTTP 500. Correct REST semantic is HTTP 409 Conflict per
     * <a href="https://www.rfc-editor.org/rfc/rfc7231#section-6.5.8">RFC 7231 §6.5.8</a>.</p>
     *
     * <p>Bucket E paths affected:
     * <ul>
     *   <li>Path 1: tenant create race on {@code instances.subdomain} UNIQUE</li>
     *   <li>Path 5: role-grant race on {@code user_roles(user_id, role_id) WHERE deleted=FALSE} UNIQUE partial index</li>
     * </ul>
     *
     * @param ex DataIntegrityViolationException
     * @return 409 ProblemDetail
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation (likely UNIQUE constraint race): {}",
            ex.getMostSpecificCause().getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "Tài nguyên đã tồn tại hoặc xung đột với tài nguyên khác. Vui lòng thử lại với giá trị khác."
        );
        problemDetail.setTitle("Conflict");
        problemDetail.setProperty("errorCode", "RESOURCE_CONFLICT");
        return problemDetail;
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ProblemDetail handleNoHandlerFound(NoHandlerFoundException ex) {
        log.warn("No handler found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            "Endpoint not found: " + ex.getHttpMethod() + " " + ex.getRequestURL()
        );
        problemDetail.setTitle("Not Found");
        return problemDetail;
    }
}
