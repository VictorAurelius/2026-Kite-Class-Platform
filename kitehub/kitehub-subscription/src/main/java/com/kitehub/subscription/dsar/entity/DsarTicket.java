package com.kitehub.subscription.dsar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Self-service DSAR (Data Subject Access Request) ticket.
 *
 * <p>Backs {@code BR-PDPL-DSAR-001..005} (see
 * {@code documents/01-business/kitehub/marketing/rules.md}). Stand-alone entity
 * (NOT extending {@code BaseEntity}) because the migration uses {@code BIGSERIAL}
 * for {@code id} per gap spec — {@code BaseEntity} is UUID-based.</p>
 *
 * <p>Public-surface entity: BOTH the POST submit and GET status endpoints are
 * unauthenticated by design — DSAR submitters are not logged-in users by
 * definition (right-to-access often invoked by ex-users or never-signed-up data
 * subjects). Identity verification happens out-of-band via the
 * {@code nationalIdLast4} + {@code requesterEmail} pair plus a DPO callback.</p>
 *
 * @since Wave 26 Bucket A — GAP-353c
 */
@Entity
@Table(
        name = "dsar_ticket",
        indexes = {
                @Index(name = "idx_dsar_ticket_uuid", columnList = "ticket_uuid", unique = true),
                @Index(name = "idx_dsar_ticket_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DsarTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /** Public reference exposed to requester instead of internal BIGSERIAL id. */
    @Column(name = "ticket_uuid", nullable = false, unique = true)
    private UUID ticketUuid;

    @Column(name = "requester_email", nullable = false, length = 320)
    private String requesterEmail;

    @Column(name = "requester_name", nullable = false, length = 200)
    private String requesterName;

    @Column(name = "national_id_last4", nullable = false, length = 4)
    private String nationalIdLast4;

    @Enumerated(EnumType.STRING)
    @Column(name = "right_type", nullable = false, length = 50)
    private DsarRightType rightType;

    @Column(name = "scope", columnDefinition = "TEXT")
    private String scope;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private DsarStatus status = DsarStatus.PENDING;

    @Column(name = "sla_deadline", nullable = false)
    private OffsetDateTime slaDeadline;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.ticketUuid == null) {
            this.ticketUuid = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = DsarStatus.PENDING;
        }
        if (this.slaDeadline == null) {
            // BR-PDPL-DSAR-002: 20-day SLA per PDPL Art 14 + Decree 13/2023 Art 19.
            this.slaDeadline = this.createdAt.plusDays(20);
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
        if ((this.status == DsarStatus.COMPLETED || this.status == DsarStatus.REJECTED)
                && this.resolvedAt == null) {
            this.resolvedAt = this.updatedAt;
        }
    }

    /** True when SLA deadline is in the past and ticket is not closed. */
    public boolean isOverdue() {
        return this.slaDeadline != null
                && this.slaDeadline.isBefore(OffsetDateTime.now())
                && this.status != DsarStatus.COMPLETED
                && this.status != DsarStatus.REJECTED;
    }
}
