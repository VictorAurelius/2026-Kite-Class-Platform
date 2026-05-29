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
     * <p><strong>GAP-798 — reference-id bridge.</strong> {@code user_preferences.user_id}
     * is numeric (= {@code users.reference_id}). The actor's numeric reference id arrives on
     * {@code X-User-Reference-Id} ({@link UserContext#getCurrentReferenceId()}); audit uses
     * the UUID {@code X-User-Id} separately (GAP-795). Ownership = actor reference-id == path
     * user_id. No actor-UUID → numeric bridge column needed — reference-id IS the numeric id.
     *
     * @param requestedUserId user ID from path parameter
     * @throws PermissionDeniedException if the actor's reference-id doesn't match the path user_id
     */
    private void validateUserAccess(Long requestedUserId) {
        Long actorReferenceId = UserContext.getCurrentReferenceId();

        if (actorReferenceId == null) {
            throw new PermissionDeniedException("USER_NOT_AUTHENTICATED");
        }

        if (!actorReferenceId.equals(requestedUserId)) {
            log.warn("UserPreferences.validateUserAccess: deny — actor ref-id {} != requested {}",
                    actorReferenceId, requestedUserId);
            throw new PermissionDeniedException("USER_ACCESS_DENIED");
        }
    }
}
