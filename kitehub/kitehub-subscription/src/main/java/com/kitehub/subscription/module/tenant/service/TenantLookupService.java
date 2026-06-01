package com.kitehub.subscription.module.tenant.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.subscription.api.dto.TenantResolveDto;
import com.kitehub.subscription.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Read-only lookup service for the public tenant-resolve endpoint
 * (Wave tenant-domain-1 Bucket B, GAP-813).
 *
 * <p>Maps {@link Instance} entity rows queried by subdomain slug into the
 * minimal anonymous-safe {@link TenantResolveDto} shape per
 * {@code documents/01-business/kitehub/marketing/api-contract.md} §9.1.3.</p>
 *
 * <p>Security boundary: the underlying repository method
 * {@link InstanceRepository#findBySubdomainAndDeletedFalse(String)} excludes
 * soft-deleted rows; this service further filters by status — only
 * {@link InstanceStatus#ACTIVE} rows return a 200-shape DTO; SUSPENDED /
 * DELETED rows propagate up so the controller can render 410 GONE; missing
 * rows return {@link Optional#empty()} so the controller can render 404.</p>
 *
 * @author KiteHub Team
 * @since Wave tenant-domain-1 Bucket B (GAP-813)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantLookupService {

    private final InstanceRepository instanceRepository;

    /**
     * Find tenant by subdomain slug (lowercase exact match). Returns the raw
     * Instance entity so the controller can dispatch on
     * {@link InstanceStatus} to choose 200 vs 410 mapping.
     *
     * <p>Excludes soft-deleted rows by construction (delegates to
     * {@code findBySubdomainAndDeletedFalse}).</p>
     *
     * @param slug normalized subdomain slug (caller validates format)
     * @return matching Instance or empty if not found / soft-deleted
     */
    @Transactional(readOnly = true)
    public Optional<Instance> findBySubdomain(String slug) {
        return instanceRepository.findBySubdomainAndDeletedFalse(slug);
    }

    /**
     * Project an Instance entity into the anonymous-safe DTO shape.
     *
     * <p>Only ID + subdomain + organization name + status are surfaced;
     * sensitive fields (database URL/credentials, contact email, owner ID,
     * trial / subscription dates) NEVER leak through this endpoint.</p>
     *
     * @param instance the entity to project (non-null)
     * @return DTO shaped per §9.1.3 contract
     */
    public TenantResolveDto toDto(Instance instance) {
        return TenantResolveDto.builder()
                .id(instance.getId())
                .subdomain(instance.getSubdomain())
                .name(instance.getOrganizationName())
                .status(instance.getStatus() != null ? instance.getStatus().name() : null)
                .build();
    }
}
