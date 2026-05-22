package com.kitehub.subscription.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Admin action audit log (GAP-521 / OWASP A07).
 *
 * <p>Persisted by {@link AdminAuditAspect} around every method annotated with
 * {@link Auditable}. Captures admin user, action, target entity, request
 * provenance (IP + UA), and a redacted JSON snapshot of method arguments.</p>
 *
 * <p>Retention: 7 years per {@code .claude/rules/logs-format-standard.md} §4
 * (security / audit logs).</p>
 *
 * @since 1.0.0 (Wave 72a GAP-521)
 */
@Entity
@Table(name = "admin_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_user_id", nullable = false)
    private UUID adminUserId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "target_entity_type", length = 64)
    private String targetEntityType;

    @Column(name = "target_entity_id", length = 128)
    private String targetEntityId;

    @Column(name = "request_ip", length = 64)
    private String requestIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /**
     * JSONB column. Use raw String (well-formed JSON) to avoid pulling
     * hypersistence-utils dependency just for this. The aspect builds JSON
     * manually via Jackson's ObjectMapper.
     */
    @Column(name = "payload_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payloadJson;

    @Column(nullable = false)
    @Builder.Default
    private boolean success = true;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Wave 92 Bucket A — GAP-521 Phase 2 enrichment.
     *
     * <p>Correlation key với gateway {@code X-Request-Id} / OTel {@code trace_id}.
     * Cho phép join với access logs + APM traces khi forensic investigate.</p>
     */
    @Column(name = "request_id", length = 64)
    private String requestId;

    /**
     * Wave 92 Bucket A — GAP-521 Phase 2 enrichment.
     *
     * <p>Semantic resource type tách biệt với {@link #targetEntityType} (JPA entity
     * name). Vd: {@code config_key}, {@code rbac_role}, {@code system_flag}.</p>
     */
    @Column(name = "target_resource_type", length = 64)
    private String targetResourceType;

    /**
     * Wave 92 Bucket A — GAP-521 Phase 2 enrichment.
     *
     * <p>Fully-qualified resource id (vd {@code tenant/UUID},
     * {@code config/kite.foo.bar}). Tách biệt với {@link #targetEntityId} (chỉ
     * chứa JPA entity PK).</p>
     */
    @Column(name = "target_resource_id", length = 256)
    private String targetResourceId;

    /**
     * Wave 92 Bucket A — GAP-521 Phase 2 enrichment.
     *
     * <p>JSONB snapshot of resource state TRƯỚC action. Defaults to JSON null
     * literal "null" (the 4-char string) when not set, to avoid Hibernate's
     * SqlTypes.JSON adapter rejecting native Java null on insert path.</p>
     */
    @Column(name = "before_state", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String beforeState;

    /**
     * Wave 92 Bucket A — GAP-521 Phase 2 enrichment.
     *
     * <p>JSONB snapshot of resource state SAU action. Pair với {@link #beforeState}.</p>
     */
    @Column(name = "after_state", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String afterState;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        // Wave 104.5 GAP-715 fix: Hibernate 6 SqlTypes.JSON adapter rejects
        // null String → triggers "Could not convert 'java.lang.String' to '[B'".
        // Match OnboardingProgress.stepsJson pattern: default to JSON null literal.
        if (payloadJson == null) payloadJson = "null";
        if (beforeState == null) beforeState = "null";
        if (afterState == null) afterState = "null";
    }
}
