package com.kiteclass.core.module.childprotection.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.childprotection.converter.AesGcmAttributeConverter;
import com.kiteclass.core.module.childprotection.enums.VettingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
 * Vetting — staff background-check record per Decree 56/2017/NĐ-CP + Luật Trẻ
 * em 2016 Đ.25 (vetting nhân sự). Phase 1B foundation: entity + service-level
 * state machine + AES-256 on sensitive fields. File upload UI + verify queue
 * UI deferred to Phase 1B follow-up.
 *
 * <p><b>Encrypted fields</b> (AES-256-GCM via {@link AesGcmAttributeConverter}):
 * <ul>
 *   <li>{@code lltpNumber} — LLTP số 2 (criminal-record certificate) document
 *       identifier; PII per PDPL Decree 13/2023 Art 16</li>
 *   <li>{@code policeCheckDetails} — narrative outcome of the police-check /
 *       interview (free-form, may include sensitive info)</li>
 * </ul>
 *
 * <p><b>State machine</b> (BR-VETTING-001 — service layer enforces):
 * <pre>
 *   PENDING → SUBMITTED → INTERVIEW_DONE → APPROVED ⇄ EXPIRED
 *                                         ↘
 *                                          REJECTED (terminal)
 * </pre>
 *
 * <p><b>RBAC</b> (BR-VETTING-003): only SAFEGUARDING_OFFICER role may
 * read/write Vetting records. Staff teachers without APPROVED record are
 * blocked from student PII endpoints (Phase 1B follow-up: filter aspect).
 *
 * <p><b>Soft delete</b> (BR-VETTING-005): inherited from BaseEntity. Retention
 * enforcement (anti-delete on REJECTED, 7-year retention) deferred to
 * GAP-322c Phase 1C.
 *
 * @since Wave 18b2 Bucket B — GAP-322b Phase 1B foundation
 */
@Entity
@Table(
        name = "vettings",
        indexes = {
                @Index(name = "idx_vettings_instance_id", columnList = "instance_id"),
                @Index(name = "idx_vettings_teacher_id", columnList = "teacher_id"),
                @Index(name = "idx_vettings_status", columnList = "status"),
                @Index(name = "idx_vettings_deleted", columnList = "deleted"),
                @Index(name = "idx_vettings_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vetting extends BaseEntity {

    /**
     * FK to {@code users.id} (the teacher being vetted). Plaintext for query
     * efficiency + tenant scoping. Per BR-VETTING-005, soft-delete preserves
     * the row for audit.
     */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /**
     * Lifecycle status (BR-VETTING-001 state machine — service layer enforces
     * transitions; DB CHECK constraint pins values).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VettingStatus status = VettingStatus.PENDING;

    /**
     * LLTP số 2 (criminal-record certificate) document identifier — encrypted
     * at rest via {@link AesGcmAttributeConverter}. Stored as PostgreSQL BYTEA.
     * Per PDPL Decree 13/2023 Art 16 (BR-VETTING-002).
     */
    @Convert(converter = AesGcmAttributeConverter.class)
    @Column(name = "lltp_number", columnDefinition = "BYTEA")
    private String lltpNumber;

    /**
     * Police-check / interview narrative outcome — encrypted at rest. May
     * contain sensitive notes; restricted decryption to SAFEGUARDING_OFFICER
     * (BR-VETTING-003).
     */
    @Convert(converter = AesGcmAttributeConverter.class)
    @Column(name = "police_check_details", columnDefinition = "BYTEA")
    private String policeCheckDetails;

    /**
     * Timestamp at which HR/admin submitted the documents (PENDING →
     * SUBMITTED). Null while PENDING.
     */
    @Column(name = "submitted_at")
    private Instant submittedAt;

    /**
     * Timestamp at which the safeguarding officer concluded the interview
     * (SUBMITTED → INTERVIEW_DONE). Null until reached.
     */
    @Column(name = "interviewed_at")
    private Instant interviewedAt;

    /**
     * Timestamp at which the officer made the APPROVE/REJECT decision.
     */
    @Column(name = "decided_at")
    private Instant decidedAt;

    /**
     * Expiry date for an APPROVED vetting (BR-VETTING-001 — APPROVED → EXPIRED
     * triggered when {@code now > expiresAt}). Per Decree 56/2017 + LLTP ≤2
     * years cadence; concrete cron + reminder ships Phase 1B follow-up.
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * User ID of the safeguarding officer who approved/rejected this record.
     * Audit trail.
     */
    @Column(name = "decided_by_user_id")
    private Long decidedByUserId;
}
