package com.kiteclass.core.module.settings.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.settings.dto.request.UpdateUserPreferencesRequest;
import com.kiteclass.core.module.settings.dto.response.UserPreferencesResponse;
import com.kiteclass.core.module.settings.entity.UserPreferences;
import com.kiteclass.core.module.settings.enums.Language;
import com.kiteclass.core.module.settings.enums.Theme;
import com.kiteclass.core.module.settings.mapper.UserPreferencesMapper;
import com.kiteclass.core.module.settings.repository.UserPreferencesRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

/**
 * Service implementation for UserPreferences management.
 *
 * @since 2.9
 */
@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class UserPreferencesServiceImpl implements UserPreferencesService {

    private final UserPreferencesRepository userPreferencesRepository;
    private final UserPreferencesMapper userPreferencesMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public UserPreferencesResponse getUserPreferences(Long userId) {
        UUID instanceId = TenantContext.getCurrentTenant();

        UserPreferences preferences = userPreferencesRepository
                .findByInstanceIdAndUserIdAndDeletedFalse(instanceId, userId)
                .orElseGet(() -> createDefaultPreferences(instanceId, userId));

        return userPreferencesMapper.toResponse(preferences);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserPreferencesResponse updateUserPreferences(Long userId, @Valid UpdateUserPreferencesRequest request) {
        UUID instanceId = TenantContext.getCurrentTenant();

        UserPreferences preferences = userPreferencesRepository
                .findByInstanceIdAndUserIdAndDeletedFalse(instanceId, userId)
                .orElseGet(() -> {
                    UserPreferences newPreferences = createDefaultPreferences(instanceId, userId);
                    return userPreferencesRepository.save(newPreferences);
                });

        // Update fields from request (PATCH semantics)
        userPreferencesMapper.updateFromRequest(request, preferences);

        preferences = userPreferencesRepository.save(preferences);

        log.info("Updated user preferences for user {} in instance {}", userId, instanceId);

        return userPreferencesMapper.toResponse(preferences);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserPreferencesResponse initializeDefaultPreferences(Long userId) {
        UUID instanceId = TenantContext.getCurrentTenant();

        // Check if preferences already exist
        if (userPreferencesRepository.existsByInstanceIdAndUserIdAndDeletedFalse(instanceId, userId)) {
            log.debug("User preferences already exist for user {} in instance {}", userId, instanceId);
            return getUserPreferences(userId);
        }

        UserPreferences preferences = createDefaultPreferences(instanceId, userId);
        preferences = userPreferencesRepository.save(preferences);

        log.info("Initialized default preferences for user {} in instance {}", userId, instanceId);

        return userPreferencesMapper.toResponse(preferences);
    }

    /**
     * Create default user preferences.
     *
     * @param instanceId tenant instance ID
     * @param userId     user ID
     * @return default preferences
     */
    private UserPreferences createDefaultPreferences(UUID instanceId, Long userId) {
        UserPreferences preferences = new UserPreferences();
        preferences.setInstanceId(instanceId);
        preferences.setUserId(userId);
        preferences.setLanguage(Language.VI);
        preferences.setTimezone("Asia/Ho_Chi_Minh");
        preferences.setTheme(Theme.LIGHT);
        preferences.initializeDefaultNotificationPreferences();

        return preferences;
    }
}
