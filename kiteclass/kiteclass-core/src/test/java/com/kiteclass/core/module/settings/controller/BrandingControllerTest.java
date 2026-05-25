package com.kiteclass.core.module.settings.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.module.settings.dto.request.UpdateBrandingRequest;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import com.kiteclass.core.module.settings.service.BrandingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for BrandingController.
 *
 * <p>Uses @TestConfiguration to provide mock beans instead of deprecated @MockitoBean.
 *
 * @since 2.9
 */
@WebMvcTest(BrandingController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("BrandingController Tests")
class BrandingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BrandingService brandingService;

    @TestConfiguration
    static class TestConfig {
        /**
         * Provides a mock BrandingService for testing.
         *
         * @return mock BrandingService instance
         */
        @Bean
        public BrandingService brandingService() {
            return mock(BrandingService.class);
        }
    }

    @Test
    @DisplayName("Should get branding")
    void shouldGetBranding() throws Exception {
        // Given
        BrandingResponse response = BrandingResponse.builder()
                .id(1L)
                .displayName("Test Center")
                .primaryColor("#3B82F6")
                .secondaryColor("#8B5CF6")
                .accentColor("#10B981")
                .build();

        when(brandingService.getBranding()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/settings/branding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("Test Center"))
                .andExpect(jsonPath("$.data.primaryColor").value("#3B82F6"));
    }

    @Test
    @DisplayName("Should update branding")
    void shouldUpdateBranding() throws Exception {
        // Given
        UpdateBrandingRequest request = UpdateBrandingRequest.builder()
                .displayName("Updated Center")
                .tagline("New Tagline")
                .primaryColor("#FF0000")
                .secondaryColor("#00FF00")
                .accentColor("#0000FF")
                .contactEmail("new@test.com")
                .contactPhone("0909090909")
                .build();

        BrandingResponse response = BrandingResponse.builder()
                .id(1L)
                .displayName("Updated Center")
                .tagline("New Tagline")
                .primaryColor("#FF0000")
                .build();

        when(brandingService.updateBranding(any(UpdateBrandingRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/v1/settings/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("Updated Center"))
                .andExpect(jsonPath("$.data.tagline").value("New Tagline"))
                .andExpect(jsonPath("$.data.primaryColor").value("#FF0000"));
    }

    @Test
    @DisplayName("Should reject invalid color format")
    void shouldRejectInvalidColorFormat() throws Exception {
        // Given
        UpdateBrandingRequest request = UpdateBrandingRequest.builder()
                .displayName("Test")
                .primaryColor("invalid-color")
                .secondaryColor("#00FF00")
                .accentColor("#0000FF")
                .build();

        // When & Then
        mockMvc.perform(put("/api/v1/settings/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject invalid email")
    void shouldRejectInvalidEmail() throws Exception {
        // Given
        UpdateBrandingRequest request = UpdateBrandingRequest.builder()
                .displayName("Test")
                .primaryColor("#FF0000")
                .secondaryColor("#00FF00")
                .accentColor("#0000FF")
                .contactEmail("invalid-email")
                .build();

        // When & Then
        mockMvc.perform(put("/api/v1/settings/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should upload logo")
    void shouldUploadLogo() throws Exception {
        // Given
        String logoUrl = "https://s3.amazonaws.com/bucket/logo.png";
        BrandingResponse response = BrandingResponse.builder()
                .id(1L)
                .logoUrl(logoUrl)
                .build();

        when(brandingService.uploadLogo(eq(logoUrl))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/settings/branding/logo")
                        .param("fileUrl", logoUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.logoUrl").value(logoUrl));
    }

    @Test
    @DisplayName("Should upload favicon")
    void shouldUploadFavicon() throws Exception {
        // Given
        String faviconUrl = "https://s3.amazonaws.com/bucket/favicon.ico";
        BrandingResponse response = BrandingResponse.builder()
                .id(1L)
                .faviconUrl(faviconUrl)
                .build();

        when(brandingService.uploadFavicon(eq(faviconUrl))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/settings/branding/favicon")
                        .param("fileUrl", faviconUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.faviconUrl").value(faviconUrl));
    }

    @Test
    @DisplayName("Should reject empty display name")
    void shouldRejectEmptyDisplayName() throws Exception {
        // Given
        UpdateBrandingRequest request = UpdateBrandingRequest.builder()
                .displayName("")
                .primaryColor("#FF0000")
                .secondaryColor("#00FF00")
                .accentColor("#0000FF")
                .build();

        // When & Then
        mockMvc.perform(put("/api/v1/settings/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
