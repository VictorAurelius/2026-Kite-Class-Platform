package com.kiteclass.core.module.marketing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.marketing.config.LandingPageSafetyProperties;
import com.kiteclass.core.module.marketing.entity.LandingPage;
import com.kiteclass.core.module.marketing.service.impl.LandingPageContentSanitizerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for {@link LandingPageContentSanitizer} (GAP-827 landing input safety).
 *
 * <p>No Mockito: pure logic. Builds the impl with a fixed-allowlist config so tests are
 * deterministic + fast (no Spring context).
 *
 * @since wave-thesis-5 (GAP-827)
 */
class LandingPageContentSanitizerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LandingPageContentSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        LandingPageSafetyProperties props = new LandingPageSafetyProperties();
        props.setAllowedImageHosts(List.of(
                "cdn.kitehub.me",
                "assets.kitehub.me",
                "localhost",
                "minio"));
        sanitizer = new LandingPageContentSanitizerImpl(MAPPER, props);
    }

    // ---- sanitizeText: XSS strip + VN diacritic preservation -------------------------

    @Nested
    class SanitizeText {

        @Test
        void stripsScriptTag() {
            String out = sanitizer.sanitizeText("Hello<script>alert('xss')</script>World");
            assertThat(out).doesNotContain("<script>").doesNotContain("alert");
            assertThat(out).contains("Hello").contains("World");
        }

        @Test
        void stripsImgOnerrorVector() {
            String out = sanitizer.sanitizeText("<img src=x onerror=alert(1)>Trung tâm");
            assertThat(out).doesNotContainIgnoringCase("onerror").doesNotContain("<img");
            assertThat(out).contains("Trung tâm");
        }

        @Test
        void stripsAnchorJavascriptScheme() {
            String out = sanitizer.sanitizeText("<a href=\"javascript:alert(1)\">click</a>");
            assertThat(out).doesNotContainIgnoringCase("javascript:").doesNotContain("<a");
            assertThat(out).contains("click");
        }

        @Test
        void preservesVietnameseDiacriticsRaw() {
            // Wave 106 GAP-764 — must NOT corrupt â/ê/ô into &acirc; etc.
            String vn = "Trần Thị Hồng — Trung tâm Anh ngữ Sky Education";
            String out = sanitizer.sanitizeText(vn);
            assertThat(out).isEqualTo(vn);
            assertThat(out).doesNotContain("&"); // no HTML entity corruption
        }

        @Test
        void preservesAllSevenFrequentVnDiacritics() {
            String vn = "â ê ô ữ ồ ằ ấ";
            assertThat(sanitizer.sanitizeText(vn)).isEqualTo(vn);
        }

        @Test
        void nfcNormalizesCombiningForm() {
            // NFD (decomposed base + U+0302 combining circumflex) → NFC precomposed
            String base = "Tr" + "â" + "n";  // decomposed â
            String nfdForm = java.text.Normalizer.normalize(base, java.text.Normalizer.Form.NFD);
            String nfcForm = java.text.Normalizer.normalize(base, java.text.Normalizer.Form.NFC);
            assertThat(nfdForm).isNotEqualTo(nfcForm); // sanity: genuinely decomposed
            assertThat(sanitizer.sanitizeText(nfdForm)).isEqualTo(nfcForm);
        }

        @Test
        void returnsNullForNull() {
            assertThat(sanitizer.sanitizeText(null)).isNull();
        }

        @Test
        void trimsWhitespace() {
            assertThat(sanitizer.sanitizeText("  hello  ")).isEqualTo("hello");
        }

        @Test
        void allMarkupBecomesEmpty() {
            assertThat(sanitizer.sanitizeText("<script></script>")).isEmpty();
        }
    }

    // ---- sanitizeJson: recursive string-value sanitize -------------------------------

    @Nested
    class SanitizeJson {

        @Test
        void sanitizesObjectStringValuesKeepsKeys() {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("name", "Cô Hồng<script>alert(1)</script>");
            node.put("subject", "Toán");

            JsonNode out = sanitizer.sanitizeJson(node);

            assertThat(out.get("name").asText()).doesNotContain("<script>").contains("Cô Hồng");
            assertThat(out.get("subject").asText()).isEqualTo("Toán");
            assertThat(out.has("name")).isTrue(); // key preserved
        }

        @Test
        void sanitizesNestedArrayElements() {
            ArrayNode arr = MAPPER.createArrayNode();
            ObjectNode t = MAPPER.createObjectNode();
            t.put("author", "Phụ huynh<img src=x onerror=alert(1)>");
            ArrayNode credentials = MAPPER.createArrayNode();
            credentials.add("IELTS 8.0<script>evil()</script>");
            t.set("credentials", credentials);
            arr.add(t);

            JsonNode out = sanitizer.sanitizeJson(arr);

            assertThat(out.get(0).get("author").asText())
                    .doesNotContainIgnoringCase("onerror").contains("Phụ huynh");
            assertThat(out.get(0).get("credentials").get(0).asText())
                    .doesNotContain("<script>").contains("IELTS 8.0");
        }

        @Test
        void leavesNumberAndBooleanUnchanged() {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("rating", 5);
            node.put("highlighted", true);

            JsonNode out = sanitizer.sanitizeJson(node);

            assertThat(out.get("rating").asInt()).isEqualTo(5);
            assertThat(out.get("highlighted").asBoolean()).isTrue();
        }

        @Test
        void preservesVnDiacriticsInJson() {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("content", "Học phí hợp lý, giáo viên tận tâm");
            JsonNode out = sanitizer.sanitizeJson(node);
            assertThat(out.get("content").asText()).isEqualTo("Học phí hợp lý, giáo viên tận tâm");
        }

        @Test
        void returnsNullForNull() {
            assertThat(sanitizer.sanitizeJson(null)).isNull();
        }
    }

    // ---- validateImageUrl: scheme + host allowlist -----------------------------------

    @Nested
    class ValidateImageUrl {

        @Test
        void acceptsAllowedHttpsHost() {
            String url = "https://cdn.kitehub.me/banners/hero.png";
            assertThat(sanitizer.validateImageUrl(url)).isEqualTo(url);
        }

        @Test
        void rejectsJavascriptScheme() {
            assertThatThrownBy(() -> sanitizer.validateImageUrl("javascript:alert(1)"))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        void rejectsDataScheme() {
            assertThatThrownBy(() -> sanitizer.validateImageUrl(
                    "data:text/html;base64,PHNjcmlwdD4="))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        void rejectsNonHttpsHttp() {
            assertThatThrownBy(() -> sanitizer.validateImageUrl("http://cdn.kitehub.me/x.png"))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        void rejectsOffAllowlistHost() {
            // off-origin .svg can carry script
            assertThatThrownBy(() -> sanitizer.validateImageUrl("https://evil.example.com/x.svg"))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        void nullPassesThrough() {
            assertThatCode(() -> sanitizer.validateImageUrl(null)).doesNotThrowAnyException();
            assertThat(sanitizer.validateImageUrl(null)).isNull();
        }

        @Test
        void blankPassesThrough() {
            assertThat(sanitizer.validateImageUrl("   ")).isEqualTo("   ");
        }
    }

    // ---- sanitize(entity): end-to-end on entity --------------------------------------

    @Nested
    class SanitizeEntity {

        @Test
        void stripsXssFromAllTextFieldsAndJsonb() {
            LandingPage e = new LandingPage();
            e.setHeroTitle("Trung tâm<script>alert(1)</script>");
            e.setHeroSubtitle("Phương pháp<img src=x onerror=alert(2)>");
            e.setTeacherBio("<b>Cô Hồng</b> 10 năm kinh nghiệm<script>x()</script>");
            e.setAboutText("Giới thiệu<iframe src=evil></iframe>");
            ObjectNode teachers = MAPPER.createObjectNode();
            teachers.put("name", "Thầy Nhì<script>evil()</script>");
            e.setTeachers(teachers);

            sanitizer.sanitize(e);

            assertThat(e.getHeroTitle()).doesNotContain("<script>").contains("Trung tâm");
            assertThat(e.getHeroSubtitle()).doesNotContainIgnoringCase("onerror").contains("Phương pháp");
            assertThat(e.getTeacherBio()).doesNotContain("<script>").doesNotContain("<b>").contains("Cô Hồng");
            assertThat(e.getAboutText()).doesNotContain("<iframe").contains("Giới thiệu");
            assertThat(e.getTeachers().get("name").asText())
                    .doesNotContain("<script>").contains("Thầy Nhì");
        }

        @Test
        void rejectsMaliciousHeroImageUrl() {
            LandingPage e = new LandingPage();
            e.setHeroImageUrl("https://evil.example.com/x.svg");
            assertThatThrownBy(() -> sanitizer.sanitize(e))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        void preservesValidImageUrl() {
            LandingPage e = new LandingPage();
            e.setHeroImageUrl("https://assets.kitehub.me/hero.png");
            sanitizer.sanitize(e);
            assertThat(e.getHeroImageUrl()).isEqualTo("https://assets.kitehub.me/hero.png");
        }

        @Test
        void handlesNullFieldsGracefully() {
            LandingPage e = new LandingPage();
            assertThatCode(() -> sanitizer.sanitize(e)).doesNotThrowAnyException();
        }
    }
}
