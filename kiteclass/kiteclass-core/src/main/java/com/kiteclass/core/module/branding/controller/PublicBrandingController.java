package com.kiteclass.core.module.branding.controller;

import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import com.kiteclass.core.module.settings.entity.Branding;
import com.kiteclass.core.module.settings.repository.BrandingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Minimal branding lookup for <strong>unauthenticated</strong> contexts — i.e.
 * the login / register / reset-password pages of kiteclass-frontend which need
 * to paint the tenant's logo + primary color before the user signs in.
 *
 * <p>Returns only what the auth layer needs so we don't leak admin-facing
 * configuration (contact info, social media) to anonymous callers.
 *
 * @since Wave 4 (GAP-037)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/branding/public")
@RequiredArgsConstructor
@Tag(name = "PublicBranding", description = "Public branding lookup for auth pages")
public class PublicBrandingController {

    private final BrandingRepository brandingRepository;
    private final FrontendInstanceRepository frontendInstanceRepository;

    @GetMapping
    @Operation(summary = "Get minimal branding (logo + name + primary color) for a tenant")
    public ResponseEntity<Map<String, Object>> get(@RequestParam("tenantId") String tenantIdSlug) {
        UUID tenantUuid = resolveTenantUuid(tenantIdSlug);
        if (tenantUuid == null) {
            return ResponseEntity.ok(defaults());
        }

        return brandingRepository.findByInstanceIdAndDeletedFalse(tenantUuid)
                .map(this::toPublicPayload)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(defaults()));
    }

    private UUID resolveTenantUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            // Happy path — callers who already know the UUID.
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            // Otherwise look up by slug via FrontendInstance mapping.
            Optional<FrontendInstance> slugMatch =
                    frontendInstanceRepository.findBySlugAndDeletedFalse(raw);
            if (slugMatch.isPresent() && slugMatch.get().getTenantSlug() != null) {
                try {
                    return UUID.fromString(slugMatch.get().getTenantSlug());
                } catch (IllegalArgumentException ex) {
                    log.debug("FrontendInstance {} has non-UUID tenantSlug: {}",
                            raw, slugMatch.get().getTenantSlug());
                }
            }
            return null;
        }
    }

    private Map<String, Object> toPublicPayload(Branding b) {
        return Map.of(
                "displayName", orDefault(b.getDisplayName(), "KiteClass"),
                "logoUrl", b.getLogoUrl() == null ? "" : b.getLogoUrl(),
                "primaryColor", orDefault(b.getPrimaryColor(), "#3B82F6"),
                "secondaryColor", orDefault(b.getSecondaryColor(), "#8B5CF6"),
                "accentColor", orDefault(b.getAccentColor(), "#10B981"),
                "tagline", b.getTagline() == null ? "" : b.getTagline()
        );
    }

    private Map<String, Object> defaults() {
        return Map.of(
                "displayName", "KiteClass",
                "logoUrl", "",
                "primaryColor", "#3B82F6",
                "secondaryColor", "#8B5CF6",
                "accentColor", "#10B981",
                "tagline", ""
        );
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
