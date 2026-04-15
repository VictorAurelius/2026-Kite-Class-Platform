package com.kiteclass.core.common.security;

/**
 * Removes XSS vectors from user-uploaded SVG.
 *
 * <p>Must strip:
 * <ul>
 *   <li>{@code <script>} elements (any case, any namespace)</li>
 *   <li>{@code on*} event-handler attributes (onclick, onload, …)</li>
 *   <li>{@code xlink:href} pointing off-origin (javascript:, data:, http://attacker/)</li>
 *   <li>Foreign-object embeds (iframe, embed, object)</li>
 * </ul>
 *
 * <p>MUST preserve: path, rect, circle, polygon, g, text, defs, filter, gradient, animate
 * (scope allows SVG branding assets to render).
 *
 * <p>Implementation provided in Sub-PR 4.2 (JSoup-based); see ADR-011.
 *
 * @since 3.23.0 (Wave 4 Sub-PR 4.0 scaffold)
 */
public interface SvgSanitizer {

    /**
     * @param rawSvg raw SVG markup from untrusted source (tenant upload)
     * @return sanitized SVG; never null; may be empty if input was all unsafe
     */
    String sanitize(String rawSvg);
}
