package com.kiteclass.core.module.settings.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for updating user preferences.
 * All fields are optional (PATCH update).
 *
 * @since 2.9
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserPreferencesRequest {

    @Pattern(regexp = "^(en|vi)$", message = "Language must be 'en' or 'vi'")
    @Size(max = 5, message = "Language code must not exceed 5 characters")
    private String language;

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;

    @Pattern(regexp = "^(light|dark|auto)$", message = "Theme must be 'light', 'dark', or 'auto'")
    @Size(max = 10, message = "Theme must not exceed 10 characters")
    private String theme;

    private Map<String, Boolean> notificationPreferences;
}
