package com.kiteclass.gateway.module.user.repository;

import com.kiteclass.gateway.module.user.entity.Role;
import com.kiteclass.gateway.module.user.entity.UserRole;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for UserRole entity operations.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Repository
public interface UserRoleRepository extends R2dbcRepository<UserRole, Long> {

    /**
     * Find all user-role associations for a user.
     *
     * @param userId user ID
     * @return Flux of UserRole
     */
    Flux<UserRole> findByUserId(Long userId);

    /**
     * Delete all role assignments for a user.
     *
     * @param userId user ID
     * @return Mono of Void
     */
    Mono<Void> deleteByUserId(Long userId);

    /**
     * Find all roles for a user with role details.
     *
     * @param userId user ID
     * @return Flux of Role
     */
    @Query("""
        SELECT r.* FROM roles r
        INNER JOIN user_roles ur ON r.id = ur.role_id
        WHERE ur.user_id = :userId
        ORDER BY r.code
        """)
    Flux<Role> findRolesByUserId(Long userId);
}
