package com.kiteclass.core.common.context;

/**
 * Thread-local storage for current user context.
 *
 * <p>Stores the current user ID from X-User-Id header (forwarded by Gateway).
 * The user ID is extracted by TenantFilterInterceptor from the request header.
 *
 * <p>Usage:
 * <pre>{@code
 * // Set current user (done by TenantFilterInterceptor)
 * UserContext.setCurrentUser(userId);
 *
 * // Get current user (can be null for unauthenticated requests)
 * Long userId = UserContext.getCurrentUser();
 *
 * // Check if user context is set
 * if (UserContext.isSet()) {
 *     // Use user ID
 * }
 *
 * // Clear context (done in afterCompletion)
 * UserContext.clear();
 * }</pre>
 *
 * <p>IMPORTANT: Always clear context after request completion to prevent memory leaks
 * and cross-request data leakage.
 *
 * <p>Unlike TenantContext which throws exception when not set, UserContext allows
 * null values to support unauthenticated requests and background jobs.
 *
 * @author KiteClass Team
 * @since 2.2.0
 * @see com.kiteclass.core.config.TenantFilterInterceptor
 * @see TenantContext
 */
public final class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private UserContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Sets the current user ID for this thread.
     *
     * @param userId the user ID (must not be null)
     * @throws IllegalArgumentException if userId is null
     */
    public static void setCurrentUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        CURRENT_USER.set(userId);
    }

    /**
     * Gets the current user ID for this thread.
     *
     * <p>Returns null if user context is not set, allowing support for
     * unauthenticated requests and background jobs.
     *
     * @return the current user ID, or null if not set
     */
    public static Long getCurrentUser() {
        return CURRENT_USER.get();
    }

    /**
     * Clears the user context for this thread.
     * Must be called after request completion to prevent memory leaks.
     */
    public static void clear() {
        CURRENT_USER.remove();
    }

    /**
     * Checks if user context is set for current thread.
     *
     * @return true if user context is set, false otherwise
     */
    public static boolean isSet() {
        return CURRENT_USER.get() != null;
    }
}
