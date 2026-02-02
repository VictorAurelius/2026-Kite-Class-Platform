package com.kiteclass.gateway.common.exception;

import com.kiteclass.gateway.common.constant.MessageCodes;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a token is used for a different tenant.
 *
 * <p>Uses i18n message code: {@code error.auth.tenant_mismatch} - "Token không hợp lệ cho tenant này"
 *
 * <p>Enforces tenant isolation by ensuring:
 * <ul>
 *   <li>Tokens can only access their own tenant data</li>
 *   <li>Cross-tenant access is prevented</li>
 *   <li>Tenant context is validated on every request</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.1.0
 */
public class TenantMismatchException extends BusinessException {

    /**
     * Creates exception for tenant mismatch.
     *
     * @param message descriptive message about the mismatch
     */
    public TenantMismatchException(String message) {
        super(MessageCodes.AUTH_TENANT_MISMATCH, HttpStatus.FORBIDDEN);
    }
}
