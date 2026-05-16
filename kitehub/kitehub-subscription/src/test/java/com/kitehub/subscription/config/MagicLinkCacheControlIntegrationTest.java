package com.kitehub.subscription.config;

import com.kitehub.subscription.beta.controller.BetaAccessController;
import com.kitehub.subscription.beta.dto.BetaTokenValidationResponse;
import com.kitehub.subscription.beta.service.BetaAccessService;
import com.kitehub.subscription.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test verifying {@link MagicLinkCacheControlInterceptor} is wired
 * into Spring MVC for beta-signup invite endpoints, closing GAP-584 AC#2
 * (Wave 86) — origin defense-in-depth.
 *
 * <p>Uses {@code @WebMvcTest} + {@code @Import({SecurityConfig.class, WebMvcConfig.class,
 * MagicLinkCacheControlInterceptor.class, AdminApiKeyInterceptor.class})} so the
 * interceptor registry built by {@link WebMvcConfig#addInterceptors} engages and
 * the interceptor actually fires on the validate endpoint.</p>
 */
@WebMvcTest(controllers = BetaAccessController.class)
@Import({SecurityConfig.class, WebMvcConfig.class,
        MagicLinkCacheControlInterceptor.class, AdminApiKeyInterceptor.class})
@DisplayName("MagicLink Cache-Control wiring — Wave 86 GAP-584 AC#2")
class MagicLinkCacheControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BetaAccessService service;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setUp() {
        Mockito.reset(service, authService);
    }

    @Test
    @WithAnonymousUser
    @DisplayName("GET /api/v1/auth/beta-signup/validate returns Cache-Control: no-store header")
    void betaSignupValidateReturnsCacheControlNoStore() throws Exception {
        UUID token = UUID.randomUUID();
        BetaTokenValidationResponse resp = BetaTokenValidationResponse.ok(
                "test@example.com", "Test User", "Test Org", "P2_CENTER_OWNER");
        when(service.validateToken(any(UUID.class))).thenReturn(resp);

        mockMvc.perform(get("/api/v1/auth/beta-signup/validate")
                        .param("token", token.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control",
                        "no-store, no-cache, max-age=0, must-revalidate"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"));
    }
}
