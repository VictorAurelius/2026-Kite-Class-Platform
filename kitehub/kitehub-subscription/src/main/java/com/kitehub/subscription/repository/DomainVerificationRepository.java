package com.kitehub.subscription.repository;

import com.kitehub.platform.domain.entity.Instance;
import org.springframework.data.repository.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Narrow Spring Data repository scoped to the custom-domain verification timeout sweep
 * (GAP-1024). Kept separate from {@link InstanceRepository} on purpose — the domain
 * timeout scheduler needs exactly one stale-pending query + a save, and a dedicated
 * interface avoids widening the (heavily-shared) {@code InstanceRepository} surface.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
public interface DomainVerificationRepository extends Repository<Instance, UUID> {

    /**
     * Find instances stuck in {@code PENDING_VERIFY} whose last activity ({@code updatedAt},
     * {@code @LastModifiedDate}) is older than the given threshold — i.e., the tenant never
     * added the DNS TXT record within the verification timeout window.
     *
     * @param status    domain status to match (always PENDING_VERIFY for the timeout sweep)
     * @param threshold {@code now - timeoutHours}; rows updated before this are stale
     * @return stale instances to transition to FAILED
     */
    List<Instance> findByDomainStatusAndUpdatedAtBeforeAndDeletedFalse(
        Instance.DomainStatus status, LocalDateTime threshold);

    /**
     * Persist a domain-status transition (PENDING_VERIFY → FAILED) from the timeout sweep.
     *
     * @param instance the instance to save
     * @return the saved instance
     */
    Instance save(Instance instance);
}
