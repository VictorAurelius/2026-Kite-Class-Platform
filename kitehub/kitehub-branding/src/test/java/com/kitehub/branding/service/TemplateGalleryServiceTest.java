package com.kitehub.branding.service;

import com.kitehub.branding.domain.entity.BrandingTemplate;
import com.kitehub.branding.repository.BrandingTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for TemplateGalleryService.
 *
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class TemplateGalleryServiceTest {

    @Mock
    private BrandingTemplateRepository templateRepository;

    @InjectMocks
    private TemplateGalleryService templateService;

    private BrandingTemplate createTemplate(String name, String category) {
        return BrandingTemplate.builder()
                .id(UUID.randomUUID())
                .name(name)
                .category(category)
                .themeConfig("{\"colors\":{\"primary\":\"#3B82F6\"}}")
                .active(true)
                .build();
    }

    @Test
    void listTemplates_NullCategory_ReturnsAllActive() {
        // Given
        List<BrandingTemplate> templates = Arrays.asList(
                createTemplate("Classic Academy", "education"),
                createTemplate("Modern Education", "education")
        );
        when(templateRepository.findByActiveTrueOrderByNameAsc()).thenReturn(templates);

        // When
        List<BrandingTemplate> result = templateService.listTemplates(null);

        // Then
        assertThat(result).hasSize(2);
        verify(templateRepository).findByActiveTrueOrderByNameAsc();
    }

    @Test
    void listTemplates_BlankCategory_ReturnsAllActive() {
        // Given
        List<BrandingTemplate> templates = List.of(createTemplate("Template", "general"));
        when(templateRepository.findByActiveTrueOrderByNameAsc()).thenReturn(templates);

        // When
        List<BrandingTemplate> result = templateService.listTemplates("  ");

        // Then
        assertThat(result).hasSize(1);
        verify(templateRepository).findByActiveTrueOrderByNameAsc();
    }

    @Test
    void listTemplates_WithCategory_ReturnsFiltered() {
        // Given
        List<BrandingTemplate> templates = List.of(
                createTemplate("Professional Training", "business")
        );
        when(templateRepository.findByCategoryAndActiveTrue("business")).thenReturn(templates);

        // When
        List<BrandingTemplate> result = templateService.listTemplates("business");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("business");
        verify(templateRepository).findByCategoryAndActiveTrue("business");
    }

    @Test
    void getTemplate_Found_ReturnsTemplate() {
        // Given
        UUID templateId = UUID.randomUUID();
        BrandingTemplate template = createTemplate("Modern Education", "education");
        template.setId(templateId);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        // When
        Optional<BrandingTemplate> result = templateService.getTemplate(templateId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Modern Education");
    }

    @Test
    void getTemplate_NotFound_ReturnsEmpty() {
        // Given
        UUID templateId = UUID.randomUUID();
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When
        Optional<BrandingTemplate> result = templateService.getTemplate(templateId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void applyTemplate_ActiveTemplate_ReturnsThemeConfig() {
        // Given
        UUID templateId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        String themeConfig = "{\"colors\":{\"primary\":\"#3B82F6\"},\"style\":\"modern\"}";

        BrandingTemplate template = createTemplate("Modern Education", "education");
        template.setId(templateId);
        template.setThemeConfig(themeConfig);
        template.setActive(true);

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        // When
        Optional<String> result = templateService.applyTemplate(templateId, instanceId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(themeConfig);
    }

    @Test
    void applyTemplate_InactiveTemplate_ReturnsEmpty() {
        // Given
        UUID templateId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();

        BrandingTemplate template = createTemplate("Inactive Template", "general");
        template.setId(templateId);
        template.setActive(false);

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        // When
        Optional<String> result = templateService.applyTemplate(templateId, instanceId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void applyTemplate_NotFound_ReturnsEmpty() {
        // Given
        UUID templateId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When
        Optional<String> result = templateService.applyTemplate(templateId, instanceId);

        // Then
        assertThat(result).isEmpty();
    }
}
