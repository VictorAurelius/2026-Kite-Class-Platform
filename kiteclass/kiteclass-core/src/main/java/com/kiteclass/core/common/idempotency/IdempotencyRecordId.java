package com.kiteclass.core.common.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Wave beta-readiness-2 Bucket A — Composite primary key for
 * {@link IdempotencyRecord} (GAP-730).
 *
 * <p>{@code (tenant_id, idempotency_key, scope)} — tenant scoping prevents
 * cross-tenant collision; scope scoping lets the same client UUID be reused
 * safely across disjoint domains (signup vs enrollment).
 *
 * @since 3.1.0 (Wave beta-readiness-2 Bucket A)
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IdempotencyRecordId implements Serializable {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 32)
    private IdempotencyScope scope;
}
