package com.kitehub.branding.service.banner;

import com.kitehub.branding.wizard.dto.BrandColours;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BannerHtmlComposer} (GAP-1135) — the real (deterministic)
 * TEMPLATE banner HTML.
 */
class BannerHtmlComposerTest {

    private final BannerHtmlComposer composer = new BannerHtmlComposer();

    private static final BrandColours COLOURS = new BrandColours(
            "#1a73e8", "#fbbc04", "#10B981", "#1f2937", "#ffffff",
            BrandColours.Source.TEMPLATE);

    @Test
    @DisplayName("compose embeds org name, copy, brand colours, portrait, CTA")
    void composeEmbedsInputs() {
        BannerComposition c = composer.compose(
                "Trung tâm Anh ngữ ABC",
                "Mất gốc tiếng Anh? Đã có cô Khánh.",
                "https://cdn/instances/x/logo.png",
                List.of("https://cdn/instances/x/portrait-1.png"),
                "👩‍🏫",
                COLOURS);

        assertThat(c.width()).isEqualTo(1200);
        assertThat(c.height()).isEqualTo(630);

        String html = c.html();
        assertThat(html).startsWith("<!DOCTYPE html>");
        assertThat(html).contains("Trung tâm Anh ngữ ABC");        // headline
        assertThat(html).contains("Mất gốc tiếng Anh");            // copy subtitle
        assertThat(html).contains("#1a73e8");                       // primary colour
        assertThat(html).contains("portrait-1.png");                // featured portrait
        assertThat(html).contains("logo.png");                      // brand mark
        assertThat(html).contains("Đăng ký học thử");               // CTA
        assertThat(html).contains("👩‍🏫");                          // theme icon
    }

    @Test
    @DisplayName("compose falls back to icon layer when no portraits uploaded")
    void composeIconFallbackWhenNoPortrait() {
        BannerComposition c = composer.compose(
                "Solo Teacher", "Học cùng cô.", null, List.of(), "📚", COLOURS);

        assertThat(c.html()).contains("portrait--icon");
        assertThat(c.html()).doesNotContain("<img class=\"logo\"");  // no logo → mark, not img
    }

    @Test
    @DisplayName("compose HTML-escapes tenant text (sanitize-on-write)")
    void composeEscapesText() {
        BannerComposition c = composer.compose(
                "<script>alert(1)</script>", "copy", null, List.of(), null, COLOURS);

        assertThat(c.html()).doesNotContain("<script>alert(1)</script>");
        assertThat(c.html()).contains("&lt;script&gt;");
    }
}
