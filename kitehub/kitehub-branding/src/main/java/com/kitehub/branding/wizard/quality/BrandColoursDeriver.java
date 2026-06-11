package com.kitehub.branding.wizard.quality;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.wizard.dto.BrandColours;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Deterministic brand-colour deriver (GAP-1146).
 *
 * <p>Resource Classification (per {@code ai-branding-guidelines.md} §1) drives
 * the {@code source} field; the palette is always {@code TEMPLATE} until the
 * full Analyzer → Planner → Executor pipeline persists an AI-derived theme.</p>
 *
 * <p><b>Algorithm (GAP-1146):</b> the palette now reflects the wizard
 * <em>tone</em> the owner selected (professional / friendly / energetic /
 * luxury) — each tone has its own 2-variant family with a distinct colour
 * direction. The organisation name seeds the variant <em>within</em> the tone
 * family, so the result is deterministic (same job → same colours, avoids flaky
 * FE preview) AND reflects the chosen style (changing tone visibly changes the
 * palette). The {@code templateId} is mixed into the variant seed so different
 * templates under the same tone can diverge.</p>
 *
 * <p>Before GAP-1146 the palette was a pure hash of {@code organizationName},
 * ignoring tone/template/audience entirely — so changing tone never changed the
 * preview. Jobs created before GAP-1146 carry a {@code null} tone and fall back
 * to the original 6-palette name hash (backward-compatible).</p>
 *
 * <p>WCAG: every palette pairs a dark {@code neutral} text colour with a white
 * {@code background} (≥ 7:1, AAA) per {@code ai-branding-guidelines.md} §5; the
 * primary/secondary/accent are brand accents, not body-text pairs.</p>
 *
 * @see <a href="../../../../../../../../.claude/rules/ai-branding-guidelines.md">§1 Resource Classification</a>
 */
@Component
public class BrandColoursDeriver {

    /**
     * Tone → palette family. Each family carries 2 WCAG-engineered variants with
     * a distinct colour direction so the 4 tones are visibly different:
     * professional = blue/slate (trust), friendly = warm amber (approachable),
     * energetic = red/orange (dynamic), luxury = purple/deep (premium).
     */
    private static final Map<String, BrandColours[]> TONE_PALETTES = Map.of(
            "professional", new BrandColours[]{
                    new BrandColours("#1a73e8", "#fbbc04", "#10b981", "#1f2937", "#ffffff",
                            BrandColours.Source.TEMPLATE),
                    new BrandColours("#2563eb", "#f59e0b", "#22c55e", "#0f172a", "#ffffff",
                            BrandColours.Source.TEMPLATE),
            },
            "friendly", new BrandColours[]{
                    new BrandColours("#d97706", "#0ea5e9", "#f59e0b", "#1c1917", "#ffffff",
                            BrandColours.Source.TEMPLATE),
                    new BrandColours("#ea580c", "#059669", "#facc15", "#1f2937", "#ffffff",
                            BrandColours.Source.TEMPLATE),
            },
            "energetic", new BrandColours[]{
                    new BrandColours("#dc2626", "#facc15", "#14b8a6", "#1c1917", "#ffffff",
                            BrandColours.Source.TEMPLATE),
                    new BrandColours("#f97316", "#0ea5e9", "#84cc16", "#111827", "#ffffff",
                            BrandColours.Source.TEMPLATE),
            },
            "luxury", new BrandColours[]{
                    new BrandColours("#7c3aed", "#ec4899", "#06b6d4", "#1e293b", "#ffffff",
                            BrandColours.Source.TEMPLATE),
                    new BrandColours("#6d28d9", "#d97706", "#8b5cf6", "#0c0a09", "#ffffff",
                            BrandColours.Source.TEMPLATE),
            });

    /**
     * Legacy fallback for pre-GAP-1146 jobs (tone == null) — keeps the original
     * 6-palette name hash so existing jobs render identically.
     */
    private static final BrandColours[] LEGACY_PALETTE = new BrandColours[]{
            new BrandColours("#1a73e8", "#fbbc04", "#10b981", "#1f2937", "#ffffff",
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
     * @param job persisted job (tone drives the palette family; org name + template
     *            seed the variant within it)
     * @return validated brand colour palette
     */
    public BrandColours derive(BrandingJob job) {
        String seed = job.getOrganizationName() == null ? job.getId().toString() : job.getOrganizationName();
        String tone = normalizeTone(job.getTone());

        if (tone == null) {
            // Pre-GAP-1146 job (no tone) → legacy name-hash palette (backward-compat).
            return LEGACY_PALETTE[Math.floorMod(seed.hashCode(), LEGACY_PALETTE.length)];
        }

        BrandColours[] family = TONE_PALETTES.get(tone);
        // Mix templateId into the variant seed so different templates under the same
        // tone can land on different variants (still deterministic per job).
        String variantSeed = seed + "|" + (job.getTemplateId() == null ? "" : job.getTemplateId());
        return family[Math.floorMod(variantSeed.hashCode(), family.length)];
    }

    /**
     * Normalize a raw tone value to a known family key, or {@code null} when the
     * tone is absent/unrecognised (→ legacy fallback).
     */
    private static String normalizeTone(String rawTone) {
        if (rawTone == null || rawTone.isBlank()) {
            return null;
        }
        String key = rawTone.trim().toLowerCase(Locale.ROOT);
        return TONE_PALETTES.containsKey(key) ? key : null;
    }
}
