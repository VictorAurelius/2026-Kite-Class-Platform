package com.kitehub.branding.wizard.quality;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.wizard.dto.BrandColours;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-1146 — the deriver must reflect the wizard tone, not just hash the org name.
 *
 * Verifies:
 *  - changing tone changes the palette (4 tones → 4 distinct colour directions)
 *  - same job (org + tone) → same palette (deterministic, avoids flaky FE preview)
 *  - tone == null → legacy name-hash palette (backward-compat for pre-GAP-1146 jobs)
 *  - every palette is a white-background / dark-neutral pair (WCAG AA body text)
 */
class BrandColoursDeriverTest {

    private final BrandColoursDeriver deriver = new BrandColoursDeriver();

    private static BrandingJob job(String orgName, String tone, String templateId) {
        BrandingJob job = new BrandingJob();
        job.setId(UUID.randomUUID());
        job.setOrganizationName(orgName);
        job.setTone(tone);
        job.setTemplateId(templateId);
        return job;
    }

    @Test
    void differentTones_yieldDistinctPrimaryColours() {
        // Same org name, 4 different tones → 4 different primary colours.
        String org = "Trung tâm Anh ngữ Sky Education";
        Set<String> primaries = new HashSet<>();
        for (String tone : List.of("professional", "friendly", "energetic", "luxury")) {
            primaries.add(deriver.derive(job(org, tone, "T1")).primary());
        }
        // All 4 tones map to a distinct colour direction.
        assertThat(primaries).hasSize(4);
    }

    @Test
    void sameJob_isDeterministic() {
        BrandColours a = deriver.derive(job("Trung tâm Quang Minh", "professional", "T2"));
        BrandColours b = deriver.derive(job("Trung tâm Quang Minh", "professional", "T2"));
        assertThat(a.primary()).isEqualTo(b.primary());
        assertThat(a.secondary()).isEqualTo(b.secondary());
        assertThat(a.accent()).isEqualTo(b.accent());
    }

    @Test
    void nullTone_fallsBackToLegacyPalette() {
        // Pre-GAP-1146 job (no tone) still derives a valid palette deterministically.
        BrandColours a = deriver.derive(job("Trung tâm Cũ", null, null));
        BrandColours b = deriver.derive(job("Trung tâm Cũ", null, null));
        assertThat(a.primary()).isEqualTo(b.primary());
        assertThat(a.source()).isEqualTo(BrandColours.Source.TEMPLATE);
    }

    @Test
    void unknownTone_fallsBackToLegacyPalette() {
        // A stray/unrecognised tone never throws — it degrades to the legacy hash.
        BrandColours c = deriver.derive(job("Trung tâm X", "không-hợp-lệ", null));
        assertThat(c.primary()).matches("^#[0-9a-fA-F]{6}$");
    }

    @Test
    void everyTonePalette_neutralOnBackground_meetsWcagAa() {
        // The body-text pair (neutral on background=white) must clear WCAG AA (4.5:1)
        // per ai-branding-guidelines.md §5 — verified with the real WCAG 2.1 formula.
        for (String tone : List.of("professional", "friendly", "energetic", "luxury")) {
            BrandColours c = deriver.derive(job("Trung tâm WCAG", tone, "T1"));
            assertThat(c.background()).isEqualToIgnoringCase("#ffffff");
            assertThat(contrastRatio(c.neutral(), c.background()))
                    .as("neutral %s on %s for tone %s", c.neutral(), c.background(), tone)
                    .isGreaterThanOrEqualTo(4.5);
        }
    }

    /** WCAG 2.1 contrast ratio between two hex colours (1.0 .. 21.0). */
    private static double contrastRatio(String hexA, String hexB) {
        double la = relativeLuminance(hexA);
        double lb = relativeLuminance(hexB);
        double lighter = Math.max(la, lb);
        double darker = Math.min(la, lb);
        return (lighter + 0.05) / (darker + 0.05);
    }

    /** WCAG 2.1 relative luminance (sRGB linearised). */
    private static double relativeLuminance(String hex) {
        double r = channel(Integer.parseInt(hex.substring(1, 3), 16));
        double g = channel(Integer.parseInt(hex.substring(3, 5), 16));
        double b = channel(Integer.parseInt(hex.substring(5, 7), 16));
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double channel(int raw) {
        double c = raw / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
