package com.kiteclass.core.common.security.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsoupSvgSanitizerTest {

    private final JsoupSvgSanitizer sanitizer = new JsoupSvgSanitizer();

    @Test
    void nullInput_returnsEmpty() {
        assertThat(sanitizer.sanitize(null)).isEmpty();
    }

    @Test
    void blankInput_returnsEmpty() {
        assertThat(sanitizer.sanitize("   ")).isEmpty();
    }

    @Test
    void stripsScriptTag() {
        String input = "<svg xmlns='http://www.w3.org/2000/svg'>"
                + "<script>alert('xss')</script>"
                + "<path d='M0 0 L10 10'/></svg>";

        String out = sanitizer.sanitize(input);

        assertThat(out).doesNotContain("<script").doesNotContain("alert");
        assertThat(out).contains("<path");
    }

    @Test
    void stripsOnloadHandler() {
        String input = "<svg onload=\"alert(1)\"><circle cx='5' cy='5' r='3' onclick='steal()'/></svg>";

        String out = sanitizer.sanitize(input);

        assertThat(out.toLowerCase()).doesNotContain("onload").doesNotContain("onclick").doesNotContain("alert");
        assertThat(out).contains("<circle");
    }

    @Test
    void stripsJavascriptUrlInHref() {
        String input = "<svg><a href=\"javascript:alert(1)\"><rect width='10' height='10'/></a></svg>";

        String out = sanitizer.sanitize(input);

        assertThat(out.toLowerCase()).doesNotContain("javascript:");
    }

    @Test
    void stripsForeignObjectEmbed() {
        String input = "<svg><foreignObject><iframe src='http://attacker'/></foreignObject>"
                + "<path d='M0 0'/></svg>";

        String out = sanitizer.sanitize(input);

        assertThat(out.toLowerCase()).doesNotContain("foreignobject").doesNotContain("iframe");
        assertThat(out).contains("<path");
    }

    @Test
    void stripsIframeEmbedObjectTags() {
        String input = "<svg><iframe src='x'/><embed src='y'/><object data='z'/><rect/></svg>";

        String out = sanitizer.sanitize(input);

        assertThat(out.toLowerCase())
                .doesNotContain("<iframe")
                .doesNotContain("<embed")
                .doesNotContain("<object");
    }

    @Test
    void stripsXlinkHrefPointingOffOrigin() {
        String input = "<svg xmlns:xlink='http://www.w3.org/1999/xlink'>"
                + "<use xlink:href='http://evil.example/badge.svg#x'/></svg>";

        String out = sanitizer.sanitize(input);

        assertThat(out.toLowerCase()).doesNotContain("evil.example");
    }

    @Test
    void keepsSameDocumentFragmentHref() {
        String input = "<svg>"
                + "<defs><linearGradient id='g1'><stop offset='0' stop-color='#000'/></linearGradient></defs>"
                + "<use xlink:href='#g1'/></svg>";

        String out = sanitizer.sanitize(input);

        // Reference by fragment is allowed; gradient/stop preserved
        assertThat(out.toLowerCase()).contains("lineargradient").contains("stop");
    }

    @Test
    void stripsCssExpressionInStyle() {
        String input = "<svg><rect style=\"fill:red;background:expression(alert(1))\" width='10' height='10'/></svg>";

        String out = sanitizer.sanitize(input);

        assertThat(out.toLowerCase()).doesNotContain("expression(");
    }

    @Test
    void preservesSafeBrandingSvg() {
        String input = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>"
                + "<g fill='#2563eb'>"
                + "<circle cx='50' cy='50' r='40' stroke='#000' stroke-width='2'/>"
                + "<text x='10' y='90' opacity='0.8'>KiteClass</text>"
                + "</g></svg>";

        String out = sanitizer.sanitize(input);

        assertThat(out.toLowerCase())
                .contains("<svg")
                .contains("<circle")
                .contains("<text")
                .contains("kiteclass");
    }

    @Test
    void stripsMixedCaseScriptVariants() {
        String input = "<svg><ScRiPt>alert(1)</ScRiPt><path d='M1 1'/></svg>";

        String out = sanitizer.sanitize(input);

        assertThat(out.toLowerCase()).doesNotContain("<script");
    }
}
