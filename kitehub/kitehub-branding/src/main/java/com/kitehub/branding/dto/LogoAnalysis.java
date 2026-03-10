package com.kitehub.branding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Logo analysis result from GPT-4 Vision.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoAnalysis {

    /**
     * Primary brand colors (hex codes).
     */
    private List<String> primaryColors;

    /**
     * Secondary brand colors (hex codes).
     */
    private List<String> secondaryColors;

    /**
     * Design theme (modern, traditional, playful, professional).
     */
    private String theme;

    /**
     * Typography style.
     */
    private String typography;

    /**
     * Target audience description.
     */
    private String targetAudience;

    /**
     * Brand personality traits.
     */
    private List<String> brandPersonality;

    /**
     * Raw analysis text from GPT-4 Vision.
     */
    private String rawAnalysis;
}
