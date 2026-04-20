package com.kiteclass.core.module.branding.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.retention.Retention;
import com.kiteclass.core.module.retention.RetentionBucket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * BrandingResource — a branding artifact (logo, banner, hero, ...) produced via one of
 * 3 paths (STATIC / TEMPLATE / FULL_AI).
 *
 * <p>Business rules:
 * <ul>
 *   <li>BR-RES-001: (tenant, type, category) uniquely identify active resource per slot</li>
 *   <li>BR-RES-002: TEMPLATE resources MUST set templateId</li>
 *   <li>BR-RES-003: FULL_AI resources MUST set aiJobId</li>
 *   <li>BR-RES-004: STATIC resources MUST NOT set templateId or aiJobId</li>
 * </ul>
 *
 * @since 3.16.0 (GAP-007, ADR-005)
 */
@Entity
@Table(
        name = "branding_resources",
        indexes = {
                @Index(name = "idx_branding_resource_type", columnList = "instance_id,type"),
                @Index(name = "idx_branding_resource_category", columnList = "category"),
                @Index(name = "idx_branding_resource_deleted", columnList = "deleted"),
                // GAP-129 — composite index backing tenant-scoped lookup
                // (instance_id, deleted). Mirror of V45 migration so tests using
                // ddl-auto=create-drop also create this index.
                @Index(name = "idx_branding_resources_instance_deleted",
                        columnList = "instance_id,deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Retention(RetentionBucket.PURGE_ON_REQUEST)
public class BrandingResource extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private ResourceType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private ResourceCategory category;

    @Column(name = "storage_url", length = 500)
    private String storageUrl;

    /**
     * FK to image_templates when category=TEMPLATE; null otherwise.
     * (Not modelled as @ManyToOne yet — templates live in separate bounded context.)
     */
    @Column(name = "template_id")
    private Long templateId;

    /**
     * FK to branding_jobs when category=FULL_AI; null otherwise.
     */
    @Column(name = "ai_job_id")
    private UUID aiJobId;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    /**
     * Validate category-specific invariants (BR-RES-002..004).
     *
     * @throws IllegalStateException if invariants violated
     */
    public void validateInvariants() {
        if (category == ResourceCategory.TEMPLATE && templateId == null) {
            throw new IllegalStateException("TEMPLATE resource requires templateId");
        }
        if (category == ResourceCategory.FULL_AI && aiJobId == null) {
            throw new IllegalStateException("FULL_AI resource requires aiJobId");
        }
        if (category == ResourceCategory.STATIC && (templateId != null || aiJobId != null)) {
            throw new IllegalStateException("STATIC resource must not set templateId/aiJobId");
        }
    }
}
