package com.kitehub.branding.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pre-built branding template for instant branding.
 * <p>
 * Users can browse and apply templates without AI generation,
 * providing instant branding results (< 1s).
 *
 * @since 1.0
 */
@Entity
@Table(name = "branding_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandingTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "theme_config", nullable = false, columnDefinition = "text")
    private String themeConfig;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
