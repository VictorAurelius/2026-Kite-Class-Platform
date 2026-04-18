package com.kiteclass.core.module.settings.versioning;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kiteclass.core.module.settings.entity.Branding;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Serializable snapshot of the {@link Branding} fields we persist into version
 * history. Stored as JSONB in {@code branding_versions.snapshot_json}.
 *
 * <p>This intentionally mirrors the writable fields only (no audit metadata)
 * so rollback doesn't resurrect stale {@code createdAt} / {@code updatedAt}
 * timestamps.
 *
 * @since Wave 4 (GAP-033p)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrandingSnapshot {

    private String logoUrl;
    private String faviconUrl;
    private String displayName;
    private String tagline;
    private String primaryColor;
    private String secondaryColor;
    private String accentColor;
    private String themeConfigJson;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String facebookUrl;
    private String zaloUrl;
    private String websiteUrl;

    public static BrandingSnapshot from(Branding b) {
        return BrandingSnapshot.builder()
                .logoUrl(b.getLogoUrl())
                .faviconUrl(b.getFaviconUrl())
                .displayName(b.getDisplayName())
                .tagline(b.getTagline())
                .primaryColor(b.getPrimaryColor())
                .secondaryColor(b.getSecondaryColor())
                .accentColor(b.getAccentColor())
                .themeConfigJson(b.getThemeConfigJson())
                .contactEmail(b.getContactEmail())
                .contactPhone(b.getContactPhone())
                .address(b.getAddress())
                .facebookUrl(b.getFacebookUrl())
                .zaloUrl(b.getZaloUrl())
                .websiteUrl(b.getWebsiteUrl())
                .build();
    }

    public void applyTo(Branding b) {
        b.setLogoUrl(logoUrl);
        b.setFaviconUrl(faviconUrl);
        if (displayName != null && !displayName.isBlank()) {
            b.setDisplayName(displayName);
        }
        b.setTagline(tagline);
        if (primaryColor != null && !primaryColor.isBlank()) {
            b.setPrimaryColor(primaryColor);
        }
        if (secondaryColor != null && !secondaryColor.isBlank()) {
            b.setSecondaryColor(secondaryColor);
        }
        if (accentColor != null && !accentColor.isBlank()) {
            b.setAccentColor(accentColor);
        }
        b.setThemeConfigJson(themeConfigJson);
        b.setContactEmail(contactEmail);
        b.setContactPhone(contactPhone);
        b.setAddress(address);
        b.setFacebookUrl(facebookUrl);
        b.setZaloUrl(zaloUrl);
        b.setWebsiteUrl(websiteUrl);
    }
}
