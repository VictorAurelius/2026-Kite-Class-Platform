package com.kiteclass.core.module.settings.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Theme enum for user interface appearance.
 *
 * @since 2.9
 */
@Getter
@RequiredArgsConstructor
public enum Theme {
    /**
     * Light theme (default).
     */
    LIGHT("light", "Light Mode"),

    /**
     * Dark theme.
     */
    DARK("dark", "Dark Mode"),

    /**
     * Auto theme (follows system preference).
     */
    AUTO("auto", "Auto (System)");

    private final String code;
    private final String displayName;

    /**
     * Get Theme from code.
     *
     * @param code theme code (e.g., "light", "dark", "auto")
     * @return Theme enum
     * @throws IllegalArgumentException if code is invalid
     */
    public static Theme fromCode(String code) {
        for (Theme theme : values()) {
            if (theme.code.equalsIgnoreCase(code)) {
                return theme;
            }
        }
        throw new IllegalArgumentException("Invalid theme code: " + code);
    }
}
