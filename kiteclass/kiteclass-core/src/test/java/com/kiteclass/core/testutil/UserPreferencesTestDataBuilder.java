package com.kiteclass.core.testutil;

import com.kiteclass.core.module.settings.entity.UserPreferences;
import com.kiteclass.core.module.settings.enums.Language;
import com.kiteclass.core.module.settings.enums.Theme;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Test data builder for UserPreferences entity.
 *
 * @since 2.9
 */
public final class UserPreferencesTestDataBuilder {

    private UserPreferencesTestDataBuilder() {
        // Utility class
    }

    /**
     * Create default user preferences for testing.
     *
     * @param instanceId tenant instance ID
     * @param userId     user ID
     * @return user preferences entity
     */
    public static UserPreferences createDefaultPreferences(UUID instanceId, Long userId) {
        UserPreferences preferences = new UserPreferences();
        preferences.setInstanceId(instanceId);
        preferences.setUserId(userId);
        preferences.setLanguage(Language.VI);
        preferences.setTimezone("Asia/Ho_Chi_Minh");
        preferences.setTheme(Theme.LIGHT);

        Map<String, Boolean> notificationPrefs = new HashMap<>();
        notificationPrefs.put("email", true);
        notificationPrefs.put("push", true);
        notificationPrefs.put("sms", false);
        preferences.setNotificationPreferences(notificationPrefs);

        return preferences;
    }

    /**
     * Create preferences with English language.
     *
     * @param instanceId tenant instance ID
     * @param userId     user ID
     * @return user preferences entity
     */
    public static UserPreferences createEnglishPreferences(UUID instanceId, Long userId) {
        UserPreferences preferences = createDefaultPreferences(instanceId, userId);
        preferences.setLanguage(Language.EN);
        preferences.setTimezone("America/New_York");
        return preferences;
    }

    /**
     * Create preferences with dark theme.
     *
     * @param instanceId tenant instance ID
     * @param userId     user ID
     * @return user preferences entity
     */
    public static UserPreferences createDarkThemePreferences(UUID instanceId, Long userId) {
        UserPreferences preferences = createDefaultPreferences(instanceId, userId);
        preferences.setTheme(Theme.DARK);
        return preferences;
    }

    /**
     * Create preferences with custom notification settings.
     *
     * @param instanceId      tenant instance ID
     * @param userId          user ID
     * @param emailEnabled    email notification enabled
     * @param pushEnabled     push notification enabled
     * @param smsEnabled      SMS notification enabled
     * @return user preferences entity
     */
    public static UserPreferences createPreferencesWithNotifications(
            UUID instanceId,
            Long userId,
            boolean emailEnabled,
            boolean pushEnabled,
            boolean smsEnabled) {
        UserPreferences preferences = createDefaultPreferences(instanceId, userId);

        Map<String, Boolean> notificationPrefs = new HashMap<>();
        notificationPrefs.put("email", emailEnabled);
        notificationPrefs.put("push", pushEnabled);
        notificationPrefs.put("sms", smsEnabled);
        preferences.setNotificationPreferences(notificationPrefs);

        return preferences;
    }
}
