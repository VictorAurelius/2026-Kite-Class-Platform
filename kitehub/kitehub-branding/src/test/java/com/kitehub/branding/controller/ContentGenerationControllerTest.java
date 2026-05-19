package com.kitehub.branding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.dto.Feature;
import com.kitehub.branding.dto.LandingPageContent;
import com.kitehub.branding.dto.LogoAnalysis;
import com.kitehub.branding.service.ContentGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for ContentGenerationController.
 *
 * <p>Controller is now reactive ({@code Mono<ResponseEntity<...>>}) so we use
 * {@code asyncDispatch} to complete the async request before asserting.</p>
 *
 * @since 1.0
 */
@WebMvcTest(ContentGenerationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContentGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContentGenerationService contentGenerationService;

    @Test
    void testGenerateContent_Success() throws Exception {
        // Given
        LogoAnalysis logoAnalysis = LogoAnalysis.builder()
                .primaryColor("#FF5722")
                .secondaryColor("#2196F3")
                .accentColor("#4CAF50")
                .theme("MODERN")
                .targetAudience("Students")
                .brandPersonality(Arrays.asList("Friendly"))
                .build();

        ContentGenerationController.ContentGenerationRequest requestBody =
                new ContentGenerationController.ContentGenerationRequest();
        requestBody.setLogoAnalysis(logoAnalysis);
        requestBody.setOrgName("Kite English Center");
        requestBody.setLanguage("vi");

        LandingPageContent mockContent = LandingPageContent.builder()
                .heroTitle("Trung Tâm Tiếng Anh Hàng Đầu")
                .heroSubtitle("Phương pháp học hiện đại")
                .tagline("Học Vui, Tiến Xa")
                .aboutUs("Về chúng tôi...")
                .mission("Sứ mệnh của chúng tôi...")
                .vision("Tầm nhìn của chúng tôi...")
                .features(Arrays.asList(
                        Feature.builder()
                                .title("Feature 1")
                                .description("Description 1")
                                .icon("video")
                                .build()
                ))
                .ctaText("Đăng Ký Ngay")
                .build();

        when(contentGenerationService.generateLandingPageContent(
                any(LogoAnalysis.class),
                anyString(),
                anyString()
        )).thenReturn(Mono.just(mockContent));

        // When: start async request, then dispatch
        MvcResult asyncResult = mockMvc.perform(post("/api/platform/branding/content/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Then
        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heroTitle").value("Trung Tâm Tiếng Anh Hàng Đầu"))
                .andExpect(jsonPath("$.heroSubtitle").value("Phương pháp học hiện đại"))
                .andExpect(jsonPath("$.tagline").value("Học Vui, Tiến Xa"))
                .andExpect(jsonPath("$.aboutUs").value("Về chúng tôi..."))
                .andExpect(jsonPath("$.mission").value("Sứ mệnh của chúng tôi..."))
                .andExpect(jsonPath("$.vision").value("Tầm nhìn của chúng tôi..."))
                .andExpect(jsonPath("$.features[0].title").value("Feature 1"))
                .andExpect(jsonPath("$.features[0].icon").value("video"))
                .andExpect(jsonPath("$.ctaText").value("Đăng Ký Ngay"));
    }

    @Test
    void testGenerateContent_DefaultLanguage() throws Exception {
        // Given - request without language (should default to "vi")
        LogoAnalysis logoAnalysis = LogoAnalysis.builder()
                .primaryColor("#FF5722")
                .secondaryColor("#2196F3")
                .accentColor("#4CAF50")
                .theme("MODERN")
                .targetAudience("Students")
                .brandPersonality(Arrays.asList("Friendly"))
                .build();

        ContentGenerationController.ContentGenerationRequest requestBody =
                new ContentGenerationController.ContentGenerationRequest();
        requestBody.setLogoAnalysis(logoAnalysis);
        requestBody.setOrgName("Kite English Center");
        // No language set

        LandingPageContent mockContent = LandingPageContent.builder()
                .heroTitle("Hero Title")
                .heroSubtitle("Hero Subtitle")
                .tagline("Tagline")
                .aboutUs("About")
                .mission("Mission")
                .vision("Vision")
                .features(List.of())
                .ctaText("CTA")
                .build();

        when(contentGenerationService.generateLandingPageContent(
                any(LogoAnalysis.class),
                anyString(),
                eq("vi")  // Should default to "vi"
        )).thenReturn(Mono.just(mockContent));

        MvcResult asyncResult = mockMvc.perform(post("/api/platform/branding/content/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk());
    }

    @Test
    void testGetContent_NotFound() throws Exception {
        // Given
        String instanceId = "550e8400-e29b-41d4-a716-446655440000";

        // When & Then
        mockMvc.perform(get("/api/platform/branding/content/{instanceId}", instanceId))
                .andExpect(status().isNotFound());
    }
}
