package com.kitehub.subscription.saleslead.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * KiteHub PLATFORM sales lead (GAP-1101 — Enterprise "Liên hệ" capture).
 *
 * <p>Distinct from the {@code kiteclass-core} tenant-marketing {@code leads}/
 * {@code contact_messages} domain (student → center). THIS entity is the
 * KiteHub PLATFORM sales funnel: a prospective center owner contacting KiteHub
 * sales about the Enterprise SaaS plan. See {@code POST /api/platform/sales-leads}.</p>
 *
 * <p>Standalone {@code BIGSERIAL} entity (NOT extending {@code BaseEntity})
 * mirroring {@link com.kitehub.subscription.feedback.entity.FeedbackSubmission}
 * + {@link com.kitehub.subscription.beta.entity.BetaAccessRequest} precedents —
 * the {@code publicId} (UUID) is the client-exposed reference in
 * {@code SalesLeadResponse}.</p>
 *
 * <p>Status machine: NEW (submitted) → CONTACTED (sales reached out) →
 * QUALIFIED → CLOSED. Transitions enforced at service layer.</p>
 *
 * @since GAP-1101
 */
@Entity
@Table(
        name = "sales_leads",
        indexes = {
                @Index(name = "idx_sales_leads_status_created", columnList = "status, created_at"),
                @Index(name = "idx_sales_leads_email_created", columnList = "email, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesLead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /** UUID exposed to clients as {@code SalesLeadResponse.id}. */
    @Column(name = "public_id", nullable = false, updatable = false, unique = true)
    private UUID publicId;

    /** Prospect full name. UTF-8 (Vietnamese welcome). */
    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    /** Prospect contact email (RFC-5321 length ≤320). */
    @Column(name = "email", nullable = false, length = 320)
    private String email;

    /** Prospect phone — VN sales contact channel. */
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    /** Center / organization name. UTF-8. */
    @Column(name = "organization_name", nullable = false, length = 200)
    private String organizationName;

    /** Optional free-text consultation request. */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    /** Plan the prospect is interested in (FREE/BASIC/PREMIUM/ENTERPRISE). Default ENTERPRISE. */
    @Column(name = "plan_interest", nullable = false, length = 50)
    @Builder.Default
    private String planInterest = "ENTERPRISE";

    /** Workflow status: NEW (new) | CONTACTED | QUALIFIED | CLOSED. */
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "NEW";

    /** IPv6-safe client IP for spam/abuse forensic audit. */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
        if (this.planInterest == null) {
            this.planInterest = "ENTERPRISE";
        }
        if (this.status == null) {
            this.status = "NEW";
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
