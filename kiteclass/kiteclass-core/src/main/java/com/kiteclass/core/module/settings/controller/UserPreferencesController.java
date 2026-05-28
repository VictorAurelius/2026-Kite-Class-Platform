package com.kiteclass.core.module.settings.controller;

import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.module.settings.dto.request.UpdateUserPreferencesRequest;
import com.kiteclass.core.module.settings.dto.response.UserPreferencesResponse;
import com.kiteclass.core.module.settings.service.UserPreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for UserPreferences management.
 *
 * @since 2.9
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;

    /**
     * Get user preferences for specific user.
     *
     * <p>Security: User can only access their own preferences.
     * The userId in path must match authenticated user ID from JWT token.
     *
     * @param userId user ID from path
     * @return user preferences
     * @throws PermissionDeniedException if userId doesn't match authenticated user
     */
    @GetMapping("/{userId}/preferences")
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> getUserPreferences(
            @PathVariable Long userId) {
        validateUserAccess(userId);
        UserPreferencesResponse preferences = userPreferencesService.getUserPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    /**
     * Update user preferences for specific user.
     * Partial update (PATCH semantics).
     *
     * <p>Security: User can only update their own preferences.
     * The userId in path must match authenticated user ID from JWT token.
     *
     * @param userId user ID from path
     * @param request update request
     * @return updated preferences
     * @throws PermissionDeniedException if userId doesn't match authenticated user
     */
    @PatchMapping("/{userId}/preferences")
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> updateUserPreferences(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserPreferencesRequest request) {
        validateUserAccess(userId);
        UserPreferencesResponse preferences = userPreferencesService.updateUserPreferences(userId, request);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    /**
     * Initialize default preferences for new user.
     * Internal endpoint called during user registration.
     *
     * <p>Note: This is typically called by the Gateway during user registration flow.
     * No authentication check needed as it's an internal service call.
     *
     * @param userId user ID
     * @return initialized preferences
     */
    @PostMapping("/{userId}/preferences/initialize")
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> initializePreferences(
            @PathVariable Long userId) {
        UserPreferencesResponse preferences = userPreferencesService.initializeDefaultPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    /**
     * Validates that the requested userId matches the authenticated user.
     *
     * <p>Security check: Prevents users from accessing/modifying other users' preferences.
     *
     * <p><strong>PARTIAL (GAP-795):</strong> the authenticated actor identity is now a
     * {@link java.util.UUID} ({@link UserContext#getCurrentUser()}), but this module keys
     * preferences on a numeric {@code user_preferences.user_id} (BIGINT) and the path
     * variable is {@code Long}. There is NO bridge from the actor UUID to that numeric
     * user_id, so own-resource ownership cannot be evaluated → the check fails closed
     * (deny). This matches the prior effective behavior (pre-GAP-795 the Long.parseLong(UUID)
     * throw left {@code UserContext} null → {@code USER_NOT_AUTHENTICATED}).
     *
     * <p>TODO(GAP-795 follow-up): migrate the user-preferences module to key on the actor
     * UUID (path + {@code user_id} column + service), then restore the equality check.
     *
     * @param requestedUserId user ID from path parameter
     * @throws PermissionDeniedException always for non-resolvable actor (see above)
     */
    private void validateUserAccess(Long requestedUserId) {
        java.util.UUID authenticatedUserId = UserContext.getCurrentUser();

        if (authenticatedUserId == null) {
            throw new PermissionDeniedException("USER_NOT_AUTHENTICATED");
        }

        // GAP-795: actor UUID vs numeric path user_id is unbridgeable → fail closed.
        log.warn("UserPreferences.validateUserAccess: deny — actor UUID {} has no numeric "
                + "user_id bridge (GAP-795 PARTIAL; requestedUserId={})",
                authenticatedUserId, requestedUserId);
        throw new PermissionDeniedException("USER_ACCESS_DENIED");
    }
}
