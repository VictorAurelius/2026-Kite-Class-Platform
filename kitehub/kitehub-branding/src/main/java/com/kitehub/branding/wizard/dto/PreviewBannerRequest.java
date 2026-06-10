package com.kitehub.branding.wizard.dto;

import java.util.List;

/**
 * Request body for {@code POST /api/v1/branding/jobs/preview-banner} (Wizard
 * Step 7 live banner preview — GAP-1141).
 *
 * <p>The FE passes its already-computed deterministic palette + copy so the
 * backend can compose + rasterise a TEMPLATE-mode banner WITHOUT touching
 * Gemini, the DB, or the FULL_AI quota. Preview is always TEMPLATE so the
 * PREMIUM/ENTERPRISE FULL_AI quota is never consumed while the owner explores
 * (per {@code ai-branding-guidelines.md} §2.4 FULL_AI commits only on Deploy).</p>
 *
 * <p>All fields are nullable-tolerant — {@code colours} may be absent or carry
 * invalid hex, in which case the controller falls back to a safe default
 * palette so the preview never 500s.</p>
 *
 * @param organizationName tenant / centre display name (banner headline)
 * @param copy             marketing copy (banner subtitle source); nullable
 * @param logoUrl          uploaded logo URL (brand mark); nullable
 * @param portraitUrls     uploaded portrait URLs (GAP-1134 — first is featured); nullable
 * @param themeIcon        theme/subject icon (emoji or short text); nullable
 * @param colours          validated brand palette (5 hex); nullable → default
 * @param mode             requested generation mode: {@code "TEMPLATE"} (default,
 *                         free, never burns quota) or {@code "FULL_AI"} (GAP-1147 —
 *                         PREMIUM/ENTERPRISE on-demand AI banner; tier-gated +
 *                         quota-metered server-side, falls back to TEMPLATE when
 *                         ineligible/exhausted). Nullable → TEMPLATE.
 * @since GAP-1141 (GAP-1147 adds {@code mode})
 */
public record PreviewBannerRequest(
        String organizationName,
        String copy,
        String logoUrl,
        List<String> portraitUrls,
        String themeIcon,
        BrandColours colours,
        String mode
) {
}
