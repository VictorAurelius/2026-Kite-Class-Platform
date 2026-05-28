package com.kiteclass.core.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Base entity with audit fields and multi-tenant support for all domain entities.
 *
 * <p>Provides:
 * <ul>
 *   <li>Multi-tenant isolation (instanceId)</li>
 *   <li>Audit timestamps (createdAt, updatedAt)</li>
 *   <li>Audit users (createdBy, updatedBy)</li>
 *   <li>Soft delete support (deleted flag)</li>
 *   <li>Optimistic locking (version)</li>
 * </ul>
 *
 * <p>JPA auditing is enabled via @EntityListeners(AuditingEntityListener.class)
 * and must be configured in JpaConfig with @EnableJpaAuditing.
 *
 * <p>Multi-tenant isolation is enforced via Hibernate filter "tenantFilter".
 * The filter is automatically enabled by TenantFilterInterceptor for all API requests.
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners({AuditingEntityListener.class, com.kiteclass.core.config.EntityPersistenceListener.class})
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "instance_id = :tenantId")
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Instance ID for multi-tenant isolation.
     * Every entity must belong to exactly one instance (tenant).
     *
     * <p>This field is automatically set when entity is persisted.
     * The value is taken from TenantContext which is populated from X-Tenant-Id header.
     *
     * <p>Hibernate filter ensures queries only return entities for current tenant.
     *
     * @see com.kiteclass.core.common.context.TenantContext
     * @see com.kiteclass.core.config.TenantFilterInterceptor
     */
    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    /**
     * Timestamp when entity was created.
     * Automatically set by JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when entity was last updated.
     * Automatically updated by JPA auditing.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * User ID (UUID) who created this entity.
     * Automatically set by JPA auditing via AuditorAware bean.
     *
     * <p>Stores the X-User-Id UUID (JWT {@code sub} claim) — there is no numeric
     * user id in the auth token (GAP-795).
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    /**
     * User ID (UUID) who last updated this entity.
     * Automatically updated by JPA auditing via AuditorAware bean.
     *
     * <p>Stores the X-User-Id UUID (JWT {@code sub} claim) — see {@link #createdBy}.
     */
    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    /**
     * Soft delete flag. When true, entity is considered deleted.
     * Queries should filter by deleted=false unless specifically querying deleted entities.
     */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    /**
     * Version for optimistic locking.
     * Prevents concurrent modification conflicts.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Marks entity as deleted (soft delete).
     * Does not actually remove the record from database.
     */
    public void markAsDeleted() {
        this.deleted = true;
    }

    /**
     * Restores a soft-deleted entity.
     */
    public void restore() {
        this.deleted = false;
    }

    /**
     * Checks if entity is deleted.
     *
     * @return true if entity is deleted
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(this.deleted);
    }

    /**
     * Checks if entity is new (not yet persisted).
     *
     * @return true if entity has no ID
     */
    public boolean isNew() {
        return this.id == null;
    }
}
