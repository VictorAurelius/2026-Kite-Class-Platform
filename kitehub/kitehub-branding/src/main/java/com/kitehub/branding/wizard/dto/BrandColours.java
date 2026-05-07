package com.kitehub.branding.wizard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.regex.Pattern;

/**
 * Brand colour palette value object.
 *
 * <p>Validated hex strings ({@code #RRGGBB}) per {@code design-patterns.md}
 * §3.2 Primitive Obsession — replaces raw {@code String} colours. Field set
 * matches {@code documents/01-business/kitehub/ai-branding/api-contract.md}
 * v1 jobs/{id} response (sub-GAP-272k): primary, secondary, accent, neutral,
 * background.</p>
 *
 * <p>{@code source} captures which Resource Classification path produced the
 * colours (per {@code ai-branding-guidelines.md} §1):
 * {@code TEMPLATE | AI_ANALYSIS | USER_OVERRIDE}.</p>
 *
 * @since 1.0
 */
public record BrandColours(
        @JsonProperty("primary") String primary,
        @JsonProperty("secondary") String secondary,
        @JsonProperty("accent") String accent,
        @JsonProperty("neutral") String neutral,
        @JsonProperty("background") String background,
        @JsonProperty("source") Source source
) {

    private static final Pattern HEX_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    public BrandColours {
        validateHex("primary", primary);
        validateHex("secondary", secondary);
        validateHex("accent", accent);
        validateHex("neutral", neutral);
        validateHex("background", background);
        if (source == null) {
            source = Source.TEMPLATE;
        }
    }

    private static void validateHex(String field, String value) {
        if (value == null || !HEX_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid hex colour for " + field + ": " + value + " (expected #RRGGBB)");
        }
    }

    /**
     * Resource Classification source for the brand colours.
     *
     * @see <a href="../../../../../../../../.claude/rules/ai-branding-guidelines.md">§1</a>
     */
    public enum Source {
        TEMPLATE,
        AI_ANALYSIS,
        USER_OVERRIDE
    }
}
