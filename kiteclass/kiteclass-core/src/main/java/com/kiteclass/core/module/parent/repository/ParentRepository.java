package com.kiteclass.core.module.parent.repository;

import com.kiteclass.core.module.parent.entity.Parent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Finds a parent by ID with {@code studentLinks} prefetched (GAP-134 anti-N+1).
     *
     * <p>Use when rendering the parent dashboard or audit trails that enumerate
     * the children attached to a parent. Pair with
     * {@link com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository#findByParentIdWithStudent(Long)}
     * when the joined {@code student} record is also needed — that method eagerly
     * fetches the far side of the many-to-many bridge.
     *
     * @param id the parent ID
     * @return Optional containing parent with studentLinks prefetched
     * @since 3.17.0 (GAP-134 expansion — Wave 9.5)
     */
    @EntityGraph(attributePaths = {"studentLinks"})
    @Query("SELECT p FROM Parent p WHERE p.id = :id AND p.deleted = false")
    Optional<Parent> findByIdWithStudentLinks(@Param("id") Long id);
}
