package com.kiteclass.core.module.parent.entity;

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

import java.time.Instant;

/**
 * Minimal v1 row in the parent complaint queue (Wave 19 — GAP-321c Phase 1C).
 *
 * <p>Persists the parent's free-text complaint scoped by `(parent_id, student_id)`
 * with a status state-machine stub. Full workflow (4-level escalation,
 * attachments, resolver UI) lands in GAP-339; this v1 satisfies Đ.83 K2
 * "communication right" by ensuring the complaint is captured + auditable
 * even before the back-office workflow ships.
 *
 * @author KiteClass Team
 * @since 2.19.0 (Wave 19 — GAP-321c Phase 1C v1)
 */
@Entity
@Table(name = "parent_complaint_queue",
        indexes = {
                @Index(name = "idx_parent_complaint_queue_parent",
                        columnList = "parent_id"),
                @Index(name = "idx_parent_complaint_queue_student",
                        columnList = "student_id"),
                @Index(name = "idx_parent_complaint_queue_status",
                        columnList = "status")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentComplaint extends BaseEntity {

    /** Authenticated parent who filed the complaint. */
    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    /** Linked student the complaint concerns. */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** Free-text complaint body (Vietnamese, no length cap on TEXT column). */
    @Column(name = "complaint_text", nullable = false, columnDefinition = "TEXT")
    private String complaintText;

    /** Status state-machine stub — always {@code PENDING} on insert (Wave 19). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    /** Set when status flips to {@code RESOLVED} or {@code REJECTED}. */
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /**
     * Status state machine. Wave 19 v1 only emits {@code PENDING}; full
     * transitions (PENDING → IN_REVIEW → RESOLVED/REJECTED) ship in
     * GAP-339.
     */
    public enum Status {
        PENDING,
        IN_REVIEW,
        RESOLVED,
        REJECTED
    }
}
