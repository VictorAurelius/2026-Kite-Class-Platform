package com.kitehub.branding.wizard.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BrandColours value object")
class BrandColoursTest {

    @Test
    @DisplayName("accepts valid hex strings")
    void acceptsValidHex() {
        BrandColours colours = new BrandColours(
                "#1a73e8", "#fbbc04", "#10B981", "#1f2937", "#ffffff",
                BrandColours.Source.TEMPLATE);
        assertThat(colours.primary()).isEqualTo("#1a73e8");
        assertThat(colours.source()).isEqualTo(BrandColours.Source.TEMPLATE);
    }

    @Test
    @DisplayName("rejects non-hex primary colour")
    void rejectsBadPrimary() {
        assertThatThrownBy(() -> new BrandColours(
                "rgb(0,0,0)", "#fbbc04", "#10B981", "#1f2937", "#ffffff",
                BrandColours.Source.TEMPLATE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("primary");
    }

    @Test
    @DisplayName("rejects 3-digit hex shorthand")
    void rejectsShortHex() {
        assertThatThrownBy(() -> new BrandColours(
                "#fff", "#fbbc04", "#10B981", "#1f2937", "#ffffff",
                BrandColours.Source.TEMPLATE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("defaults source to TEMPLATE when null")
    void defaultsSource() {
        BrandColours colours = new BrandColours(
                "#1a73e8", "#fbbc04", "#10B981", "#1f2937", "#ffffff", null);
        assertThat(colours.source()).isEqualTo(BrandColours.Source.TEMPLATE);
    }

    @Test
    @DisplayName("serializes to JSON with all 6 fields")
    void serializesToJson() throws Exception {
        BrandColours colours = new BrandColours(
                "#1a73e8", "#fbbc04", "#10B981", "#1f2937", "#ffffff",
                BrandColours.Source.AI_ANALYSIS);
        String json = new ObjectMapper().writeValueAsString(colours);
        assertThat(json)
                .contains("\"primary\":\"#1a73e8\"")
                .contains("\"secondary\":\"#fbbc04\"")
                .contains("\"accent\":\"#10B981\"")
                .contains("\"neutral\":\"#1f2937\"")
                .contains("\"background\":\"#ffffff\"")
                .contains("\"source\":\"AI_ANALYSIS\"");
    }
}
