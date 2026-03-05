package com.kiteclass.core.module.settings.service;

import com.kiteclass.core.module.settings.dto.request.UpdateUserPreferencesRequest;
import com.kiteclass.core.module.settings.dto.response.UserPreferencesResponse;
import jakarta.validation.Valid;

/**
 * Service interface for UserPreferences management.
 *
 * @since 2.9
 */
public interface UserPreferencesService {

    /**
     * Get user preferences for current user.
     * Returns default preferences if not exists.
     *
     * @param userId user ID (Gateway User reference)
     * @return user preferences response
     */
    UserPreferencesResponse getUserPreferences(Long userId);

    /**
     * Update user preferences for current user.
     * Creates new preferences if not exists.
     *
     * @param userId user ID
     * @param request update request
     * @return updated preferences response
     */
    UserPreferencesResponse updateUserPreferences(Long userId, @Valid UpdateUserPreferencesRequest request);

    /**
     * Initialize default preferences for new user.
     * Called during user registration flow.
     *
     * @param userId user ID
     * @return initialized preferences response
     */
    UserPreferencesResponse initializeDefaultPreferences(Long userId);
}
