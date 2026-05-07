package com.kitehub.branding.wizard.quality;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.wizard.dto.BrandColours;
import org.springframework.stereotype.Component;

/**
 * Deterministic v0 brand-colour deriver.
 *
 * <p>Resource Classification (per {@code ai-branding-guidelines.md} §1) drives
 * the {@code source} field; for now we always return TEMPLATE because the
 * full Analyzer → Planner → Executor pipeline (Wave 4 scaffold) does not
 * yet persist a {@code BrandingTheme} row keyed by job. When that lands,
 * this deriver will read the persisted theme and surface
 * {@code AI_ANALYSIS} / {@code USER_OVERRIDE}.</p>
 *
 * <p>The v0 algorithm hashes {@code organizationName} into one of six
 * curated education-friendly palettes — deterministic so the same job
 * yields the same colours across calls (avoids flaky FE preview).</p>
 *
 * <p>TODO Wave 35+: replace with persisted theme lookup once Bucket A's
 * {@code BrandingRegenerateUsage} writer extends to theme persistence
 * (or a sibling {@code BrandingThemeRepository} lands).</p>
 *
 * @see <a href="../../../../../../../../.claude/rules/ai-branding-guidelines.md">§1 Resource Classification</a>
 */
@Component
public class BrandColoursDeriver {

    private static final BrandColours[] PALETTE = new BrandColours[]{
            new BrandColours("#1a73e8", "#fbbc04", "#10B981", "#1f2937", "#ffffff",
                    BrandColours.Source.TEMPLATE),
            new BrandColours("#2563eb", "#f59e0b", "#22c55e", "#0f172a", "#ffffff",
                    BrandColours.Source.TEMPLATE),
            new BrandColours("#7c3aed", "#ec4899", "#06b6d4", "#1e293b", "#ffffff",
                    BrandColours.Source.TEMPLATE),
            new BrandColours("#0ea5e9", "#f97316", "#84cc16", "#111827", "#ffffff",
                    BrandColours.Source.TEMPLATE),
            new BrandColours("#dc2626", "#facc15", "#14b8a6", "#1c1917", "#ffffff",
                    BrandColours.Source.TEMPLATE),
            new BrandColours("#059669", "#d97706", "#8b5cf6", "#0c0a09", "#ffffff",
                    BrandColours.Source.TEMPLATE),
    };

    /**
     * Derive deterministic colours for a job.
     *
     * @param job persisted job
     * @return validated brand colour palette
     */
    public BrandColours derive(BrandingJob job) {
        String key = job.getOrganizationName() == null ? job.getId().toString() : job.getOrganizationName();
        int idx = Math.floorMod(key.hashCode(), PALETTE.length);
        return PALETTE[idx];
    }
}
