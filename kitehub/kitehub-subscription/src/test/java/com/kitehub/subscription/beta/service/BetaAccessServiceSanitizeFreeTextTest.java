package com.kitehub.subscription.beta.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression-guard unit tests for {@link BetaAccessService#sanitizeFreeText}
 * — Wave 106 GAP-764 fix (UTF-8 preserve for Vietnamese diacritics).
 *
 * <p><b>Background (GAP-764):</b> Wave 105 Bucket E0 Bug 2 introduced
 * defense-in-depth XSS sanitization via {@code HtmlUtils.htmlEscape(stripped)}
 * single-arg variant. That variant escapes ALL non-ASCII chars as numeric
 * character references — Vietnamese diacritics ({@code â/ê/ô/ữ/ồ/...}) got
 * corrupted to entities like {@code &acirc;/&ecirc;/&ocirc;}.</p>
 *
 * <p>Wave 106 RST Mảng A2 walk caught 2 production rows corrupted (id 11+12).
 * Fix changed to {@code HtmlUtils.htmlEscape(stripped, "UTF-8")} two-arg variant
 * — escapes ONLY 5 XSS chars ({@code <>&"'}), preserves VN diacritic raw.</p>
 *
 * <p>This test class is the <b>paired regression-guard</b> per
 * {@code .claude/rules/e2e-rst-test-layer-boundary.md} §3 RST→E2E promotion
 * mandate + Wave 106 plan §7.5 mandate "bug fix PR PHẢI paired NEW spec same
 * PR cho deterministic API contract bugs". Full service-layer Testcontainers
 * IT roundtrip defers to GAP-769 (broader scope).</p>
 *
 * <p>Reference rule: {@code .claude/rules/vn-localization-audit-checklist.md}
 * v1.1.0 §5 "Data roundtrip preservation through sanitization layers" —
 * mandate this test exists prospectively for ANY future input sanitization
 * touching tenant-facing field.</p>
 *
 * @since Wave 106 GAP-764 fix
 */
@DisplayName("BetaAccessService.sanitizeFreeText — VN diacritic preserve + XSS escape (GAP-764)")
class BetaAccessServiceSanitizeFreeTextTest {

    @Test
    @DisplayName("null input returns null (no NPE)")
    void nullInput() {
        assertThat(BetaAccessService.sanitizeFreeText(null)).isNull();
    }

    @Test
    @DisplayName("empty string returns empty")
    void emptyInput() {
        assertThat(BetaAccessService.sanitizeFreeText("")).isEmpty();
    }

    // -- Vietnamese diacritic preservation (GAP-764 regression-guard) ----

    @ParameterizedTest(name = "VN diacritic preserved: {0}")
    @CsvSource({
        // 7 most-frequent VN diacritics (covers ~95% Vietnamese names)
        "'Trần Thị Hồng',                'Trần Thị Hồng'",
        "'Nguyễn Văn An',                'Nguyễn Văn An'",
        "'Phạm Thị Mai',                 'Phạm Thị Mai'",
        "'Lê Văn Quang',                 'Lê Văn Quang'",
        "'Trung tâm Anh ngữ Sky',        'Trung tâm Anh ngữ Sky'",
        "'Trung tâm Toán Quang Minh',    'Trung tâm Toán Quang Minh'",
        // Mixed VN + technical token (acceptable code-switching per dev-readable-doc-language §4)
        "'Lớp IELTS 7.0 Buổi tối',       'Lớp IELTS 7.0 Buổi tối'",
        // All 7 frequent diacritics in one input
        "'âêôữồằấÂÊÔỮỒẰẤ',                'âêôữồằấÂÊÔỮỒẰẤ'"
    })
    void vnDiacriticPreservedRaw(String input, String expected) {
        String actual = BetaAccessService.sanitizeFreeText(input);
        assertThat(actual)
                .as("VN diacritic MUST preserve raw UTF-8 per vn-localization-audit-checklist.md §5")
                .isEqualTo(expected)
                .doesNotContain("&acirc;")
                .doesNotContain("&ecirc;")
                .doesNotContain("&ocirc;")
                .doesNotContain("&aacute;");
    }

    // -- XSS char escape (defense-in-depth still works) ------------------

    @ParameterizedTest(name = "XSS char escaped: {0}")
    @CsvSource({
        "'Test & Co',           'Test &amp; Co'",
        "'Quoted \"name\"',     'Quoted &quot;name&quot;'",
        "'Apostrophe d''Or',    'Apostrophe d&#39;Or'"
    })
    void xssCharEscapedWhenPresent(String input, String expected) {
        String actual = BetaAccessService.sanitizeFreeText(input);
        assertThat(actual)
                .as("XSS chars MUST escape to HTML entity per defense-in-depth")
                .isEqualTo(expected);
    }

    // -- HTML tag stripping (Wave 105 Bucket E0 Bug 2 original behavior) -

    @Test
    @DisplayName("HTML tags stripped — <script>alert('xss')</script> → alert('xss') text only")
    void htmlTagsStripped() {
        String input = "Test <script>alert('xss')</script> end";
        String actual = BetaAccessService.sanitizeFreeText(input);
        assertThat(actual)
                .as("HTML tag sequences MUST strip per Wave 105 Bucket E0 Bug 2")
                .doesNotContain("<script>")
                .doesNotContain("</script>")
                .doesNotContain("<")
                .doesNotContain(">")
                .contains("Test")
                .contains("end");
    }

    @Test
    @DisplayName("<img onerror> XSS variant stripped")
    void htmlImgOnerrorStripped() {
        String input = "Avatar <img src=x onerror=alert(1)> upload";
        String actual = BetaAccessService.sanitizeFreeText(input);
        assertThat(actual)
                .doesNotContain("<img")
                .doesNotContain("onerror")
                .contains("Avatar")
                .contains("upload");
    }

    // -- Combined: VN diacritic + XSS char + HTML tag ---------------------

    @Test
    @DisplayName("Combined: VN diacritic preserved AND XSS char escaped AND HTML tag stripped")
    void combinedSanitization() {
        String input = "Trần & Thị <script>evil</script> tâm";
        String actual = BetaAccessService.sanitizeFreeText(input);
        assertThat(actual)
                .as("All three behaviors applied correctly in one pass")
                .contains("Trần")           // VN preserved
                .contains("Thị")            // VN preserved
                .contains("tâm")            // VN preserved (the original GAP-764 case)
                .contains("&amp;")          // XSS char escaped
                .doesNotContain("<script>") // HTML tag stripped
                .doesNotContain("&acirc;"); // not corrupted to HTML entity
    }
}
