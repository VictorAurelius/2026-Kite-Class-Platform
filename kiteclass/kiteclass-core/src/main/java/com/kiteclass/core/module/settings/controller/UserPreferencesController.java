package com.kiteclass.core.module.settings.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.settings.dto.request.UpdateUserPreferencesRequest;
import com.kiteclass.core.module.settings.dto.response.UserPreferencesResponse;
import com.kiteclass.core.module.settings.service.UserPreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;

    /**
     * Get user preferences for specific user.
     * TODO: Add authentication to get current user ID from JWT.
     *
     * @param userId user ID
     * @return user preferences
     */
    @GetMapping("/{userId}/preferences")
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> getUserPreferences(
            @PathVariable Long userId) {
        UserPreferencesResponse preferences = userPreferencesService.getUserPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    /**
     * Update user preferences for specific user.
     * Partial update (PATCH semantics).
     *
     * @param userId user ID
     * @param request update request
     * @return updated preferences
     */
    @PatchMapping("/{userId}/preferences")
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> updateUserPreferences(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserPreferencesRequest request) {
        UserPreferencesResponse preferences = userPreferencesService.updateUserPreferences(userId, request);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    /**
     * Initialize default preferences for new user.
     * Internal endpoint called during user registration.
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
}
