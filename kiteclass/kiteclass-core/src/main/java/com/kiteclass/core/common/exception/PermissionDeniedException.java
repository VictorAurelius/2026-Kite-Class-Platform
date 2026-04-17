package com.kiteclass.core.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user does not have permission to perform an action.
 *
 * <p>This exception is used to indicate authorization failures,
 * such as when a teacher tries to mark attendance but is not the MAIN_TEACHER.
 *
 * <p>HTTP Status: 403 Forbidden
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
public class PermissionDeniedException extends BusinessException {

    /**
     * Constructs a new PermissionDeniedException with error code.
     *
     * @param errorCode the error code for this exception
     */
    public PermissionDeniedException(String errorCode) {
        super(errorCode, HttpStatus.FORBIDDEN);
    }

    /**
     * Constructs a new PermissionDeniedException with error code and arguments.
     *
     * @param errorCode the error code for this exception
     * @param args arguments for error message formatting
     */
    public PermissionDeniedException(String errorCode, Object... args) {
        super(errorCode, HttpStatus.FORBIDDEN, args);
    }

    /**
     * Constructs a new PermissionDeniedException with error code, cause, and arguments.
     *
     * @param errorCode the error code for this exception
     * @param cause the cause of this exception
     * @param args arguments for error message formatting
     */
    public PermissionDeniedException(String errorCode, Throwable cause, Object... args) {
        super(errorCode, HttpStatus.FORBIDDEN, cause, args);
    }
}
