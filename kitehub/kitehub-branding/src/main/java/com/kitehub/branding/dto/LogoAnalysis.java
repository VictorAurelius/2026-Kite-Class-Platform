package com.kitehub.branding.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Logo analysis result from AI vision model (Ollama llava:13b or GPT-4 Vision).
 * Frontend expects single color values, not arrays.
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
     * Primary brand color (hex code, e.g., "#FF5733").
     */
    @NotBlank(message = "primaryColor is required")
    private String primaryColor;

    /**
     * Secondary brand color (hex code, e.g., "#3498DB").
     */
    @NotBlank(message = "secondaryColor is required")
    private String secondaryColor;

    /**
     * Accent color for highlights and CTAs (hex code, e.g., "#F39C12").
     */
    @NotBlank(message = "accentColor is required")
    private String accentColor;

    /**
     * Design theme enum: MODERN, CLASSIC, PLAYFUL, MINIMAL.
     */
    @NotBlank(message = "theme is required")
    private String theme;

    /**
     * Typography style (e.g., "Modern Sans-serif", "Classic Serif").
     */
    private String typography;

    /**
     * Target audience description.
     */
    private String targetAudience;

    /**
     * Brand personality traits (e.g., ["Professional", "Friendly", "Innovative"]).
     */
    private List<String> brandPersonality;

    /**
     * Raw analysis text from AI vision model.
     */
    private String rawAnalysis;
}
