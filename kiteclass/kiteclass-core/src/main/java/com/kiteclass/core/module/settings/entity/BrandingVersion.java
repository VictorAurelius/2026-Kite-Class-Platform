package com.kiteclass.core.module.settings.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Snapshot of a {@link Branding} row at a point in time.
 *
 * <p>GAP-033p (Wave 4) — version history + manual rollback.
 * Automated rollback + A/B tests deferred to a later wave.
 *
 * <p>Invariants:
 * <ul>
 *   <li>{@code versionNumber} is monotonic per {@code instanceId}.</li>
 *   <li>At most one row per instance has {@code active = true}.</li>
 *   <li>{@code snapshotJson} is a complete serialization of the Branding fields
 *       so rollback can re-hydrate without JPA lazy-loading tricks.</li>
 * </ul>
 *
 * @since Wave 4 (GAP-033p)
 */
@Entity
@Table(
        name = "branding_versions",
        indexes = {
                @Index(name = "idx_branding_versions_instance", columnList = "instance_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_version_per_instance",
                        columnNames = {"instance_id", "version_number"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandingVersion extends BaseEntity {

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "jsonb")
    private String snapshotJson;

    /** Non-null when this row was created via rollback — points to the restored version. */
    @Column(name = "rollback_of")
    private Long rollbackOf;

    /** Exactly one row per instance may be active (enforced by partial unique index). */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = false;
}
