package com.kitehub.branding.wizard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.dto.BrandingAsset;
import com.kitehub.branding.wizard.dto.BrandColours;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the GAP-1107 #2 fix: {@code MockProvisioningService} must
 * persist {@code assetsGenerated} as a {@code BrandingAsset[]} JSON array so the
 * {@code AssetStorageController} parser (which uses
 * {@code TypeReference<List<BrandingAsset>>}) reads ≥1 asset instead of throwing
 * {@code MismatchedInputException} on a metadata OBJECT ("0 assets" regression).
 */
@DisplayName("MockProvisioningService mock-asset build")
class MockProvisioningServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static BrandColours sampleColours() {
        return new BrandColours("#2563eb", "#06b6d4", "#f59e0b",
            "#64748b", "#ffffff", BrandColours.Source.TEMPLATE);
    }

    @Test
    @DisplayName("Approved resources serialize as a JSON array that the controller parser can read")
    void buildsAssetsAsParseableArray() throws Exception {
        List<BrandingAsset> assets = MockProvisioningService.buildDeployedAssets(
            "toan-master", "sky-wave",
            List.of("logo", "colors", "banner", "hero"), sampleColours());

        // Persisted shape MUST be a JSON array (starts with '[').
        String json = objectMapper.writeValueAsString(assets);
        assertThat(json.trim()).startsWith("[");

        // Round-trip through the EXACT parser AssetStorageController.parseAssetsJson uses.
        List<BrandingAsset> parsed = objectMapper.readValue(
            json, new TypeReference<List<BrandingAsset>>() {});

        assertThat(parsed).hasSize(4);
        assertThat(parsed).extracting(BrandingAsset::getType)
            .containsExactly("LOGO", "COLORS", "BANNER", "HERO");
        assertThat(parsed).allSatisfy(a -> {
            assertThat(a.getUrl()).contains("/instances/toan-master/");
            assertThat(a.getUploadedAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("Empty/blank approved list falls back to the default resource set (never 0 assets)")
    void fallsBackToDefaultResourcesWhenEmpty() {
        List<BrandingAsset> assets = MockProvisioningService.buildDeployedAssets(
            null, null, List.of(), sampleColours());

        assertThat(assets).hasSize(MockProvisioningService.DEFAULT_RESOURCES.size());
        assertThat(assets).extracting(BrandingAsset::getType)
            .containsExactly("LOGO", "COLORS", "BANNER", "HERO");
        // null slug → "tenant" placeholder URL segment.
        assertThat(assets.get(0).getUrl()).contains("/instances/tenant/");
    }

    @Test
    @DisplayName("COLORS asset carries the brand primary colour + JSON content-type")
    void coloursAssetEncodesPrimary() {
        List<BrandingAsset> assets = MockProvisioningService.buildDeployedAssets(
            "sky-edu", "warm-glow", List.of("colors"), sampleColours());

        assertThat(assets).hasSize(1);
        BrandingAsset colours = assets.get(0);
        assertThat(colours.getType()).isEqualTo("COLORS");
        assertThat(colours.getVariant()).isEqualTo("#2563eb");
        assertThat(colours.getContentType()).isEqualTo("application/json");
    }
}
