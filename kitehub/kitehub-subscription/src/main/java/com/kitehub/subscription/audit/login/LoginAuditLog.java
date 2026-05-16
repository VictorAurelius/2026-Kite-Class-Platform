package com.kitehub.subscription.audit.login;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Per-login audit row (GAP-517 / OWASP A07 §2.5).
 *
 * <p>Persisted by {@link LoginAuditService#recordLogin} after every successful
 * password verification. A SHA-256 fingerprint of {@code ip || user_agent}
 * enables new-fingerprint detection for PLATFORM_ADMIN alerts.</p>
 *
 * <p>Retention: 7 years per {@code .claude/rules/logs-format-standard.md} §4
 * (security / audit logs).</p>
 *
 * <p>Sibling of {@link com.kitehub.subscription.audit.AdminAuditLog} (Wave 72a)
 * — admin-action audit; this entity = per-login audit. Different concerns.</p>
 *
 * @since 1.0.0 (Wave 72b Bucket C GAP-517)
 */
@Entity
@Table(name = "login_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt;

    /**
     * Source IP (IPv4 or IPv6 textual form). Backed by VARCHAR(45) as of V52
     * — see migration commentary for the INET→VARCHAR rationale.
     */
    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /** Optional 2-letter ISO country (e.g. {@code VN}). May be null if geo lookup unavailable. */
    @Column(name = "geo_country", length = 8)
    private String geoCountry;

    /** Hex SHA-256 of {@code ip || "|" || user_agent}. Fixed 64 chars. */
    @Column(name = "fingerprint_hash", length = 64)
    private String fingerprintHash;

    @Column(name = "alert_sent", nullable = false)
    @Builder.Default
    private boolean alertSent = false;

    @Column(name = "alert_sent_at")
    private LocalDateTime alertSentAt;

    @PrePersist
    void onCreate() {
        if (loginAt == null) loginAt = LocalDateTime.now();
    }
}
