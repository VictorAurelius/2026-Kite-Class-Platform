package com.kiteclass.core.module.settings.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for UserPreferences.
 *
 * @since 2.9
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferencesResponse {
    private Long id;
    private Long userId;
    private String language;
    private String timezone;
    private String theme;
    private Map<String, Boolean> notificationPreferences;
}
