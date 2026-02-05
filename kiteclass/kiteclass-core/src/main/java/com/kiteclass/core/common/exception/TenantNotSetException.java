package com.kiteclass.core.common.exception;

/**
 * Exception thrown when tenant context is not set but required.
 *
 * <p>This occurs when:
 * <ul>
 *   <li>X-Tenant-Id header is missing from request</li>
 *   <li>Code tries to access TenantContext before it's initialized</li>
 *   <li>Request bypasses TenantFilterInterceptor</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 * @see com.kiteclass.core.common.context.TenantContext
 */
public class TenantNotSetException extends RuntimeException {

    private static final String ERROR_CODE = "TENANT_NOT_SET";

    /**
     * Constructs exception with detail message.
     *
     * @param message the detail message
     */
    public TenantNotSetException(String message) {
        super(message);
    }

    /**
     * Constructs exception with detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public TenantNotSetException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Gets the error code for this exception.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
