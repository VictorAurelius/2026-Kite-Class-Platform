package com.kiteclass.core.module.storage.constant;

import lombok.Getter;

/**
 * Access control levels for uploaded files.
 *
 * <p>Defines who can access uploaded files:
 * <ul>
 *   <li>PUBLIC: Anyone with the URL can access</li>
 *   <li>PRIVATE: Only the uploader can access</li>
 *   <li>TENANT: Anyone in the same tenant can access</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@Getter
public enum AccessLevel {

    PUBLIC("Công khai", "Anyone with the URL can access"),
    PRIVATE("Riêng tư", "Only the uploader can access"),
    TENANT("Nội bộ", "Anyone in the same tenant can access");

    private final String displayNameVi;
    private final String description;

    AccessLevel(String displayNameVi, String description) {
        this.displayNameVi = displayNameVi;
        this.description = description;
    }

    /**
     * Returns badge class for UI styling.
     *
     * @return Tailwind CSS classes
     */
    public String getBadgeClass() {
        return switch (this) {
            case PUBLIC -> "bg-green-100 text-green-800";
            case PRIVATE -> "bg-red-100 text-red-800";
            case TENANT -> "bg-blue-100 text-blue-800";
        };
    }
}
