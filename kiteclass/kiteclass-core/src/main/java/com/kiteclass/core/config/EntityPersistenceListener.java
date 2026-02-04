package com.kiteclass.core.config;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA entity listener that automatically sets instanceId before persist/update.
 *
 * <p>Ensures all entities have instanceId set from current TenantContext.
 * Prevents accidental cross-tenant data creation/updates.
 *
 * <p>Triggered on:
 * <ul>
 *   <li>@PrePersist - Before INSERT (new entity)</li>
 *   <li>@PreUpdate - Before UPDATE (existing entity)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.2.0
 * @see com.kiteclass.core.common.entity.BaseEntity
 * @see com.kiteclass.core.common.context.TenantContext
 */
@Slf4j
public class EntityPersistenceListener {

    /**
     * Sets instanceId before INSERT.
     * Gets tenant ID from TenantContext.
     *
     * @param entity the entity about to be persisted
     */
    @PrePersist
    public void setInstanceIdOnPersist(BaseEntity entity) {
        if (entity.getInstanceId() == null && TenantContext.isSet()) {
            entity.setInstanceId(TenantContext.getCurrentTenant());
            log.debug("Auto-set instanceId on persist: {} for entity: {}",
                entity.getInstanceId(), entity.getClass().getSimpleName());
        }
    }

    /**
     * Validates instanceId before UPDATE.
     * Prevents changing instanceId to different tenant.
     *
     * @param entity the entity about to be updated
     */
    @PreUpdate
    public void validateInstanceIdOnUpdate(BaseEntity entity) {
        if (TenantContext.isSet()) {
            var currentTenantId = TenantContext.getCurrentTenant();
            if (!currentTenantId.equals(entity.getInstanceId())) {
                log.error("Attempt to update entity from different tenant! " +
                        "Entity instanceId: {}, Current tenant: {}",
                    entity.getInstanceId(), currentTenantId);
                throw new IllegalStateException(
                    "Cannot update entity from different tenant. " +
                    "Entity belongs to tenant: " + entity.getInstanceId() +
                    ", Current tenant: " + currentTenantId
                );
            }
        }
    }
}
