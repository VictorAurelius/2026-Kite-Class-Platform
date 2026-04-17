package com.kiteclass.core.module.parent.repository;

import com.kiteclass.core.module.parent.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data-access for {@link Parent}.
 *
 * <p>All finder methods include {@code AndDeletedFalse} to honour the soft-delete
 * contract from {@link com.kiteclass.core.common.entity.BaseEntity}. Multi-tenant
 * isolation is enforced via the Hibernate {@code tenantFilter} (activated by
 * {@link com.kiteclass.core.config.TenantFilterInterceptor}); the explicit
 * {@code existsByEmailAndInstanceIdAndDeletedFalse} variant is used when the
 * filter is not active (e.g., internal endpoints that accept a raw tenant id).
 *
 * @since 2.14.0
 */
@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {

    /**
     * @param id parent id
     * @return parent if present and not soft-deleted
     */
    Optional<Parent> findByIdAndDeletedFalse(Long id);

    /**
     * Tenant-scoped lookup by email. Returns empty when the email is unused in
     * this tenant (which is the condition to allow invitation issuance).
     */
    Optional<Parent> findByEmailAndInstanceIdAndDeletedFalse(String email, UUID instanceId);

    /**
     * Returns {@code true} when a non-deleted parent with the given email
     * already exists in this tenant. Used by the invitation service to reject
     * duplicate invites and by the redemption flow to prevent double-creates.
     */
    boolean existsByEmailAndInstanceIdAndDeletedFalse(String email, UUID instanceId);
}
