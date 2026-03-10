package com.kitehub.gateway.repository;

import com.kitehub.platform.domain.entity.Instance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Instance lookup in Gateway.
 *
 * @since 1.0
 */
@Repository
public interface InstanceRepository extends JpaRepository<Instance, UUID> {

    /**
     * Find instance by subdomain.
     *
     * @param subdomain the subdomain (e.g., "customer1")
     * @return Optional containing the instance if found
     */
    Optional<Instance> findBySubdomain(String subdomain);

    /**
     * Find instance by custom domain.
     *
     * @param customDomain the custom domain (e.g., "school.example.com")
     * @return Optional containing the instance if found
     */
    Optional<Instance> findByCustomDomain(String customDomain);
}
