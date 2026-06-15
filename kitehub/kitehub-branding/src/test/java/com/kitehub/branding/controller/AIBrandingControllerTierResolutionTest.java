package com.kitehub.branding.controller;

import com.kitehub.branding.dto.LogoAnalysis;
import com.kitehub.branding.dto.ThemeConfig;
import com.kitehub.branding.service.AIBrandingService;
import com.kitehub.branding.service.AIInputCapService;
import com.kitehub.branding.service.AIRateLimitService;
import com.kitehub.branding.service.ThemeGenerationService;
import com.kitehub.branding.tenant.SubscriptionTierResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GAP-1020 (Part 2) — proves AIBrandingController gates entitlement on the SERVER-RESOLVED tier,
 * not the client-supplied {@code X-Subscription-Tier} header. A spoofed {@code ENTERPRISE} header
 * must NOT raise the tier the rate-limiter sees when the instance's authoritative tier is FREE.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AIBrandingController — tier from subscription, header not trusted")
class AIBrandingControllerTierResolutionTest {

    @Mock private AIBrandingService aiBrandingService;
    @Mock private ThemeGenerationService themeGenerationService;
    @Mock private AIRateLimitService aiRateLimitService;
    @Mock private AIInputCapService aiInputCapService;
    @Mock private SubscriptionTierResolver tierResolver;

    @Test
    @DisplayName("Spoofed ENTERPRISE header → FREE limits applied (resolver wins)")
    void clientEnterpriseHeaderDoesNotRaiseQuota() {
        AIBrandingController controller = new AIBrandingController(
                aiBrandingService, themeGenerationService, aiRateLimitService,
                aiInputCapService, tierResolver);

        UUID instanceId = UUID.randomUUID();
        String instanceHeader = instanceId.toString();

        // Authoritative tier for this instance is FREE — even though the client sent ENTERPRISE.
        when(tierResolver.resolveEffectiveTier(eq(instanceId), eq("ENTERPRISE"))).thenReturn("FREE");
        lenient().when(aiRateLimitService.isRateLimited(eq(instanceId), anyString())).thenReturn(false);
        lenient().when(aiInputCapService.checkInputSize(anyString(), any(String[].class))).thenReturn(null);
        lenient().when(themeGenerationService.generateThemeConfig(any())).thenReturn(mock(ThemeConfig.class));

        LogoAnalysis analysis = LogoAnalysis.builder()
                .primaryColor("#2563EB").secondaryColor("#1E40AF").accentColor("#F59E0B")
                .theme("MODERN").typography("Sans").targetAudience("students").build();

        // tenantHeader == instanceHeader so the ownership guard passes for a non-admin caller.
        controller.generateTheme(analysis, instanceHeader, instanceHeader, "ENTERPRISE");

        // Rate-limit + input-cap MUST see the server-resolved FREE tier, never the spoofed header.
        verify(aiRateLimitService).isRateLimited(eq(instanceId), eq("FREE"));
        verify(aiRateLimitService, never()).isRateLimited(eq(instanceId), eq("ENTERPRISE"));
        verify(aiInputCapService).checkInputSize(eq("FREE"), any(String[].class));
    }
}
