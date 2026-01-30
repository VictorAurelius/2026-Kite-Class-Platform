package com.kiteclass.gateway.common.constant;

/**
 * Centralized message codes for i18n.
 *
 * <p>All message codes must have corresponding entries in messages.properties.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
public final class MessageCodes {

    private MessageCodes() {
    }

    // Error codes
    public static final String ENTITY_NOT_FOUND = "error.entity.not_found";
    public static final String ENTITY_NOT_FOUND_FIELD = "error.entity.not_found.field";
    public static final String ENTITY_DUPLICATE = "error.entity.duplicate";
    public static final String INTERNAL_ERROR = "error.internal";
    public static final String UNAUTHORIZED = "error.unauthorized";
    public static final String FORBIDDEN = "error.forbidden";

    // Auth codes
    public static final String AUTH_INVALID_CREDENTIALS = "error.auth.invalid_credentials";
    public static final String AUTH_ACCOUNT_LOCKED = "error.auth.account_locked";
    public static final String AUTH_ACCOUNT_INACTIVE = "error.auth.account_inactive";
    public static final String AUTH_TOKEN_EXPIRED = "error.auth.token_expired";
    public static final String AUTH_TOKEN_INVALID = "error.auth.token_invalid";
    public static final String AUTH_REFRESH_EXPIRED = "error.auth.refresh_expired";
    public static final String AUTH_REFRESH_TOKEN_INVALID = "error.auth.refresh_token_invalid";
    public static final String AUTH_REFRESH_TOKEN_EXPIRED = "error.auth.refresh_token_expired";
    public static final String AUTH_REGISTRATION_FAILED = "error.auth.registration_failed";

    // Password reset codes
    public static final String PASSWORD_RESET_TOKEN_INVALID = "error.password_reset.token_invalid";
    public static final String PASSWORD_RESET_TOKEN_EXPIRED = "error.password_reset.token_expired";
    public static final String PASSWORD_RESET_TOKEN_USED = "error.password_reset.token_used";

    // User codes
    public static final String USER_NOT_FOUND = "error.user.not_found";
    public static final String USER_EMAIL_EXISTS = "error.user.email_exists";

    // Validation codes
    public static final String VALIDATION_REQUIRED = "validation.required";
    public static final String VALIDATION_MIN_LENGTH = "validation.min_length";
    public static final String VALIDATION_MAX_LENGTH = "validation.max_length";
    public static final String VALIDATION_EMAIL_INVALID = "validation.email.invalid";
    public static final String VALIDATION_PHONE_INVALID = "validation.phone.invalid";
    public static final String VALIDATION_DATA_INVALID = "validation.data.invalid";
    public static final String VALIDATION_PASSWORD_INVALID = "validation.password.invalid";

    // Success codes
    public static final String SUCCESS_CREATED = "success.user.created";
    public static final String SUCCESS_UPDATED = "success.user.updated";
    public static final String SUCCESS_DELETED = "success.user.deleted";
    public static final String SUCCESS_PASSWORD_CHANGED = "success.password.changed";
    public static final String SUCCESS_PASSWORD_RESET_SENT = "success.password.reset_sent";
    public static final String SUCCESS_PASSWORD_RESET = "success.password.reset";
}
