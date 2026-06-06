package com.kiteclass.core.module.auth.entity;

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
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * KC-native login credential for tenant-scoped roles (PARENT/TEACHER/STUDENT).
 *
 * <p>Wave auth-1 (Option B, GAP-725) — kiteclass-core issues its own JWT for these
 * roles instead of KH subscription (which only knows OWNER/STAFF). The credential row
 * is the source of the JWT's three identity claims: {@code referenceId = entityId}
 * (the kiteclass domain row id consumed by {@code @authz.hasAccessToChild}),
 * {@code tenantId = instanceId}, {@code role = entityType}.
 *
 * <p>Intentionally NOT extending {@code BaseEntity}: login lookup is PRE-auth (no
 * {@code TenantContext} set), so the row must be reachable without the tenant
 * Hibernate {@code @Filter} / RLS GUC. Email is globally unique.
 */
@Entity
@Table(name = "auth_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable UUID forwarded as {@code sub} / X-User-Id (audit identity). */
    @Column(name = "user_uuid", nullable = false, unique = true)
    private UUID userUuid;

    /** PARENT | TEACHER | STUDENT — maps to the JWT {@code role} claim. */
    @Column(name = "entity_type", nullable = false, length = 16)
    private String entityType;

    /** Numeric kiteclass domain id (parents.id / teachers.id / students.id) = referenceId. */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /** BCrypt hash. */
    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    /** Tenant binding = JWT {@code tenantId} claim. */
    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
