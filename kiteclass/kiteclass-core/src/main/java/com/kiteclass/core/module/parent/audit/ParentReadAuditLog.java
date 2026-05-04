package com.kiteclass.core.module.parent.audit;

import com.kiteclass.core.common.entity.BaseEntity;
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

import java.time.LocalDateTime;

/**
 * Append-only row recording one parent-side facet read.
 *
 * <p>Written by {@link ParentReadAuditLogService} from each facet service
 * (transcript / attendance / fees / conduct / notifications) AFTER the
 * scope guard accepts but BEFORE returning data — the row reflects the
 * intent to surface the data, even if the caller never receives the
 * response.
 *
 * <p>Phase 1B v1 skeleton: minimum fields to answer "who read what when".
 * IP, user agent, request id, and the 5-year retention sweeper are
 * deferred to GAP-321b.4. The skeleton on its own already meets PDPL
 * Decree 13/2023 Art 16 traceability; the deferred fields enrich audit
 * investigations.
 *
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@Entity
@Table(name = "parent_read_audit_log",
        indexes = {
                @Index(name = "idx_parent_read_audit_parent_child_time",
                        columnList = "parent_id, child_id, read_at"),
                @Index(name = "idx_parent_read_audit_instance_facet",
                        columnList = "instance_id, facet"),
                @Index(name = "idx_parent_read_audit_deleted",
                        columnList = "deleted")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentReadAuditLog extends BaseEntity {

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Enumerated(EnumType.STRING)
    @Column(name = "facet", nullable = false, length = 20)
    private ParentFacet facet;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;
}
