package com.kiteclass.core.module.settings.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Language enum for user interface localization.
 *
 * @since 2.9
 */
@Getter
@RequiredArgsConstructor
public enum Language {
    /**
     * English.
     */
    EN("en", "English"),

    /**
     * Vietnamese.
     */
    VI("vi", "Tiếng Việt");

    private final String code;
    private final String displayName;

    /**
     * Get Language from code.
     *
     * @param code language code (e.g., "en", "vi")
     * @return Language enum
     * @throws IllegalArgumentException if code is invalid
     */
    public static Language fromCode(String code) {
        for (Language language : values()) {
            if (language.code.equalsIgnoreCase(code)) {
                return language;
            }
        }
        throw new IllegalArgumentException("Invalid language code: " + code);
    }
}
