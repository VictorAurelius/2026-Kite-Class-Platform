package com.kitehub.subscription.feedback.entity;

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
 * In-app feedback widget submission (GAP-542 Wave 78 Bucket F).
 *
 * <p>Schema mirror of V44 Flyway migration. Public POST /api/v1/feedback
 * persists rows; the email-survey scheduler ({@code FeedbackSurveyScheduler})
 * uses {@code email} + {@code createdAt} for day-7/14 reminder digests.</p>
 *
 * <p>Standalone {@code BIGSERIAL} entity mirroring
 * {@link com.kitehub.subscription.beta.entity.BetaAccessRequest} precedent —
 * not extending {@code BaseEntity}. The {@code publicId} (UUID) is the
 * client-exposed reference in {@code FeedbackSubmissionResponse}.</p>
 *
 * @since Wave 78 — GAP-542
 */
@Entity
@Table(
        name = "feedback_submissions",
        indexes = {
                @Index(name = "idx_feedback_submissions_status_created", columnList = "status, created_at"),
                @Index(name = "idx_feedback_submissions_tenant_created", columnList = "tenant_id, created_at"),
                @Index(name = "idx_feedback_submissions_email_created", columnList = "email, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /** UUID exposed to clients as {@code FeedbackSubmissionResponse.id}. */
    @Column(name = "public_id", nullable = false, updatable = false, unique = true)
    private UUID publicId;

    /** 1-5 rating per BR-FEEDBACK-001 (1=very poor, 5=excellent). */
    @Column(name = "rating", nullable = false)
    private Short rating;

    /** 5-2000 chars trimmed; UTF-8 (Vietnamese welcome). */
    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    private String comment;

    /** Optional — required only when user opts into follow-up. */
    @Column(name = "email", length = 320)
    private String email;

    /** Optional — FE auto-populates window.location.href at submit. */
    @Column(name = "page_url", length = 2000)
    private String pageUrl;

    /** Enum value: BUG | USABILITY | FEATURE_REQUEST | GENERAL. Default GENERAL. */
    @Column(name = "category", nullable = false, length = 50)
    @Builder.Default
    private String category = "GENERAL";

    /** Auto-attached from JWT claim when authenticated; nullable for public submit. */
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    /** Auto-attached from JWT subject when authenticated; nullable for public submit. */
    @Column(name = "user_id", length = 100)
    private String userId;

    /** IPv6-safe client IP for rate-limit forensic audit. */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    /** Workflow status: RECEIVED (new) | REVIEWED (admin triaged) | ARCHIVED. */
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "RECEIVED";

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
        if (this.category == null) {
            this.category = "GENERAL";
        }
        if (this.status == null) {
            this.status = "RECEIVED";
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
