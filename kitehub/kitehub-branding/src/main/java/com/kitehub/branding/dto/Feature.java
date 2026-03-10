package com.kitehub.branding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Feature highlight for landing page.
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feature {

    /**
     * Feature title (e.g., "Học Trực Tuyến Linh Hoạt").
     */
    private String title;

    /**
     * Feature description (1-2 sentences).
     */
    private String description;

    /**
     * Optional icon name (e.g., "video", "calendar", "trophy").
     */
    private String icon;
}
