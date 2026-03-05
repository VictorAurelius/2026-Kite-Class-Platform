package com.kiteclass.core.module.settings.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.settings.enums.Language;
import com.kiteclass.core.module.settings.enums.Theme;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * UserPreferences entity for user-specific settings.
 *
 * <p>Stores locale (language, timezone), UI theme, and notification preferences.
 * One preferences record per user (user_id).
 *
 * @since 2.9
 */
@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences extends BaseEntity {

    /**
     * User ID (reference to Gateway User via reference_id).
     * This is a cross-service link.
     */
    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Locale settings

    /**
     * UI language.
     */
    @NotNull(message = "Language is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 5)
    private Language language = Language.VI;

    /**
     * User timezone (IANA timezone identifier).
     */
    @NotBlank(message = "Timezone is required")
    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "Asia/Ho_Chi_Minh";

    // UI preferences

    /**
     * UI theme preference.
     */
    @NotNull(message = "Theme is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "theme", nullable = false, length = 10)
    private Theme theme = Theme.LIGHT;

    // Notification preferences (JSON)

    /**
     * Notification channel preferences.
     *
     * <p>Example:
     * <pre>
     * {
     *   "email": true,
     *   "push": true,
     *   "sms": false
     * }
     * </pre>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notification_preferences", columnDefinition = "jsonb", nullable = false)
    private Map<String, Boolean> notificationPreferences = new HashMap<>();

    /**
     * Initialize default notification preferences.
     */
    public void initializeDefaultNotificationPreferences() {
        if (notificationPreferences == null) {
            notificationPreferences = new HashMap<>();
        }
        notificationPreferences.putIfAbsent("email", true);
        notificationPreferences.putIfAbsent("push", true);
        notificationPreferences.putIfAbsent("sms", false);
    }
}
