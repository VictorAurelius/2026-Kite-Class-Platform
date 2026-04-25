package com.kiteclass.core.module.document.branding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HexColorUtilTest {

    @Test
    void stripHash_valid_with_hash() {
        assertThat(HexColorUtil.stripHash("#2563EB")).isEqualTo("2563EB");
    }

    @Test
    void stripHash_valid_without_hash() {
        assertThat(HexColorUtil.stripHash("2563EB")).isEqualTo("2563EB");
    }

    @Test
    void stripHash_lowercase_normalized_to_upper() {
        assertThat(HexColorUtil.stripHash("#a1b2c3")).isEqualTo("A1B2C3");
    }

    @Test
    void stripHash_null_returns_null() {
        assertThat(HexColorUtil.stripHash(null)).isNull();
    }

    @Test
    void stripHash_blank_returns_null() {
        assertThat(HexColorUtil.stripHash("   ")).isNull();
    }

    @Test
    void stripHash_wrong_length_returns_null() {
        assertThat(HexColorUtil.stripHash("#ABC")).isNull();
        assertThat(HexColorUtil.stripHash("#ABCDEFGH")).isNull();
    }

    @Test
    void stripHash_invalid_chars_returns_null() {
        assertThat(HexColorUtil.stripHash("#GGHHII")).isNull();
        assertThat(HexColorUtil.stripHash("#2563EZ")).isNull();
    }

    @Test
    void toRgbBytes_returns_three_bytes_in_order() {
        byte[] bytes = HexColorUtil.toRgbBytes("#2563EB");
        assertThat(bytes).isNotNull();
        assertThat(bytes).hasSize(3);
        assertThat(bytes[0] & 0xFF).isEqualTo(0x25);
        assertThat(bytes[1] & 0xFF).isEqualTo(0x63);
        assertThat(bytes[2] & 0xFF).isEqualTo(0xEB);
    }

    @Test
    void toRgbBytes_null_returns_null() {
        assertThat(HexColorUtil.toRgbBytes(null)).isNull();
        assertThat(HexColorUtil.toRgbBytes("")).isNull();
        assertThat(HexColorUtil.toRgbBytes("not-a-color")).isNull();
    }
}
