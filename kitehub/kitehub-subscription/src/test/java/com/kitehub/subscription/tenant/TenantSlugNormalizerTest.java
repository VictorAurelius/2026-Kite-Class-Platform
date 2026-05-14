package com.kitehub.subscription.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TenantSlugNormalizer} (GAP-535 Wave 77 Bucket D).
 *
 * <p>Covers the Vietnamese diacritic + smart-quote + collision-recovery
 * pipeline outlined in the gap acceptance criteria.</p>
 */
@DisplayName("TenantSlugNormalizer")
class TenantSlugNormalizerTest {

    private final TenantSlugNormalizer normalizer = new TenantSlugNormalizer();

    @ParameterizedTest(name = "[{index}] \"{0}\" -> \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            // Wave 77 outside-in AC primary example
            "Trường Mầm Non \"Hoa Mai\" | truong-mam-non-hoa-mai",
            // Smart-quote-only (U+201C / U+201D)
            "“Hoa Mai” | hoa-mai",
            // Apostrophe variants (U+2018 / U+2019)
            "Anh ‘Tuấn’ Nguyễn | anh-tuan-nguyen",
            // Pure VN diacritics
            "Trường Mầm Non Hoa Mai | truong-mam-non-hoa-mai",
            // Edge — only diacritics + spaces
            "à á ả ã ạ | a-a-a-a-a",
            // Edge — only diacritics no spaces
            "àáảãạ | aaaaa",
            // Mixed case + ampersand
            "ABC Học Đường & Cộng Sự | abc-hoc-duong-cong-su",
            // Leading/trailing whitespace + punctuation
            "  --Hệ Thống KiteClass!--  | he-thong-kiteclass",
            // Numeric retained
            "Lớp 12A1 | lop-12a1",
            // Stroke-bar đ/Đ
            "ĐẠI HỌC ĐÀ NẴNG | dai-hoc-da-nang",
    })
    @DisplayName("normalize — Vietnamese + smart-quote patterns")
    void normalizeMatrix(String input, String expected) {
        assertThat(normalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("normalize — null input → empty string")
    void normalizeNullSafe() {
        assertThat(normalizer.normalize(null)).isEmpty();
    }

    @Test
    @DisplayName("normalize — only-non-alphanumeric → empty string (caller must reject)")
    void normalizeOnlyPunctuation() {
        assertThat(normalizer.normalize("--__??!!")).isEmpty();
        assertThat(normalizer.normalize("\"\"")).isEmpty();
    }

    @Test
    @DisplayName("withCollisionSuffix — basic append")
    void collisionSuffixBasic() {
        assertThat(normalizer.withCollisionSuffix("hoa-mai", 1)).isEqualTo("hoa-mai-1");
        assertThat(normalizer.withCollisionSuffix("hoa-mai", 9)).isEqualTo("hoa-mai-9");
        assertThat(normalizer.withCollisionSuffix("hoa-mai", 10)).isEqualTo("hoa-mai-10");
    }

    @Test
    @DisplayName("withCollisionSuffix — zero/negative rejected")
    void collisionSuffixRejectsZero() {
        assertThatThrownBy(() -> normalizer.withCollisionSuffix("hoa-mai", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> normalizer.withCollisionSuffix("hoa-mai", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("withCollisionSuffix — long base trimmed to fit cap")
    void collisionSuffixTrimsBase() {
        String longBase = "a".repeat(TenantSlugNormalizer.MAX_SLUG_LENGTH);
        String result = normalizer.withCollisionSuffix(longBase, 1);
        assertThat(result).hasSizeLessThanOrEqualTo(TenantSlugNormalizer.MAX_SLUG_LENGTH);
        assertThat(result).endsWith("-1");
    }

    @Test
    @DisplayName("normalize — truncates to MAX_SLUG_LENGTH and trims trailing dash")
    void normalizeRespectsMaxLength() {
        // Input that, when slugified, exceeds the cap.
        String input = "Trường ".repeat(40); // 7 chars/word * 40 = 280
        String result = normalizer.normalize(input);
        assertThat(result).hasSizeLessThanOrEqualTo(TenantSlugNormalizer.MAX_SLUG_LENGTH);
        assertThat(result).doesNotEndWith("-");
    }
}
