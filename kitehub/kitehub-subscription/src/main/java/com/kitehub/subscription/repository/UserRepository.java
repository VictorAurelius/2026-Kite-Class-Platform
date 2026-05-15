package com.kitehub.subscription.repository;

import com.kitehub.platform.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 *
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByVerificationToken(String verificationToken);

    /**
     * Lookup for password-reset confirm (GAP-548). Token is opaque URL-safe random
     * string scoped to one in-flight reset per user — single-use, cleared on confirm.
     */
    Optional<User> findByPasswordResetToken(String passwordResetToken);
}
