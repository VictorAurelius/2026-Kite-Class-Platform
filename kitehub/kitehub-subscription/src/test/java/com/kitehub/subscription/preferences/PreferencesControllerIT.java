package com.kitehub.subscription.preferences;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.config.SecurityConfig;
import com.kitehub.subscription.preferences.controller.PreferencesController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-layer integration test for {@link PreferencesController}.
 *
 * <p>Closes GAP-663 (Wave beta-readiness-2 Bucket D) — Wave 98 Bucket B0 shipped the controller
 * with zero IT coverage. This test verifies actual {@code @RestController} routing + Spring Security
 * config + cookie attribute serialization that unit-level Mockito tests cannot exercise.</p>
 *
 * <p><strong>Cookie httpOnly=false rationale (GAP-663 §Problem #2):</strong>
 * The controller deliberately sets {@code httpOnly(false)} so FE's {@code useOnboardingPhase} hook
 * can read the marker via {@code document.cookie} for cross-tab UI dismissal sync without a
 * round-trip GET. Defense-in-depth: cookie value is opaque "0"/"1" timestamp marker (no sensitive
 * payload) + SameSite=Lax + secure=true. Documented in api-contract.md cookie semantic field.</p>
 *
 * @since Wave beta-readiness-2 Bucket D — GAP-663
 */
@WebMvcTest(controllers = PreferencesController.class)
@Import(SecurityConfig.class)
@DisplayName("PreferencesController IT — GAP-663 HTTP routing + cookie + validation")
class PreferencesControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String ENDPOINT = "/api/v1/preferences/dismiss-banner-state";
    private static final String COOKIE_NAME_PREFIX = "kite-banner-dismissed-";

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("POST dismiss-banner-state → 204 + cookie set với httpOnly=false (GAP-663 cookie semantic)")
    void dismissBannerState_returns204AndSetsCookieWithHttpOnlyFalse() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("bannerKey", "wave-103-beta", "dismissed", true)
        );

        MvcResult result = mockMvc.perform(post(ENDPOINT)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists(COOKIE_NAME_PREFIX + "wave-103-beta"))
                .andExpect(cookie().value(COOKIE_NAME_PREFIX + "wave-103-beta", "1"))
                .andExpect(cookie().httpOnly(COOKIE_NAME_PREFIX + "wave-103-beta", false))
                .andExpect(cookie().secure(COOKIE_NAME_PREFIX + "wave-103-beta", true))
                .andReturn();

        // Verify SameSite=Lax via raw header (MockMvc cookie() matcher doesn't expose SameSite)
        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookieHeader)
                .as("Cookie header must include SameSite=Lax for CSRF protection per GAP-656 Step 5")
                .contains("SameSite=Lax");
        assertThat(setCookieHeader)
                .as("Cookie Max-Age must be 30 days (2592000 seconds) per COOKIE_MAX_AGE constant")
                .contains("Max-Age=2592000");
    }

    @Test
    @WithAnonymousUser
    @DisplayName("POST dismiss-banner-state without JWT → 401 (SecurityConfig default-deny per OWASP A05)")
    void dismissBannerState_withoutJwt_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("bannerKey", "test-banner", "dismissed", true)
        );
        mockMvc.perform(post(ENDPOINT)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("POST dismiss-banner-state với dismissed=false → cookie value '0' (un-dismiss path)")
    void dismissBannerState_withDismissedFalse_setsCookieValueZero() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("bannerKey", "test-banner", "dismissed", false)
        );

        mockMvc.perform(post(ENDPOINT)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent())
                .andExpect(cookie().value(COOKIE_NAME_PREFIX + "test-banner", "0"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    @DisplayName("POST dismiss-banner-state với bannerKey vi phạm @Pattern → 400 validation error")
    void dismissBannerState_withInvalidBannerKey_returns400() throws Exception {
        // bannerKey "Wave_103_BETA" vi phạm @Pattern("^[a-z0-9-]+$") — uppercase + underscore banned
        String body = objectMapper.writeValueAsString(
                Map.of("bannerKey", "Wave_103_BETA", "dismissed", true)
        );

        mockMvc.perform(post(ENDPOINT)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
