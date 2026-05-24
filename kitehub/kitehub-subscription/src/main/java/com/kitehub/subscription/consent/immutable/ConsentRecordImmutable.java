package com.kitehub.subscription.consent.immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLInetJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Immutable PDPL consent record với hash chain — Wave br-4 Bucket B (GAP-353b).
 *
 * <p>Append-only audit trail backing {@code BR-PDPL-CONSENT-001..004}. Different
 * từ {@link com.kitehub.subscription.consent.entity.ConsentRecord} (Wave 25 Bucket A
 * pseudonymous visitor_id idempotent upsert) ở 3 điểm:
 * <ul>
 *   <li>IMMUTABLE: chỉ có @Getter (no @Setter) + DB-level RLS blocks UPDATE/DELETE.
 *       Withdraw = INSERT row mới với {@code granted.analytics=false}.</li>
 *   <li>HASH CHAIN: {@code currentHash = SHA-256(prevHash || canonical(row))}
 *       cho tamper-evidence. Chain validation chạy ở
 *       {@link ConsentService#verifyChainIntegrity(Long)}.</li>
 *   <li>GRANTED JSONB: linh hoạt cho category evolution.</li>
 * </ul>
 *
 * <p>Mapped tới table {@code consent_record_immutable} via V56 migration.
 *
 * @since Wave beta-readiness-4 Bucket B — GAP-353b
 */
@Entity
@Table(name = "consent_record_immutable")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentRecordImmutable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /** Soft reference (no FK). Nullable for marketing-surface visitor pre-login. */
    @Column(name = "user_id", updatable = false)
    private Long userId;

    /** Soft reference (no FK). Nullable for pre-tenant marketing-surface visitor. */
    @Column(name = "tenant_id", updatable = false)
    private Long tenantId;

    /**
     * JSONB consent categories.
     * Shape: {"essential":true,"analytics":bool,"marketing":bool[,"personalization":bool,...]}.
     * Stored as String + serialized via Jackson — Hibernate 6 {@code SqlTypes.JSON} handles
     * INET-similar binding mismatch risk per {@code postgres-specific-type-testcontainers.md}.
     */
    @Column(name = "granted", columnDefinition = "jsonb", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String granted;

    /** SHA-256 hex (64 chars) of previous row's currentHash. NULL = chain head for userId. */
    @Column(name = "prev_hash", length = 64, updatable = false)
    private String prevHash;

    /** SHA-256(COALESCE(prevHash,"") || canonical(row content)). Mandatory. */
    @Column(name = "current_hash", length = 64, nullable = false, updatable = false)
    private String currentHash;

    /**
     * INET — Postgres-native validated. {@code InetJdbcType} explicit binding tránh
     * binding mismatch character varying ↔ inet (per audit RCA 2026-05-16
     * postgres-specific-type-testcontainers.md mandate).
     */
    @Column(name = "ip_address", columnDefinition = "inet", nullable = false, updatable = false)
    @JdbcType(PostgreSQLInetJdbcType.class)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT", nullable = false, updatable = false)
    private String userAgent;

    @Column(name = "signed_at", nullable = false, updatable = false)
    private OffsetDateTime signedAt;
}
