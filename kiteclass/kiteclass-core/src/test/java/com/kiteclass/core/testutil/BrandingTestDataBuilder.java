package com.kiteclass.core.testutil;

import com.kiteclass.core.module.settings.entity.Branding;

import java.util.UUID;

/**
 * Test data builder for Branding entity.
 *
 * @since 2.9
 */
public final class BrandingTestDataBuilder {

    private BrandingTestDataBuilder() {
        // Utility class
    }

    /**
     * Create default branding for testing.
     *
     * @param instanceId tenant instance ID
     * @return branding entity
     */
    public static Branding createDefaultBranding(UUID instanceId) {
        Branding branding = new Branding();
        branding.setInstanceId(instanceId);
        branding.setDisplayName("Test Center");
        branding.setTagline("Test Education Platform");
        branding.setPrimaryColor("#3B82F6");
        branding.setSecondaryColor("#8B5CF6");
        branding.setAccentColor("#10B981");
        branding.setContactEmail("contact@test.com");
        branding.setContactPhone("0901234567");
        branding.setAddress("123 Test Street");
        branding.setFacebookUrl("https://facebook.com/test");
        branding.setZaloUrl("https://zalo.me/test");
        branding.setWebsiteUrl("https://test.com");
        return branding;
    }

    /**
     * Create branding with custom display name.
     *
     * @param instanceId  tenant instance ID
     * @param displayName custom display name
     * @return branding entity
     */
    public static Branding createBranding(UUID instanceId, String displayName) {
        Branding branding = createDefaultBranding(instanceId);
        branding.setDisplayName(displayName);
        return branding;
    }

    /**
     * Create branding with custom colors.
     *
     * @param instanceId     tenant instance ID
     * @param primaryColor   primary color hex
     * @param secondaryColor secondary color hex
     * @param accentColor    accent color hex
     * @return branding entity
     */
    public static Branding createBrandingWithColors(
            UUID instanceId,
            String primaryColor,
            String secondaryColor,
            String accentColor) {
        Branding branding = createDefaultBranding(instanceId);
        branding.setPrimaryColor(primaryColor);
        branding.setSecondaryColor(secondaryColor);
        branding.setAccentColor(accentColor);
        return branding;
    }
}
