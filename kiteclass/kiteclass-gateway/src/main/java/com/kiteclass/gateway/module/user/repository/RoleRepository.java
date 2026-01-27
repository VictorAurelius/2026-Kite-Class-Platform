package com.kiteclass.gateway.module.user.repository;

import com.kiteclass.gateway.module.user.entity.Role;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for Role entity operations.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Repository
public interface RoleRepository extends R2dbcRepository<Role, Long> {

    /**
     * Find role by code.
     *
     * @param code role code (e.g., "OWNER", "ADMIN")
     * @return Mono of Role
     */
    Mono<Role> findByCode(String code);

    /**
     * Find all system roles.
     *
     * @return Flux of system Roles
     */
    Flux<Role> findByIsSystemTrue();
}
