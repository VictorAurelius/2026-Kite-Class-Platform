package com.kitehub.subscription.audit;

import jakarta.persistence.*;
import lombok.*;

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
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.OTHER)
    private String payloadJson;

    @Column(nullable = false)
    @Builder.Default
    private boolean success = true;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
