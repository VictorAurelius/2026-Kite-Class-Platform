package com.kitehub.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Admin audit-log summary DTO for the read API (GAP-774).
 *
 * <p>Read-only projection of {@code com.kitehub.subscription.audit.AdminAuditLog}
 * exposed at {@code /api/v1/admin/audit-logs}. Surfaces the privileged-action
 * audit trail (OWASP A07 / PDPL) for PLATFORM_ADMIN review. JSON snapshot
 * columns ({@code payloadJson} / {@code beforeState} / {@code afterState}) are
 * passed through as raw JSON strings — the viewer renders them client-side.</p>
 *
 * @since 1.0 (Wave GAP-774)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogSummary {

    /**
     * Audit-log row id.
     */
    private Long id;

    /**
     * Admin user who performed the action.
     */
    private UUID adminUserId;

    /**
     * Action name (e.g. {@code BETA_REQUEST_APPROVE}).
     */
    private String action;

    /**
     * JPA entity type the action targeted (e.g. {@code beta_access_request}).
     */
    private String targetEntityType;

    /**
     * JPA entity id the action targeted.
     */
    private String targetEntityId;

    /**
     * Semantic resource type (config key, RBAC role, etc.) — distinct from
     * {@link #targetEntityType}.
     */
    private String targetResourceType;

    /**
     * Fully-qualified resource id (e.g. {@code tenant/UUID}).
     */
    private String targetResourceId;

    /**
     * Request IP captured at action time.
     */
    private String requestIp;

    /**
     * User-agent captured at action time.
     */
    private String userAgent;

    /**
     * Correlation key with gateway {@code X-Request-Id} / trace id.
     */
    private String requestId;

    /**
     * Redacted JSON snapshot of method arguments (raw JSON string).
     */
    private String payloadJson;

    /**
     * Resource state snapshot before the action (raw JSON string, nullable for CREATE).
     */
    private String beforeState;

    /**
     * Resource state snapshot after the action (raw JSON string, nullable for DELETE).
     */
    private String afterState;

    /**
     * Whether the action succeeded.
     */
    private boolean success;

    /**
     * Error message when {@link #success} is false.
     */
    private String errorMessage;

    /**
     * Action timestamp.
     */
    private LocalDateTime createdAt;
}
