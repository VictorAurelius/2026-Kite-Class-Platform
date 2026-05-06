package com.kiteclass.core.module.childprotection.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import com.kiteclass.core.module.childprotection.converter.AesGcmAttributeConverter;
import com.kiteclass.core.module.childprotection.enums.IncidentCategory;
import com.kiteclass.core.module.childprotection.enums.IncidentSeverity;
import com.kiteclass.core.module.childprotection.enums.IncidentStatus;
import com.kiteclass.core.module.childprotection.enums.IncidentVisibilityScope;
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
 * Incident — child-protection ticket with field-level encryption on sensitive
 * columns. Phase 1A skeleton: entity + repository + service CRUD with
 * encryption roundtrip.
 *
 * <p><b>Encrypted fields</b> (AES-256-GCM via {@link AesGcmAttributeConverter}):
 * <ul>
 *   <li>{@code description} — narrative of the incident, may contain names of
 *       minors + sensitive details</li>
 *   <li>{@code evidencePaths} — newline-separated MinIO object keys pointing to
 *       evidence (images, recordings); Phase 1B encrypts MinIO bucket itself
 *       (GAP-322b)</li>
 * </ul>
 *
 * <p><b>Compliance:</b>
 * <ul>
 *   <li>BR-CHILD-PROT-001: Multi-tenant isolation via {@code instance_id}
 *       (BaseEntity tenantFilter)</li>
 *   <li>BR-CHILD-PROT-002: Sensitive fields stored encrypted-at-rest per PDPL
 *       Decree 13/2023 Art 16</li>
 *   <li>BR-CHILD-PROT-003: Tampered ciphertext rejected (GCM auth tag) —
 *       integrity guaranteed</li>
 *   <li>BR-CHILD-PROT-004: Soft-delete only (deleted=true); 7-year retention
 *       enforced in Phase 1B (GAP-322c)</li>
 * </ul>
 *
 * <p><b>Phase 1B / 1C deferred</b> (GAP-322b/c):
 * <ul>
 *   <li>Vetting workflow (LLTP upload + verify queue) → GAP-322b</li>
 *   <li>MinIO encrypted bucket for evidence files → GAP-322b</li>
 *   <li>Mandatory-reporting auto-suggest banner (Đ.51) → GAP-322c</li>
 *   <li>Hash-chained non-repudiation audit log → GAP-322c</li>
 *   <li>7-year retention enforcement → GAP-322c</li>
 * </ul>
 *
 * @since 5.x (Wave 18b1 Bucket E — GAP-322 Phase 1A)
 */
@Entity
@Table(
        name = "incidents",
        indexes = {
                @Index(name = "idx_incidents_instance_id", columnList = "instance_id"),
                @Index(name = "idx_incidents_severity", columnList = "severity"),
                @Index(name = "idx_incidents_category", columnList = "category"),
                @Index(name = "idx_incidents_status", columnList = "status"),
                @Index(name = "idx_incidents_reporter", columnList = "reporter_user_id"),
                @Index(name = "idx_incidents_subject_student", columnList = "subject_student_id"),
                @Index(name = "idx_incidents_deleted", columnList = "deleted"),
                @Index(name = "idx_incidents_visibility_scope", columnList = "visibility_scope")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incident extends BaseEntity {

    /**
     * Brief, NON-sensitive title for list views (kept plaintext for indexing
     * + admin triage). Sensitive narrative goes in {@link #description}.
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * Sensitive narrative — encrypted at rest via {@link AesGcmAttributeConverter}.
     * Stored as PostgreSQL BYTEA. May contain names of minors + sensitive
     * details. Per PDPL Decree 13/2023 Art 16 special-protection.
     */
    @Convert(converter = AesGcmAttributeConverter.class)
    @Column(name = "description", columnDefinition = "BYTEA")
    private String description;

    /**
     * Sensitive evidence paths (newline-separated MinIO object keys) —
     * encrypted at rest. Phase 1B encrypts the MinIO bucket itself
     * (GAP-322b).
     */
    @Convert(converter = AesGcmAttributeConverter.class)
    @Column(name = "evidence_paths", columnDefinition = "BYTEA")
    private String evidencePaths;

    /**
     * Severity classification. {@code CRITICAL} + abuse category triggers
     * Đ.51 mandatory-reporting banner in Phase 1B (GAP-322c).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private IncidentSeverity severity;

    /**
     * Incident category. Combined with severity drives reporting workflow.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private IncidentCategory category;

    /**
     * Lifecycle status. Phase 1A allows arbitrary transitions; Phase 1B
     * enforces state-machine rules.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.REPORTED;

    /**
     * User ID of the reporter (PH / HS / GV / staff). Plaintext FK — used for
     * audit trail + reporter notifications. Never null.
     */
    @Column(name = "reporter_user_id", nullable = false)
    private Long reporterUserId;

    /**
     * Student involved as subject of the incident — optional (e.g., bullying
     * involves multiple students; main subject recorded here, others in
     * description). FK to {@code students.id}.
     */
    @Column(name = "subject_student_id")
    private Long subjectStudentId;

    /**
     * User ID of the assigned safeguarding officer (Phase 1B will gate
     * decryption access to this user + Hiệu trưởng + designated counselor).
     */
    @Column(name = "assigned_officer_user_id")
    private Long assignedOfficerUserId;

    /**
     * Visibility scope (Phase 1C, GAP-322c) — controls audience exposure.
     * Defaults to {@link IncidentVisibilityScope#STAFF_ONLY} so legacy rows
     * + new ABUSE / GROOMING / CSAM records never leak to the parent portal
     * conduct facet without explicit officer downgrade. Per BR-CHILD-PROTECT-005.
     *
     * <p>Consumed by Bucket D (Wave 19) {@code ParentConductFacetService}
     * JPQL filter: only {@code PARENT_VISIBLE} + {@code PUBLIC} surfaces in
     * the parent-portal conduct facet.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_scope", nullable = false, length = 32)
    @Builder.Default
    private IncidentVisibilityScope visibilityScope = IncidentVisibilityScope.STAFF_ONLY;

    /**
     * Mandatory retention deadline (Phase 1C v1.5, GAP-359 sub-task 359.1).
     *
     * <p>Set when {@link IncidentService#updateStatus(Long, IncidentStatus)}
     * transitions the incident to {@link IncidentStatus#CLOSED} (closed_at +
     * 7 years per BR-CHILD-PROTECT-008). While in the window soft-delete is
     * BLOCKED via {@code RetentionWindowActiveException}. After expiry the
     * {@code RetentionLifecycleService} cron secure-deletes the row + appends
     * an audit-log entry.
     *
     * <p>Remains {@code null} for non-CLOSED incidents. Compliance: PDPL
     * Decree 13/2023/NĐ-CP Art 16 + Luật Trẻ em 2016 Đ.51 follow-through +
     * BLHS Đ.147 statute-of-limitations alignment.
     */
    @Column(name = "retention_until")
    private Instant retentionUntil;
}
