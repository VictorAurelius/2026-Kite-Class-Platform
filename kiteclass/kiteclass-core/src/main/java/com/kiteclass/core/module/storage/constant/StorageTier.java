package com.kiteclass.core.module.storage.constant;

import lombok.Getter;

/**
 * Storage quota tiers for tenants.
 *
 * <p>Defines storage limits for different subscription tiers:
 * <ul>
 *   <li>FREE: 1 GB storage</li>
 *   <li>BASIC: 10 GB storage</li>
 *   <li>PRO: 50 GB storage</li>
 *   <li>ENTERPRISE: 100 GB storage</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@Getter
public enum StorageTier {

    FREE("Miễn phí", "Free tier", 1L * 1024 * 1024 * 1024),           // 1 GB
    BASIC("Cơ bản", "Basic tier", 10L * 1024 * 1024 * 1024),          // 10 GB
    PRO("Chuyên nghiệp", "Professional tier", 50L * 1024 * 1024 * 1024), // 50 GB
    ENTERPRISE("Doanh nghiệp", "Enterprise tier", 100L * 1024 * 1024 * 1024); // 100 GB

    private final String displayNameVi;
    private final String description;
    private final long quotaBytes;

    StorageTier(String displayNameVi, String description, long quotaBytes) {
        this.displayNameVi = displayNameVi;
        this.description = description;
        this.quotaBytes = quotaBytes;
    }

    /**
     * Returns badge class for UI styling.
     *
     * @return Tailwind CSS classes
     */
    public String getBadgeClass() {
        return switch (this) {
            case FREE -> "bg-gray-100 text-gray-800";
            case BASIC -> "bg-blue-100 text-blue-800";
            case PRO -> "bg-purple-100 text-purple-800";
            case ENTERPRISE -> "bg-yellow-100 text-yellow-800";
        };
    }

    /**
     * Returns human-readable storage size.
     *
     * @return Formatted size string (e.g., "1 GB", "10 GB")
     */
    public String getDisplaySize() {
        long gb = quotaBytes / (1024 * 1024 * 1024);
        return gb + " GB";
    }
}
