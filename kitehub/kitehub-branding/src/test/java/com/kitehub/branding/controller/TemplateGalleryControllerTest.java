package com.kitehub.branding.controller;

import com.kitehub.branding.domain.entity.BrandingTemplate;
import com.kitehub.branding.service.TemplateGalleryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for TemplateGalleryController.
 *
 * @since 1.0
 */
@WebMvcTest(TemplateGalleryController.class)
@AutoConfigureMockMvc(addFilters = false)
class TemplateGalleryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TemplateGalleryService templateService;

    private static final String TEMPLATES_URL = "/api/platform/branding/templates";
    private static final String INSTANCE_HEADER = "X-Instance-Id";
    private static final UUID INSTANCE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private BrandingTemplate createTemplate(String name, String category, String themeConfig) {
        return BrandingTemplate.builder()
                .id(UUID.randomUUID())
                .name(name)
                .category(category)
                .themeConfig(themeConfig)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void listTemplates_ReturnsAllActiveTemplates() throws Exception {
        // Given
        List<BrandingTemplate> templates = Arrays.asList(
                createTemplate("Classic Academy", "education",
                        "{\"colors\":{\"primary\":\"#059669\"}}"),
                createTemplate("Modern Education", "education",
                        "{\"colors\":{\"primary\":\"#3B82F6\"}}")
        );
        when(templateService.listTemplates(null)).thenReturn(templates);

        // When & Then
        mockMvc.perform(get(TEMPLATES_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Classic Academy"))
                .andExpect(jsonPath("$[1].name").value("Modern Education"));

        verify(templateService).listTemplates(null);
    }

    @Test
    void listTemplates_WithCategoryFilter_ReturnsFilteredTemplates() throws Exception {
        // Given
        List<BrandingTemplate> templates = List.of(
                createTemplate("Professional Training", "business",
                        "{\"colors\":{\"primary\":\"#1F2937\"}}")
        );
        when(templateService.listTemplates("business")).thenReturn(templates);

        // When & Then
        mockMvc.perform(get(TEMPLATES_URL).param("category", "business"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Professional Training"))
                .andExpect(jsonPath("$[0].category").value("business"));

        verify(templateService).listTemplates("business");
    }

    @Test
    void getTemplate_Found_ReturnsTemplate() throws Exception {
        // Given
        UUID templateId = UUID.randomUUID();
        BrandingTemplate template = createTemplate("Modern Education", "education",
                "{\"colors\":{\"primary\":\"#3B82F6\"},\"fonts\":{\"heading\":\"Inter\"}}");
        template.setId(templateId);

        when(templateService.getTemplate(templateId)).thenReturn(Optional.of(template));

        // When & Then
        mockMvc.perform(get(TEMPLATES_URL + "/{id}", templateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Modern Education"))
                .andExpect(jsonPath("$.category").value("education"))
                .andExpect(jsonPath("$.themeConfig").value("{\"colors\":{\"primary\":\"#3B82F6\"},\"fonts\":{\"heading\":\"Inter\"}}"));
    }

    @Test
    void getTemplate_NotFound_Returns404() throws Exception {
        // Given
        UUID templateId = UUID.randomUUID();
        when(templateService.getTemplate(templateId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get(TEMPLATES_URL + "/{id}", templateId))
                .andExpect(status().isNotFound());
    }

    @Test
    void applyTemplate_Success_ReturnsThemeConfig() throws Exception {
        // Given
        UUID templateId = UUID.randomUUID();
        String themeConfig = "{\"colors\":{\"primary\":\"#3B82F6\"},\"style\":\"modern\"}";

        when(templateService.applyTemplate(eq(templateId), eq(INSTANCE_ID)))
                .thenReturn(Optional.of(themeConfig));

        // When & Then
        mockMvc.perform(post(TEMPLATES_URL + "/{id}/apply", templateId)
                        .header(INSTANCE_HEADER, INSTANCE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeConfig").value(themeConfig))
                .andExpect(jsonPath("$.status").value("applied"));

        verify(templateService).applyTemplate(templateId, INSTANCE_ID);
    }

    @Test
    void applyTemplate_NotFound_Returns404() throws Exception {
        // Given
        UUID templateId = UUID.randomUUID();
        when(templateService.applyTemplate(eq(templateId), eq(INSTANCE_ID)))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post(TEMPLATES_URL + "/{id}/apply", templateId)
                        .header(INSTANCE_HEADER, INSTANCE_ID.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void applyTemplate_MissingInstanceHeader_Returns400() throws Exception {
        // Given
        UUID templateId = UUID.randomUUID();

        // When & Then - missing X-Instance-Id header should fail
        mockMvc.perform(post(TEMPLATES_URL + "/{id}/apply", templateId))
                .andExpect(status().isBadRequest());
    }
}
