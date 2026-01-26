package com.kiteclass.gateway.module.user.repository;

import com.kiteclass.gateway.module.user.entity.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for User entity operations.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {

    /**
     * Find user by ID (non-deleted only).
     *
     * @param id user ID
     * @return Mono of User
     */
    Mono<User> findByIdAndDeletedFalse(Long id);

    /**
     * Find user by email (non-deleted only).
     *
     * @param email user email
     * @return Mono of User
     */
    Mono<User> findByEmailAndDeletedFalse(String email);

    /**
     * Check if email exists (non-deleted only).
     *
     * @param email user email
     * @return Mono of Boolean
     */
    Mono<Boolean> existsByEmailAndDeletedFalse(String email);

    /**
     * Find users with search criteria and pagination.
     *
     * @param searchTerm search term for name or email (use LOWER for case-insensitive)
     * @param limit      page size
     * @param offset     offset for pagination
     * @return Flux of Users
     */
    @Query("""
        SELECT * FROM users
        WHERE deleted = FALSE
        AND (LOWER(name) LIKE LOWER(:searchTerm) OR LOWER(email) LIKE LOWER(:searchTerm))
        ORDER BY created_at DESC
        LIMIT :limit OFFSET :offset
        """)
    Flux<User> findBySearchCriteria(String searchTerm, int limit, long offset);

    /**
     * Count users matching search criteria.
     *
     * @param searchTerm search term for name or email
     * @return Mono of count
     */
    @Query("""
        SELECT COUNT(*) FROM users
        WHERE deleted = FALSE
        AND (LOWER(name) LIKE LOWER(:searchTerm) OR LOWER(email) LIKE LOWER(:searchTerm))
        """)
    Mono<Long> countBySearchCriteria(String searchTerm);
}
