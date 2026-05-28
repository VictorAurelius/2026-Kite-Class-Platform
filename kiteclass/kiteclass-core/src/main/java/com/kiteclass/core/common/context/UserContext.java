package com.kiteclass.core.common.context;

import java.util.UUID;

/**
 * Thread-local storage for current user context.
 *
 * <p>Stores the current user ID from X-User-Id header (forwarded by Gateway).
 * The user ID is extracted by TenantFilterInterceptor from the request header.
 *
 * <p>The X-User-Id header carries the JWT {@code sub} claim, which is a {@link UUID}
 * (GAP-795). There is no numeric user id in the auth token — the UUID is the only
 * user identity. Audit fields {@code created_by} / {@code updated_by} therefore store
 * this UUID.
 *
 * <p>Usage:
 * <pre>{@code
 * // Set current user (done by TenantFilterInterceptor)
 * UserContext.setCurrentUser(userId);
 *
 * // Get current user (can be null for unauthenticated requests)
 * UUID userId = UserContext.getCurrentUser();
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

    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();

    // Numeric domain reference id (X-User-Reference-Id = users.reference_id = parents.id /
    // teachers.id / students.id). Used for ownership authz (GAP-798); null for admin/owner
    // who are not domain entities. Audit (created_by) uses CURRENT_USER (UUID) per GAP-795.
    private static final ThreadLocal<Long> CURRENT_REFERENCE_ID = new ThreadLocal<>();

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
    public static void setCurrentUser(UUID userId) {
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
    public static UUID getCurrentUser() {
        return CURRENT_USER.get();
    }

    /**
     * Sets the current numeric reference id (X-User-Reference-Id) for this thread.
     *
     * <p>Nullable: admin/owner users are not domain entities and have no reference id.
     *
     * @param referenceId numeric domain id (parents.id / teachers.id / students.id), or null
     */
    public static void setCurrentReferenceId(Long referenceId) {
        if (referenceId == null) {
            CURRENT_REFERENCE_ID.remove();
        } else {
            CURRENT_REFERENCE_ID.set(referenceId);
        }
    }

    /**
     * Gets the current numeric reference id for ownership authz (GAP-798).
     *
     * @return numeric domain id, or null if the actor has no reference id (admin/owner) or unset
     */
    public static Long getCurrentReferenceId() {
        return CURRENT_REFERENCE_ID.get();
    }

    /**
     * Clears the user context for this thread.
     * Must be called after request completion to prevent memory leaks.
     */
    public static void clear() {
        CURRENT_USER.remove();
        CURRENT_REFERENCE_ID.remove();
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
