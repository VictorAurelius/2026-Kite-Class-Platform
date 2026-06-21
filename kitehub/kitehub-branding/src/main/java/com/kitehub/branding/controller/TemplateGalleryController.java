package com.kitehub.branding.controller;

import com.kitehub.branding.domain.entity.BrandingTemplate;
import com.kitehub.branding.security.TenantOwnershipGuard;
import com.kitehub.branding.service.TemplateGalleryService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for browsing and applying pre-built branding templates.
 * <p>
 * Provides instant branding without AI generation (< 1s response time).
 *
 * <p>SLO Tier B (template list/detail reads, no AI in the path).
 * See {@code documents/05-guides/api-performance-slo.md}.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/branding/templates")
@RequiredArgsConstructor
@Tag(name = "Template Gallery", description = "Pre-built branding templates for instant branding")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-b", "controller", "template-gallery"})
public class TemplateGalleryController {

    private final TemplateGalleryService templateService;

    /**
     * GAP-1526 (OWASP A01) — OWNER-tier write authorization for applying a template (mutates the
     * instance branding). Mirrors {@code BrandingJobController.OWNER_AUTHZ}. The read endpoints
     * (list/detail) stay public — the template catalogue carries no tenant data.
     */
    private static final String OWNER_AUTHZ =
            "hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')";

    /**
     * List all active templates, optionally filtered by category.
     *
     * @param category optional category filter (education, business, general)
     * @return list of templates
     */
    @GetMapping
    @Operation(summary = "List branding templates", description = "Returns all active templates, optionally filtered by category")
    public ResponseEntity<List<BrandingTemplate>> listTemplates(
            @RequestParam(required = false) String category) {

        List<BrandingTemplate> templates = templateService.listTemplates(category);
        return ResponseEntity.ok(templates);
    }

    /**
     * Get a single template by ID.
     *
     * @param id template ID
     * @return template or 404
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID", description = "Returns a single template with full theme config")
    public ResponseEntity<BrandingTemplate> getTemplate(@PathVariable UUID id) {

        return templateService.getTemplate(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Apply a template to an instance for instant branding.
     *
     * @param id template ID
     * @param instanceId instance ID from gateway header
     * @return theme config JSON to apply, or 404 if template not found
     */
    @PostMapping("/{id}/apply")
    @PreAuthorize(OWNER_AUTHZ)
    @Operation(summary = "Apply template to instance", description = "Returns theme config for instant application")
    public ResponseEntity<Map<String, String>> applyTemplate(
            @PathVariable UUID id,
            @RequestHeader("X-Instance-Id") UUID instanceId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader) {

        // GAP-1526 (OWASP A01 / IDOR) — bind the client X-Instance-Id to the gateway-trusted tenant
        // so an OWNER cannot apply a template into another tenant's instance (platform admin bypass).
        TenantOwnershipGuard.requireInstanceOwnership(instanceId, tenantHeader);

        return templateService.applyTemplate(id, instanceId)
                .map(themeConfig -> ResponseEntity.ok(Map.of(
                        "themeConfig", themeConfig,
                        "status", "applied"
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
