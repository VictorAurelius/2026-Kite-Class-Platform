package com.kitehub.branding.service;

import com.kitehub.branding.domain.entity.BrandingTemplate;
import com.kitehub.branding.repository.BrandingTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for template gallery operations.
 * <p>
 * Provides instant branding by allowing users to browse and apply
 * pre-built templates without AI generation.
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateGalleryService {

    private final BrandingTemplateRepository templateRepository;

    /**
     * List templates, optionally filtered by category.
     *
     * @param category optional category filter
     * @return list of active templates
     */
    public List<BrandingTemplate> listTemplates(String category) {
        if (category != null && !category.isBlank()) {
            log.debug("Listing templates for category: {}", category);
            return templateRepository.findByCategoryAndActiveTrue(category);
        }
        log.debug("Listing all active templates");
        return templateRepository.findByActiveTrueOrderByNameAsc();
    }

    /**
     * Get a single template by ID.
     *
     * @param id template ID
     * @return optional template
     */
    public Optional<BrandingTemplate> getTemplate(UUID id) {
        return templateRepository.findById(id);
    }

    /**
     * Apply a template to an instance.
     * <p>
     * Returns the theme config JSON for the frontend to apply immediately.
     *
     * @param templateId template ID
     * @param instanceId instance ID to apply to
     * @return theme config JSON string, or empty if template not found
     */
    public Optional<String> applyTemplate(UUID templateId, UUID instanceId) {
        return templateRepository.findById(templateId)
                .filter(BrandingTemplate::isActive)
                .map(template -> {
                    log.info("Applying template '{}' to instance {}", template.getName(), instanceId);
                    return template.getThemeConfig();
                });
    }
}
